package com.softip.uoo.server.service;

import com.softip.uoo.server.config.AppProps;
import com.softip.uoo.server.exception.EmailRuntimeException;
import com.softip.uoo.server.model.dto.TorMailDto;
import lombok.extern.slf4j.Slf4j;
import org.berndpruenster.netlayer.tor.Tor;
import org.berndpruenster.netlayer.tor.TorCtlException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class EmailService {

    private static final String EMAIL_ODOSLANY_TO_SUBJECT = "Email odoslany: to: {}, subject: {}";
    private static final String EMAIL_NEODOSLANY_TO = "Email neodoslany: to:";
    private static final String SUBJECT = " subject: ";
    private static final String EMAIL_NEBOL_ODOSLANY = "Email nebol odoslany";

    public static final String POINT_ONION = ".onion";

    @Autowired
    private AppProps appProps;

    @Autowired
    private JavaMailSenderImpl emailSender;


    @Autowired(required = false)
    private Tor tor;

    @Autowired
    private RestTemplate restTemplate;

    public void sendMsg(String to, String subject, String text, ByteArrayOutputStream attachment, String attachmentFilename) throws Exception {
        MimeMessage msg = emailSender.createMimeMessage();
        MimeMessageHelper helper = getMimeMessageHelper(to, subject, text, msg);
        helper.addAttachment(attachmentFilename, new ByteArrayResource(attachment.toByteArray()));
        send(to, subject, msg);
    }

    public void sendMsg(String to, String subject, String text, File f) throws Exception {
        MimeMessage msg = emailSender.createMimeMessage();
        MimeMessageHelper helper = getMimeMessageHelper(to, subject, text, msg);
        helper.addAttachment(f.getName(), f);
        send(to, subject, msg);
    }

    private void send(String to, String subject, MimeMessage msg) throws Exception {
        try {
            emailSender.send(msg);
            log.info(EMAIL_ODOSLANY_TO_SUBJECT, to, subject);
        } catch (Exception e) {
            log.error(EMAIL_NEODOSLANY_TO + to + SUBJECT + subject, e);
            throw new EmailRuntimeException(EMAIL_NEBOL_ODOSLANY, e);
        }
    }

    @NotNull
    private MimeMessageHelper getMimeMessageHelper(String to, String subject, String text, MimeMessage msg) throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setFrom(appProps.getMailFrom());
        helper.setTo(to.split(","));
        helper.setSubject(subject);
        helper.setText(text);
        helper.setSentDate(new Date());
        return helper;
    }


    public void sendMsg(String to, String subject, String text) throws Exception {
        if (to.endsWith(POINT_ONION)) {
            sendTorMsgRest(to, Collections.emptyList(), Collections.emptyList(), subject, text, false);
        } else {
            sendMsg(to, Collections.emptyList(), Collections.emptyList(), subject, text);
        }

    }

    public void sendTorMsg(String to, String subject, String text) throws Exception {
            sendTorMsg(to, Collections.emptyList(), Collections.emptyList(), subject, text, false);
    }

    private void sendMsg(String to, List<String> cc, List<String> bcc, String subject, String text) throws Exception {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(appProps.getMailFrom());
        msg.setTo(to);
        msg.setCc(cc.toArray(new String[0]));
        msg.setBcc(bcc.toArray(new String[0]));
        msg.setSubject(subject);
        msg.setText(text);
        msg.setSentDate(new Date());
        try {
            emailSender.send(msg);
            log.info(EMAIL_ODOSLANY_TO_SUBJECT, to, subject);
        } catch (Exception e) {
            log.error(EMAIL_NEODOSLANY_TO + to + SUBJECT + subject, e);
            throw new EmailRuntimeException(EMAIL_NEBOL_ODOSLANY);
        }
    }


    public void sendMsg(String to, String subject, String text, boolean html) throws Exception {

        try {
            if (to.endsWith(POINT_ONION)) {
                sendTorMsgRest(to, Collections.emptyList(), Collections.emptyList(), subject, text, html);
            } else {
                MimeMessage msg = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, false);
                helper.setFrom(appProps.getMailFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(text, html);
                helper.setSentDate(new Date());

                emailSender.send(msg);
            }
            log.info(EMAIL_ODOSLANY_TO_SUBJECT, to, subject);
        } catch (Exception e) {
            log.error(EMAIL_NEODOSLANY_TO + to + SUBJECT + subject, e);
            throw new EmailRuntimeException(EMAIL_NEBOL_ODOSLANY, e);
        }
    }

    private void sendTorMsgRest(String to, List<String> cc, List<String> bcc, String subject, String text, boolean html) throws URISyntaxException {

        final String url = appProps.getBaseUrlTorMail() + "/tor/sendTorMsg";
        URI uri = new URI(url);
        TorMailDto torMailDto = TorMailDto.builder()
                .to(to)
                .cc(cc)
                .bcc(bcc)
                .subject(subject)
                .text(text)
                .html(html)
                .build();

        ResponseEntity<String> result = restTemplate.postForEntity(uri, torMailDto, String.class);
        if (!"OK".equals(result.getBody())) {
            throw new EmailRuntimeException(result.getBody());
        }
    }

    public void sendTorMsg(String to, List<String> cc, List<String> bcc, String subject, String text, Boolean html) throws Exception {
        try {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("socksProxyHost", "127.0.0.1");
            System.getProperties().put("socksProxyPort", String.valueOf(tor.getProxy().getPort()));

            emailSender.setHost(appProps.getTorMail().getHost());
            emailSender.setPort(Integer.parseInt(appProps.getTorMail().getPort()));

            emailSender.setUsername(appProps.getTorMail().getUsername());
            emailSender.setPassword(appProps.getTorMail().getPassword());
            emailSender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
            emailSender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "true");

            MimeMessage msg = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false);
            msg.setFrom(appProps.getTorMail().getFrom());
            helper.setTo(to);
            helper.setCc(cc.toArray(new String[cc.size()]));
            helper.setBcc(bcc.toArray(new String[bcc.size()]));
            helper.setSubject(subject);
            helper.setText(text, html);
            helper.setSentDate(new Date());

            emailSender.send(msg);
            log.info(EMAIL_ODOSLANY_TO_SUBJECT, to, subject);
        
        } catch (TorCtlException e) {
            log.error(EMAIL_NEODOSLANY_TO + to + SUBJECT + subject, e);
            throw new EmailRuntimeException(EMAIL_NEBOL_ODOSLANY);
        } catch (Exception e) {
            log.error(EMAIL_NEODOSLANY_TO + to + SUBJECT + subject, e);
            throw new EmailRuntimeException(EMAIL_NEBOL_ODOSLANY);
        }
    }

}
