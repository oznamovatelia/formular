package com.softip.uoo.server.controller;


import com.softip.uoo.server.model.dto.TorMailDto;
import com.softip.uoo.server.service.EmailService;
import com.softip.uoo.server.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin
@RequestMapping("/tor")
@RestController
@Validated
public class TorMailController {

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FormService formService;


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


    @GetMapping(value = "testTorMail")
    String testTorMail(@RequestParam String subject) {
        String result = "Mail was sent successfuly";
        try {
            formService.testTorMail(subject);
        } catch (Exception e) {
            result = "Mail sending failed";
        }
        return result;
    }


}
