package com.softip.uoo.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.berndpruenster.netlayer.tor.Tor;
import org.berndpruenster.netlayer.tor.TorCtlException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.tbk.tor.hs.HiddenServiceDefinition;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
public class ServerApplication {

    private static final String EQUALS_EQUALS_EQUALS = "=================================================";

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }


    @Autowired(required = false)
    private Tor tor;

    @Autowired(required = false)
    @Qualifier("torHttpClient")
    private HttpClient torHttpClient;

    @Autowired(required = false)
    @Qualifier("applicationHiddenServiceDefinition")
    private HiddenServiceDefinition applicationHiddenServiceDefinition;

    @Bean
    @ConditionalOnProperty(prefix = "org.tbk.tor", name = "enabled")
    public ApplicationRunner applicationHiddenServiceInfoRunner() {
        return args -> {
            Optional<String> httpUrl = applicationHiddenServiceDefinition.getVirtualHost()
                    .map(val -> "http://" + val + ":" + applicationHiddenServiceDefinition.getVirtualPort());

            log.info(EQUALS_EQUALS_EQUALS);
            log.info("url: {}", httpUrl.orElse("unavailable"));
            log.info("virtual host: {}", applicationHiddenServiceDefinition.getVirtualHost().orElse("unknown"));
            log.info("virtual port: {}", applicationHiddenServiceDefinition.getVirtualPort());
            log.info("host: {}", applicationHiddenServiceDefinition.getHost());
            log.info("port: {}", applicationHiddenServiceDefinition.getPort());
            log.info("directory: {}", applicationHiddenServiceDefinition.getDirectory().getAbsolutePath());
            httpUrl.ifPresent(url -> {
                log.info("-------------------------------------------------");
                try {
                    log.info("run: torsocks -p {} curl {}/form.html -v", tor.getProxy().getPort(), url);
                } catch (TorCtlException e) {
                    log.warn("Could not get tor proxy port");
                }
            });
            log.info(EQUALS_EQUALS_EQUALS);
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "org.tbk.tor", name = "enabled")
    public ApplicationRunner allOtherHiddenServicesInfoRunner(List<HiddenServiceDefinition> hiddenServices) {
        return args -> {
            List<HiddenServiceDefinition> otherHiddenServices = hiddenServices.stream()
                    .filter(val -> val != applicationHiddenServiceDefinition)
                    .collect(Collectors.toList());

            otherHiddenServices.forEach(hiddenService -> {
                Optional<String> httpUrl = applicationHiddenServiceDefinition.getVirtualHost()
                        .map(val -> "http://" + val + ":" + applicationHiddenServiceDefinition.getVirtualPort());

                log.info(EQUALS_EQUALS_EQUALS);
                log.info("url: {}", httpUrl.orElse("unavailable"));
                log.info("virtual host: {}", applicationHiddenServiceDefinition.getVirtualHost().orElse("unknown"));
                log.info("virtual port: {}", applicationHiddenServiceDefinition.getVirtualPort());
                log.info("host: {}", applicationHiddenServiceDefinition.getHost());
                log.info("port: {}", applicationHiddenServiceDefinition.getPort());
                log.info("directory: {}", applicationHiddenServiceDefinition.getDirectory().getAbsolutePath());
                httpUrl.ifPresent(url -> {
                    log.info("-------------------------------------------------");
                    try {
                        log.info("run: torsocks -p {} curl {}/form.html -v", tor.getProxy().getPort(), url);
                    } catch (TorCtlException e) {
                        log.warn("Could not get tor proxy port");
                    }
                });
                log.info(EQUALS_EQUALS_EQUALS);
            });
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "org.tbk.tor", name = "enabled")
    public ApplicationRunner torInfoRunner() {
        String successPhrase = "Congratulations. This browser is configured to use Tor.";
        String errorPhraseIgnoreCase = "not using Tor";

        return args -> {
            HttpGet req = new HttpGet("https://check.torproject.org/");

            HttpResponse rsp = torHttpClient.execute(req);

            String body = EntityUtils.toString(rsp.getEntity(), StandardCharsets.UTF_8);

            boolean containsErrorPhrase = body.toLowerCase().contains(errorPhraseIgnoreCase.toLowerCase());
            boolean containsSuccessPhrase = body.contains(successPhrase);

            boolean torEnabled = containsSuccessPhrase && !containsErrorPhrase;

            log.info(EQUALS_EQUALS_EQUALS);
            if (torEnabled) {
                try {
                    log.info("Tor is enabled." + tor.getProxy().getPort());
                } catch (TorCtlException e) {
                    log.info("Tor is enabled on port: " + e.getMessage());
                }
            } else {
                log.warn("Tor is NOT enabled.");
            }
            log.info(EQUALS_EQUALS_EQUALS);
        };
    }

}
