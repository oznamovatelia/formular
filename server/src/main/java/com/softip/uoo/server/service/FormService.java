package com.softip.uoo.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softip.uoo.server.config.AppProps;
import com.softip.uoo.server.model.FormMailModel;
import com.softip.uoo.server.model.FormModel;
import com.softip.uoo.server.model.dto.AESEncryptDto;
import com.softip.uoo.server.model.dto.LinkDataDto;
import com.softip.uoo.server.model.dto.RateLimitDto;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import util.AESUtil;
import util.PGPUtil;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class FormService {

    public static final String KENDO_MAX_FILE_SIZE = "kendoMaxFileSize";
    private static final String FORM = "form";
    private static final String CASOVY_LIMIT_MEDZI_PODANIAMI = "casovyLimitMedziPodaniami";
    private static final String DONE_MAIL = "doneMail";
    private static final String FORM_MAIL = "formMail";
    private static final String NEPLATNY_LINK = "neplatnyLink";
    private static final String LANG = "lang";
    @Autowired
    private AppProps appProps;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private List<RateLimitDto> rateLimitSet;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Autowired
    private MessageSource messageSource;

    private static final List<String> allowedExtensions = List.of(".pdf", ".docx", ".doc", ".xlsx", ".xls", ".pptx", ".ppt",
            ".odt", ".ods", ".odp", ".tiff", ".jpg", ".jpeg", ".heif", ".hevc", ".gif",
            ".bmp", ".mp4", ".mpg", ".mpeg", ".mov", ".m4v", ".avi", ".webm",
            ".3gp", ".mp3", ".m4a", ".aac", ".amr", ".wav");

    /*
     * metoda odosle na email potvrdzovaci link, vyskladany z emailu a aktualneho datumu a casu
     * tieto udaje sa zasifruju pomocou AES GCM a odoslu na zadany email
     */
    public void sendLink(String email) throws Exception {
        Locale locale = LocaleContextHolder.getLocale();
        LocalDateTime dateTime = LocalDateTime.now();

        final LinkDataDto linkDataDto = LinkDataDto.builder()
                .dateTime(dateTime)
                .email(email)
                .build();

        String payload = objectMapper.writeValueAsString(linkDataDto);

        InputStream insKey = new FileInputStream(appProps.getAesKey().getFileName());

        Key aesKey = AESUtil.readKey(insKey, appProps.getAesKey().getStorePassword(), appProps.getAesKey().getKeyPassword(), appProps.getAesKey().getAlias());
        AESEncryptDto payloadEncDto = AESUtil.encrypt(aesKey, payload.getBytes());

        String linkToSend = getBaseUrl() + "/form?payload=" + URLEncoder.encode(Base64.getEncoder().encodeToString(payloadEncDto.getCipherText()), StandardCharsets.UTF_8.toString()) + "&nonce=" + URLEncoder.encode(Base64.getEncoder().encodeToString(payloadEncDto.getIv()), StandardCharsets.UTF_8.toString());
        log.info("Text to send : " + linkToSend);


        String text = messageSource.getMessage("formMail.mail.text", new Object[]{linkToSend}, locale);
        String subject = messageSource.getMessage("formMail.mail.subject", new Object[]{}, locale);
        emailService.sendMsg(email, subject, text, true);

        //platný je iba posledný link
        rateLimitSet.removeIf(x -> x.getEmail().equals(email));

        rateLimitSet.add(RateLimitDto.builder()
                .dateTime(dateTime)
                .email(email)
                .nonce(Base64.getEncoder().encodeToString(payloadEncDto.getIv()))
                .used(false)
                .build());
    }

    public ModelAndView getForm(String payload, String nonce, Model model, boolean tor, String lang) throws FileNotFoundException, JsonProcessingException {

        InputStream insKey = new FileInputStream(appProps.getAesKey().getFileName());
        Key aesKey = AESUtil.readKey(insKey, appProps.getAesKey().getStorePassword(), appProps.getAesKey().getKeyPassword(), appProps.getAesKey().getAlias());
        byte[] iv = Base64.getDecoder().decode(nonce);

        String decryptedText = AESUtil.decrypt(aesKey, iv, Base64.getDecoder().decode(payload));

        LinkDataDto linkDataDto = objectMapper.readValue(decryptedText, LinkDataDto.class);

        final boolean isForbidden = rateLimitSet.stream()
                .noneMatch(item -> item.getEmail().equals(linkDataDto.getEmail()) && item.getNonce().equals(nonce) && !item.isUsed());
        if (isForbidden) {
            return getModelAndView(NEPLATNY_LINK);
        }

        final FormModel formModel = new FormModel();
        formModel.setEmail(linkDataDto.getEmail());
        formModel.setNonce(nonce);
        formModel.setEnv(appProps.getEnv());
        formModel.setFormAction(getBaseUrl() + "/form");
        formModel.setTor(tor);
        formModel.setMaxFileSize(maxFileSize);

        model.addAttribute(FORM, formModel);
        model.addAttribute(KENDO_MAX_FILE_SIZE, DataSize.parse(maxFileSize).toBytes());
        model.addAttribute(LANG, lang);

        return getModelAndView(FORM);
    }

    @NotNull
    private static ModelAndView getModelAndView(String viewName) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName(viewName);
        return modelAndView;
    }

    private String getBaseUrl() {
        return !StringUtils.isEmpty(appProps.getBaseUrl()) ? appProps.getBaseUrl() : ServletUriComponentsBuilder.fromCurrentContextPath().replacePath(null).build().toUriString();
    }

    public ModelAndView submitForm(FormModel formModel) throws Exception {

        final Optional<RateLimitDto> rateLimitOpt = rateLimitSet.stream()
                .filter(item -> item.getEmail().equals(formModel.getEmail()) && item.getNonce().equals(formModel.getNonce()) && !item.isUsed())
                .findFirst();

        if (!rateLimitOpt.isPresent()) {
            return getModelAndView(NEPLATNY_LINK);
        }

        XWPFDocument doc = getDocument(formModel);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(baos);

        addFilesFromFormModelToZip(formModel, zipOutputStream);

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        doc.write(b);
        InputStream docInputStream = new ByteArrayInputStream(b.toByteArray());

        String multipartName = "formular.docx";
        ZipEntry zipEntry = new ZipEntry(multipartName);
        zipOutputStream.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = docInputStream.read(bytes)) >= 0) {
            zipOutputStream.write(bytes, 0, length);
        }

        docInputStream.close();
        zipOutputStream.close();

        byte[] data = baos.toByteArray();

        InputStream insKey = new FileInputStream(appProps.getPgpPublicKeyFilename());
        final PGPPublicKey pgpPublicKey = PGPUtil.readPublicKey(insKey);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PGPUtil.encryptFile(out, data, pgpPublicKey, false, true);

        log.info("zip file size :" + out.size());
        if (out.size() <= appProps.getMaxMailSize()) {
            emailService.sendMsg(appProps.getSendEncryptedFormTo(), "Formulár s oznámením", "\nPriložený súbor obsahuje formulár s oznámením.", out, "formular.zip.enc");
        } else {
            createMultipartZipAndSend(out);
        }
        Locale locale = LocaleContextHolder.getLocale();
        //potvrdzovací mail - formular odoslaný
        emailService.sendMsg(formModel.getEmail(), messageSource.getMessage("form.mail.potvrdenie.subject", null, locale), messageSource.getMessage("form.mail.potvrdenie.text", null, locale));

        //nastav link použitý, ak uplynul limit na opatovné použitie tak vyhoď zo zoznamu
        rateLimitOpt.get().setUsed(true);
        if (rateLimitOpt.get().getDateTime().plusMinutes(Long.parseLong(appProps.getTimeBetweenLinkRequests())).isBefore(LocalDateTime.now())) {
            rateLimitSet.remove(rateLimitOpt.get());
        }

        return getModelAndView("done");
    }

    private static void addFilesFromFormModelToZip(FormModel formModel, ZipOutputStream zipOutputStream) throws IOException {
        if (formModel.getFiles() != null && formModel.getFiles().length > 0) {

            for (MultipartFile multipartFile : formModel.getFiles()) {
                String originalFilename = multipartFile.getOriginalFilename();
                if (StringUtils.isEmpty(originalFilename)) {
                    continue;
                }

                int lastIndexOf = originalFilename.lastIndexOf(".");
                if (!allowedExtensions.contains(originalFilename.substring(lastIndexOf, originalFilename.length()))) {
                    continue;
                }

                try (InputStream inputStream = multipartFile.getInputStream()) {
                    ZipEntry zipEntry = new ZipEntry(originalFilename);
                    zipOutputStream.putNextEntry(zipEntry);
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = inputStream.read(bytes)) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }
                }
            }
        }
    }

    private void createMultipartZipAndSend(ByteArrayOutputStream out) throws Exception {
        String tmpdir = Files.createTempDirectory("tmpDirUoo").toFile().getAbsolutePath();

        File formularFile = new File(tmpdir + "/formular.zip.enc");
        try (OutputStream outputStream = new FileOutputStream(formularFile)) {
            out.writeTo(outputStream);
        }

        List<File> filesToAdd = Arrays.asList(formularFile);


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("_yyyy_MM_dd_HH_mm_ss");
        String tmpdirZip = Files.createTempDirectory("tmpDirZip").toFile().getAbsolutePath();

        try (ZipFile zipFile = new ZipFile(tmpdirZip + "/formular" + LocalDateTime.now().format(formatter) + ".zip");) {
            zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), true, appProps.getMaxMailSize()); // using 10MB in this example
        }

        FileUtils.deleteDirectory(new File(tmpdir));

        File[] files = new File(tmpdirZip).listFiles();

        int i = 1;
        for (File f : files) {
            emailService.sendMsg(appProps.getSendEncryptedFormTo(), "Formulár s oznámením " + "(" + i + "/" + files.length + ")", "\nPriložený súbor obsahuje formulár s oznámením.", f);
            i++;
        }
        FileUtils.deleteDirectory(new File(tmpdirZip));
    }

    private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm ";
    @NotNull
    private XWPFDocument getDocument(FormModel formModel) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource resource = resolver.getResource("classpath:uoo_webres/template.docx");

        XWPFDocument doc = new XWPFDocument(resource.getInputStream());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        doc = replaceTextFor(doc, "VAR_INF_NEKALA_PRAKTIKA_SUVISLOST_VASA_PRACA", formModel.getInfNekalaPraktikaSuvislostVasaPraca());
        doc = replaceTextFor(doc, "VAR_IDENTIFIKACIA", formModel.getIdentifikacia());
        doc = replaceTextFor(doc, "VAR_OPIS_NEKALEJ_PRAKTIKY", formModel.getOpisNekalejPraktiky());
        doc = replaceTextFor(doc, "VAR_POPIS_OHROZENIE_VEREJNY_ZAUJEM", formModel.getPopisOhrozenieVerejnyZaujem());
        doc = replaceTextFor(doc, "VAR_KEDY_AKO_DLHO", formModel.getKedyAkoDlho());

        doc = replaceTextFor(doc, "VAR_DOKUMENTY_K_DISPOZICII", formModel.getDokumentyKDispozicii());
        doc = replaceTextFor(doc, "VAR_OZNAMENIE_ZAMESTNAVATEL", formModel.getOznamenieZamestnavatel());
        doc = replaceTextFor(doc, "VAR_OZNAMENIE_NA_POLICIU", formModel.getOznamenieNaPoliciu());
        doc = replaceTextFor(doc, "VAR_OZNAMENIE_INY_ORGAN_ANO_NIE", formModel.getOznamenieInyOrgan());
        doc = replaceTextFor(doc, "VAR_OZNAMENIE_INY_ORGAN_POPIS", formModel.getOznamenieInyOrganPopis());
        doc = replaceTextFor(doc, "VAR_OZNAMENIE_NIE", formModel.getOznamenieNie());
        doc = replaceTextFor(doc, "VAR_AKO_PRESETRENE", formModel.getAkoPresetrene());
        doc = replaceTextFor(doc, "VAR_ODVETNE_OPATRENIA", formModel.getOdvetneOpatrenia());
        doc = replaceTextFor(doc, "VAR_POPIS_ODVETNE_OPATRENIA", formModel.getPopisOdvetneOpatrenia());
        doc = replaceTextFor(doc, "VAR_POPIS_OCAKAVANIA", formModel.getPopisOcakavania());

        doc = replaceTextFor(doc, "VAR_DOZVEDENIE_O_URADE", formModel.getDozvedenieOUrade());
        doc = replaceTextFor(doc, "VAR_SUHLAS_SPRISTUPNENIE", formModel.getSuhlasSpristupnenie());
        doc = replaceTextFor(doc, "VAR_MENO", formModel.getMeno());
        doc = replaceTextFor(doc, "VAR_TELEFON", formModel.getTelefon());
        doc = replaceTextFor(doc, "VAR_EMAIL", formModel.getEmail());
        doc = replaceTextFor(doc, "VAR_DATUM_CAS", ZonedDateTime.now().format(formatter) + "CET/CEST");

        return doc;
    }

    private XWPFDocument replaceTextFor(XWPFDocument doc, String findText, String replaceText) {
        if (replaceText == null) {
            replaceText = "";
        }
        String finalReplaceText = replaceText;

        doc.getParagraphs().forEach(p ->
                p.getRuns().forEach(run -> {
                    String text = run.text();
                    if (text.contains(findText)) {
                        run.setText(text.replace(findText, finalReplaceText), 0);
                    }
                })
        );
        return doc;
    }

    public ModelAndView getFormMail(Model model, boolean tor, String lang) {
        final FormMailModel formMailModel = new FormMailModel();
        formMailModel.setEnv(appProps.getEnv());
        formMailModel.setFormAction(getBaseUrl() + "/form/mail");
        formMailModel.setTor(tor);

        model.addAttribute(FORM, formMailModel);
        model.addAttribute(LANG, lang);

        return getModelAndView(FORM_MAIL);
    }


    public ModelAndView submitFormMail(FormMailModel formMailModel) throws Exception {

        if (kontrolaMinimalnyCasMedziPodaniami(formMailModel)) {
            sendLink(formMailModel.getEmail());
            return getModelAndView(DONE_MAIL);
        } else {
            return getModelAndView(CASOVY_LIMIT_MEDZI_PODANIAMI);
        }
    }

    private boolean kontrolaMinimalnyCasMedziPodaniami(FormMailModel formMailModel) {
        return rateLimitSet.stream().noneMatch(item -> item.getEmail().equals(formMailModel.getEmail()) &&
                item.getDateTime().plusMinutes(Long.parseLong(appProps.getTimeBetweenLinkRequests())).isAfter(LocalDateTime.now()));
    }

    public void testTorMail(String subject) throws Exception {
        emailService.sendTorMsg("test456@gtfcy37qyzor7kb6blz2buwuu5u7qjkycasjdf3yaslibkbyhsxub4yd.onion", subject, "www.softip.sk"); //uoo-onionmail
    }

}
