package com.softip.uoo.server.config;

import com.softip.uoo.server.model.dto.RateLimitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class BeanConfig {

    @Bean
    public List<RateLimitDto> rateLimitSet() {
        return new ArrayList<>();
    }

    @Bean
    public RestTemplate captchaRestTemplate(AppProps appProps) {

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        if (appProps.getProxy().getHost() != null && !appProps.getProxy().getHost().isEmpty() && !appProps.getProxy().getHost().equals("empty")) {
            int portNr = -1;
            try {
                portNr = Integer.parseInt(appProps.getProxy().getPort().trim());
            } catch (Exception e) {
                log.error("Nie je možné rozparsovať číslo portu (proxy.port)" + e.getMessage());
            }

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(appProps.getProxy().getHost(), portNr));
            requestFactory.setProxy(proxy);
        }
        restTemplate.setRequestFactory(requestFactory);

        return new RestTemplate();
    }
}
