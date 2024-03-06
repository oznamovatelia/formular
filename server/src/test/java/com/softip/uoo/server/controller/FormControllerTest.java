package com.softip.uoo.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softip.uoo.server.model.FormMailModel;
import com.softip.uoo.server.model.FormModel;
import com.softip.uoo.server.service.CaptchaService;
import com.softip.uoo.server.service.EmailService;
import com.softip.uoo.server.service.FormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FormController.class)
@ActiveProfiles("test")
class FormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    BuildProperties buildProperties;

    @MockBean
    FormService formService;

    @MockBean
    CaptchaService captchaService;

    @MockBean
    EmailService emailService;


    @BeforeEach
    void setUp() throws Exception {
        when(captchaService.verifyRecaptcha(null)).thenReturn(String.valueOf(""));
        when(formService.submitFormMail(new FormMailModel())).thenReturn(null);
        when(formService.submitForm(new FormModel())).thenReturn(null);
    }

    @Test
    void getVersion() throws Exception {
        when(buildProperties.getVersion()).thenReturn("1.2.3");
        this.mockMvc
                .perform(get("/form/getVersion"))
                .andExpect(status().isOk())
                .andExpect(content().string("Version: 1.2.3"));
    }

    @Test
    void getFormMail() throws Exception {
        this.mockMvc
                .perform(get("/form/mail"))
                .andExpect(status().isOk());
    }

    @Test
    void submitFormMail() throws Exception {
        this.mockMvc
                .perform(post("/form/mail")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "test@test.test")
                        .sessionAttr("form", new FormMailModel("test@test.test", "test", "form", true)))
                .andExpect(status().isOk());
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void submitForm() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file = new MockMultipartFile("file", "orig", null, "bar".getBytes());
        this.mockMvc
                .perform(fileUpload("/form").file(file))
                .andExpect(status().isOk());
    }
}