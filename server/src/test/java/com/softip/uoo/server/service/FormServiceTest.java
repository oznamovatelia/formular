package com.softip.uoo.server.service;

import com.softip.uoo.server.model.FormMailModel;
import com.softip.uoo.server.model.FormModel;
import com.softip.uoo.server.model.dto.RateLimitDto;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.tbk.tor.hs.HiddenServiceDefinition;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FormServiceTest {
    private static final String EMAIL_TEST = "test@test.test";
    private static final String NONCE_TEST = "0Gypwo4vqU5tIwfh";
    // Mock tor
    @MockBean
    org.berndpruenster.netlayer.tor.Tor tor;

    @MockBean
    @Qualifier("torHttpClient")
    HttpClient torHttpClient;

    @MockBean
    @Qualifier("applicationHiddenServiceDefinition")
    private HiddenServiceDefinition applicationHiddenServiceDefinition;
    // Mock tor end

    @Autowired
    FormService formService;

    @MockBean
    private EmailService emailService;

    @Autowired
    private List<RateLimitDto> rateLimitSet;

    private MockHttpServletRequest mockRequest;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setContextPath("http://localhost");
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);
    }

    @Test
    void sendLink() throws Exception {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setContextPath("http://localhost");
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attrs);

        rateLimitSetRemoveAll();
        formService.sendLink(EMAIL_TEST);
        assertEquals(1, rateLimitSet.size());
        assertEquals(EMAIL_TEST, rateLimitSet.get(0).getEmail());
        assertFalse(rateLimitSet.get(0).getNonce().isEmpty());

        //kontrola - platný je iba posledný link
        formService.sendLink(EMAIL_TEST);
        assertEquals(1, rateLimitSet.size());
    }

    @Test
    void getFormMail() throws Exception {
        String formMailHtml = this.restTemplate.getForObject("http://localhost:" + port + "/form/mail", String.class);
        assertTrue(formMailHtml.contains("email"));
    }

    @Test
    void submitFormMail() throws Exception {

        rateLimitSetRemoveAll();
        ModelAndView modelAndView = formService.submitFormMail(FormMailModel.builder()
                .formAction("form")
                .env("TEST")
                .email(EMAIL_TEST)
                .tor(true).build()
        );

        assertTrue(modelAndView.hasView());
        assertEquals("doneMail", modelAndView.getViewName());

        modelAndView = formService.submitFormMail(FormMailModel.builder()
                .formAction("form")
                .env("TEST")
                .email(EMAIL_TEST)
                .tor(true).build()
        );
        assertTrue(modelAndView.hasView());
        assertEquals("casovyLimitMedziPodaniami", modelAndView.getViewName());

    }

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Test
    void getForm() throws Exception {

        rateLimitSetRemoveAll();

        ModelAndView modelAndView = formService.getForm("BZwVdkidzivn6wap6m22tJUdtcSi0TULAmzEoToGrhOrOzuplcTYRL+1sKJKNJJ4HhWxCte6ME/M6iOuPiQ+QZn1PwfJ6GZxyv0JTBK/ld4y", NONCE_TEST, null, true, "");
        assertEquals("neplatnyLink", modelAndView.getViewName());

        rateLimitSetRemoveAll();
        Model model = new ExtendedModelMap();
        rateLimitSet.add(new RateLimitDto(EMAIL_TEST, NONCE_TEST, LocalDateTime.now(), false));
        modelAndView = formService.getForm("BZwVdkidzivn6wap6m22tJUdtcSi0TULAmzEoToGrhOrOzuplcTYRL+1sKJKNJJ4HhWxCte6ME/M6iOuPiQ+QZn1PwfJ6GZxyv0JTBK/ld4y",
                NONCE_TEST, model, true, "sk");
        assertEquals("form", modelAndView.getViewName());
        FormModel formModel = (FormModel) model.getAttribute("form");
        assertEquals(EMAIL_TEST, formModel.getEmail());
        assertEquals(NONCE_TEST, formModel.getNonce());
        assertTrue(formModel.isTor());
        assertEquals(formModel.getMaxFileSize(), maxFileSize.toString());
        assertEquals(model.getAttribute(FormService.KENDO_MAX_FILE_SIZE), DataSize.parse(maxFileSize).toBytes());
    }

    @Test
    void submitForm() throws Exception {
        rateLimitSetRemoveAll();
        FormModel formModel = FormModel.builder()
                .formAction("form")
                .tor(true)
                .nonce(NONCE_TEST)
                .email(EMAIL_TEST)
                .build();
        ModelAndView modelAndView = formService.submitForm(formModel);
        assertEquals("neplatnyLink", modelAndView.getViewName());

        rateLimitSet.add(new RateLimitDto(EMAIL_TEST, NONCE_TEST, LocalDateTime.now(), false));
        formModel = FormModel.builder()
                .formAction("form")
                .tor(true)
                .nonce(NONCE_TEST)
                .email(EMAIL_TEST)
                .build();
        modelAndView = formService.submitForm(formModel);
        assertEquals("done", modelAndView.getViewName());
        assertTrue(rateLimitSet.get(0).isUsed());
        assertEquals(EMAIL_TEST, rateLimitSet.get(0).getEmail());

        modelAndView = formService.submitForm(formModel);
        assertEquals("neplatnyLink", modelAndView.getViewName());
        assertTrue(rateLimitSet.get(0).isUsed());
        assertEquals(EMAIL_TEST, rateLimitSet.get(0).getEmail());
    }

    private void rateLimitSetRemoveAll() {
        while (!rateLimitSet.isEmpty()) {
            rateLimitSet.remove(0);
        }
    }
}