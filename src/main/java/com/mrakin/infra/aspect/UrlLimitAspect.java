package com.mrakin.infra.aspect;

import com.mrakin.infra.maintenance.UrlMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class UrlLimitAspect {

    private final UrlMaintenanceService urlMaintenanceService;

    @AfterReturning(pointcut = "@annotation(com.mrakin.usecases.CleanupUrlLimit)")
    public void manageUrlLimit() {
        urlMaintenanceService.cleanupOldUrls();
    }
}
