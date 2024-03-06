package com.softip.uoo.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.softip.uoo.server.config.AppProps;
import com.softip.uoo.server.exception.EmailRuntimeException;
import com.softip.uoo.server.model.FormMailModel;
import com.softip.uoo.server.model.FormModel;
import com.softip.uoo.server.model.dto.TorMailDto;
import com.softip.uoo.server.service.CaptchaService;
import com.softip.uoo.server.service.EmailService;
import com.softip.uoo.server.service.ExitNodeCheck;
import com.softip.uoo.server.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.FileNotFoundException;

import static com.softip.uoo.server.service.EmailService.POINT_ONION;

@CrossOrigin
@RequestMapping("/form")
@RestController
@Validated
public class FormController {

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    private FormService formService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AppProps appProps;

    private boolean isTor(HttpServletRequest request) {
        try {
            String ip = ExitNodeCheck.getClientIp(request);
            return ExitNodeCheck.isExitNodeInOnlineList(ip);
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping(value = "")
    ModelAndView getForm(@RequestParam String payload, @RequestParam String nonce, Model model, HttpServletRequest request) throws FileNotFoundException, JsonProcessingException {
        String lang = request.getParameter("lang");
        return formService.getForm(payload, nonce, model, isTor(request), lang);
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView submitForm(@ModelAttribute("form") FormModel formModel) throws Exception {
        return formService.submitForm(formModel);
    }

    @GetMapping(value = "getVersion")
    String getVersion() {
        return "Version: " + buildProperties.getVersion();
    }


    @PostMapping(value = "/sendTorMsg", consumes = "application/json")
    public String sendTorMsg(@Valid @RequestBody TorMailDto torMailDto) {

        try {
            emailService.sendTorMsg(torMailDto.getTo(), torMailDto.getCc(), torMailDto.getBcc(), torMailDto.getSubject(), torMailDto.getText(), torMailDto.getHtml());
        } catch (Exception e) {
            return e.getMessage();
        }
        return "OK";
    }

    @GetMapping(value = "/mail")
    ModelAndView getFormMail(Model model, HttpServletRequest request) {
        return formService.getFormMail(model, isTor(request), request.getParameter("lang"));
    }

    @PostMapping(value = "/mail")
    public ModelAndView submitFormMail(Model model, @ModelAttribute("form") FormMailModel formMailModel, HttpServletRequest request) throws Exception {
        if (!StringUtils.isEmpty(appProps.getCaptcha().getSite())) {
            String error = captchaService.verifyRecaptcha(request.getParameter("frc-captcha-solution"));
            if (StringUtils.hasText(error)) {
                model.addAttribute("captchaError", messageSource.getMessage("captcha.chyba", null, LocaleContextHolder.getLocale()));
                return formService.getFormMail(model, isTor(request), request.getParameter("lang"));
            }
        }

        if (!StringUtils.hasText(formMailModel.getEmail())) {
            throw new EmailRuntimeException("Prázdny mail");
        }

        // ak nie je torMail tak kontrola na .onion
        if (appProps.getBaseUrlTorMail().isEmpty() && formMailModel.getEmail().trim().toLowerCase().endsWith(POINT_ONION)) {
            model.addAttribute("emailError", messageSource.getMessage("form.mail.email.onion.nepodporovane", null, LocaleContextHolder.getLocale()));
            return formService.getFormMail(model, isTor(request), request.getParameter("lang"));
        }

        return formService.submitFormMail(formMailModel);

    }


}
