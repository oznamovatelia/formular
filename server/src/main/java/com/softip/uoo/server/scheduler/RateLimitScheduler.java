package com.softip.uoo.server.scheduler;

import com.softip.uoo.server.config.AppProps;
import com.softip.uoo.server.model.dto.RateLimitDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RateLimitScheduler {

    @Autowired
    AppProps appProps;

    @Autowired
    private List<RateLimitDto> rateLimitSet;

    @Scheduled(fixedDelayString = "${app.rateLimitSchedulerDelay}")
    public void clearRateLimit() {
        //vyhoď tie ktoré prekročili časovú platnosť linku
        rateLimitSet.removeIf(item -> item.getDateTime().plusMinutes(Long.parseLong(appProps.getLinkTimeValidiry())).isBefore((LocalDateTime.now())));

        //vyhoď tie ktoré boli použité a uplynul predpísaný čas potrebný pre opatovné poźiadanie o link
        rateLimitSet.removeIf(item -> item.isUsed() && item.getDateTime().plusMinutes(Long.parseLong(appProps.getTimeBetweenLinkRequests())).isBefore((LocalDateTime.now())));

    }
}
