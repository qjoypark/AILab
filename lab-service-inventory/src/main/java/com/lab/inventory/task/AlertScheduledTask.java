package com.lab.inventory.task;

import com.lab.inventory.service.AlertService;
import com.lab.inventory.service.HazardousDiscrepancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预警定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduledTask {
    
    private final AlertService alertService;
    private final HazardousDiscrepancyService hazardousDiscrepancyService;
    
    /**
     * 低库存预警检查 - 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkLowStockAlert() {
        log.info("执行低库存预警检查定时任务");
        try {
            alertService.checkLowStockAlert();
        } catch (Exception e) {
            log.error("低库存预警检查失败", e);
        }
    }
    
    /**
     * 有效期预警检查 - 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkExpirationAlert() {
        log.info("执行有效期预警检查定时任务");
        try {
            alertService.checkExpirationAlert();
        } catch (Exception e) {
            log.error("有效期预警检查失败", e);
        }
    }
    
    /**
     * 危化品账实差异计算 - 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void calculateHazardousDiscrepancy() {
        log.info("执行危化品账实差异计算定时任务");
        try {
            hazardousDiscrepancyService.calculateDiscrepancy();
        } catch (Exception e) {
            log.error("危化品账实差异计算失败", e);
        }
    }
}
