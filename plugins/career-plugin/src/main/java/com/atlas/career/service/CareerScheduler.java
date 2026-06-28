package com.atlas.career.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CareerScheduler {
    private static final Logger log = LoggerFactory.getLogger(CareerScheduler.class);
    private final CareerWorkflow workflow;

    public CareerScheduler(CareerWorkflow workflow) {
        this.workflow = workflow;
    }

    @Scheduled(cron = "${atlas.career.daily-scan-cron:0 0 8 * * *}")
    public void dailyScan() {
        var result = workflow.scanCompanies();
        var packages = workflow.runDailyPreparation();
        log.info("Career Copilot daily scan checked {} companies, found {} jobs, prepared {} packages",
                result.companiesScanned(),
                result.jobsFound(),
                packages.size());
    }
}
