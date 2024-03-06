package com.softip.uoo.server.scheduler;

import com.softip.uoo.server.config.AppProps;
import com.softip.uoo.server.model.dto.RateLimitDto;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.tbk.tor.hs.HiddenServiceDefinition;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@ActiveProfiles("test")
class RateLimitSchedulerTest {

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
    RateLimitScheduler rateLimitScheduler;

    @Autowired
    AppProps appProps;

    @Autowired
    private List<RateLimitDto> rateLimitSet;

    @Test
    void clearRateLimit1() {
        //test - vyhoď tie ktoré prekročili časovú platnosť linku
        rateLimitSetRemoveAll();
        rateLimitSet.addAll(getTest1RateLimitSet());
        rateLimitScheduler.clearRateLimit();
        assertEquals(2, rateLimitSet.size());
    }

    @Test
    void clearRateLimit2() {

        //test - vyhoď tie ktoré boli použité a uplynul predpísaný čas potrebný pre opatovné poźiadanie o link
        rateLimitSetRemoveAll();
        rateLimitSet.addAll(getTest2RateLimitSet());
        rateLimitScheduler.clearRateLimit();
        assertEquals(2, rateLimitSet.size());
    }

    private void rateLimitSetRemoveAll() {
        while (!rateLimitSet.isEmpty()) {
            rateLimitSet.remove(0);
        }
    }

    private List<RateLimitDto> getTest1RateLimitSet() {
        return List.of(
                RateLimitDto.builder()
                        .email("test1@test.test")
                        .nonce("")
                        .dateTime(LocalDateTime.now().minusMinutes(Long.parseLong(appProps.getLinkTimeValidiry())).minusMinutes(1L))
                        .used(false)
                        .build(),
                RateLimitDto.builder()
                        .email("test2@test.test")
                        .nonce("")
                        .dateTime(LocalDateTime.now())
                        .used(false)
                        .build(),
                RateLimitDto.builder()
                        .email("test3@test.test")
                        .nonce("")
                        .dateTime(LocalDateTime.now())
                        .used(false)
                        .build()
        );
    }

    private List<RateLimitDto> getTest2RateLimitSet() {
        return List.of(
                RateLimitDto.builder()
                        .email("test1@test.test")
                        .nonce("")
                        .dateTime(LocalDateTime.now().minusMinutes(Long.parseLong(appProps.getTimeBetweenLinkRequests())).minusMinutes(1L))
                        .used(true)
                        .build(),
                RateLimitDto.builder()
                        .email("test2@test.test")
                        .nonce("")
                        .dateTime(LocalDateTime.now())
                        .used(true)
                        .build(),
                RateLimitDto.builder()
                        .email("test3@test.test")
                        .nonce("")
                        .dateTime(LocalDateTime.now())
                        .used(false)
                        .build()
        );
    }
}