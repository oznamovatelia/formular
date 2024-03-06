package com.softip.uoo.server.model.dto.captcha;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendlyCaptchaData {
    private String solution;
    private String secret;
    private String sitekey;
}