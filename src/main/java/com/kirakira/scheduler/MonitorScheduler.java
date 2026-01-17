package com.kirakira.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kirakira.service.MonitorService;

@Component
public class MonitorScheduler {
    private final MonitorService monitorService;
    private static final Logger log = LoggerFactory.getLogger(MonitorScheduler.class);

    public MonitorScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(fixedRateString = "${scheduler.monitor.interval.ms:300000}")
    public void monitorSubmissions() {
        try {
            monitorService.checkRecentSubmissionsAndNotify();
        } catch (Exception e) {
            log.error("Error during scheduled submission monitoring", e);
        }
    }
}
