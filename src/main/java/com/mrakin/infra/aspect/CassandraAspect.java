package com.mrakin.infra.aspect;

import com.mrakin.domain.model.Url;
import com.mrakin.infra.db.repository.CassandraUrlAccessRepository;
import com.mrakin.usecases.UrlAccessedCassandraEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Aspect
@Component
@ConditionalOnBean(CassandraUrlAccessRepository.class)
@RequiredArgsConstructor
@Slf4j
public class CassandraAspect {

    private final CassandraUrlAccessRepository cassandraRepository;

    @Async
    @AfterReturning(pointcut = "@annotation(urlAccessedCassandraEvent)", returning = "url")
    public void logAccessToCassandra(UrlAccessedCassandraEvent urlAccessedCassandraEvent, Url url) {
        try {
            log.debug("Logging access to Cassandra for shortCode={}, fullUrl={}", 
                    url.getShortCode(), url.getOriginalUrl());
            cassandraRepository.incrementAccess(
                    url.getShortCode(),
                    url.getOriginalUrl(),
                    LocalDate.now()
            );
        } catch (Exception e) {
            log.error("Failed to log access to Cassandra", e);
        }
    }
}
