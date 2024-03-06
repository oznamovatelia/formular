package com.softip.uoo.server.service;

import com.softip.uoo.server.config.AppProps;
import com.softip.uoo.server.model.dto.captcha.FriendlyCaptchaData;
import com.softip.uoo.server.model.dto.captcha.FriendlyCaptchaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.stream.Collectors;

@Service
public class CaptchaService {

    private static final String CAPTCHA_URL = "https://api.friendlycaptcha.com/api/v1/siteverify";
    @Autowired
    AppProps appProps;

    @Autowired
    private RestTemplate captchaRestTemplate;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private MessageSource messageSource;


    public String verifyRecaptcha(String responseCaptchaSolution) {

        FriendlyCaptchaResponse friendlyCaptchaResponse = captchaRestTemplate.postForObject(
                URI.create(CAPTCHA_URL),
                FriendlyCaptchaData.builder().solution(responseCaptchaSolution)
                        .sitekey(appProps.getCaptcha().getSite())
                        .secret(appProps.getCaptcha().getSecret()).build(),
                FriendlyCaptchaResponse.class);

        if (friendlyCaptchaResponse == null) {
            return messageSource.getMessage("captcha.chyba", null, LocaleContextHolder.getLocale());
        }

        if (!friendlyCaptchaResponse.isSuccess()) {
            return friendlyCaptchaResponse.getErrors().stream().collect(Collectors.joining(", "));
        } else {
            return "";
        }
    }
}