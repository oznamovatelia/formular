package com.softip.uoo.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProps {

    private Keystore aesKey;
    private Captcha captcha;
    private Proxy proxy;
    private String pgpPublicKeyFilename;
    private String sendEncryptedFormTo;
    private String baseUrl;
    private String baseUrlTorMail;
    private String env;
    private String rateLimitSchedulerDelay;
    private String rateLimitSchedulerMaxDelay;
    private TorMail torMail;
    private String mailFrom;


    private String timeBetweenLinkRequests;
    private String linkTimeValidiry;
    private long maxMailSize;

    @Data
    public static class Keystore {
        private String storePassword;
        private String fileName;
        private String keyPassword;
        private String alias;
    }

    @Data
    public static class TorMail {
        private String username;
        private String password;
        private String host;
        private String port;
        private String from;
    }

    @Data
    public static class Captcha {
        private String site;
        private String secret;
    }

    @Data
    public static class Proxy {
        private String host;
        private String port;
    }
}
