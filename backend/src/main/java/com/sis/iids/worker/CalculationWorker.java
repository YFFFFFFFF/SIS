package com.sis.iids.worker;

import com.sis.iids.calculation.CalculationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CalculationWorker {

    private final CalculationService calculationService;
    private final boolean enabled;

    public CalculationWorker(CalculationService calculationService,
                             @Value("${iids.worker.enabled:true}") boolean enabled) {
        this.calculationService = calculationService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${iids.worker.poll-ms:1000}")
    public void runScheduled() {
        if (enabled) {
            runPendingOnce();
        }
    }

    public boolean runPendingOnce() {
        return calculationService.runNextPendingTask();
    }
}
