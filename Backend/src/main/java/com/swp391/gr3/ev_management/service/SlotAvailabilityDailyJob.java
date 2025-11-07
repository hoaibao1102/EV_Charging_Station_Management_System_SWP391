package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotAvailabilityDailyJob {

    private final SlotConfigRepository slotConfigRepository;
    private final SlotAvailabilitySchedulerService schedulerService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void resetAllActiveConfigsForToday() {
        var actives = slotConfigRepository.findByIsActive(SlotConfigStatus.ACTIVE);
        if (actives.isEmpty()) return;

        LocalDate today = LocalDate.now();
        for (var cfg : actives) {
            int created = schedulerService.resetAndCreateForConfigInDate(cfg.getConfigId(), today);
             log.info("Regenerated {} availabilities for config {}", created, cfg.getConfigId());
        }
    }
}
