package com.kirakira.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kirakira.service.MonitorService;

@Component
public class MonitorScheduler {
    private final MonitorService monitorService;

    public MonitorScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(fixedRate = 300 * 1000)
    public void monitorSubmissions() {
        monitorService.checkRecentSubmissionsAndNotify();
    }
}
