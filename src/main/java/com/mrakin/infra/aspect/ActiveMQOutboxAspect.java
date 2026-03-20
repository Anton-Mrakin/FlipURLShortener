package com.mrakin.infra.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.mrakin.domain.event.UrlShortenedEvent;
import com.mrakin.domain.model.Url;
import com.mrakin.infra.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(name = "activemq.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class ActiveMQOutboxAspect {

    private final TransactionOutbox outbox;
    private final ObjectMapper objectMapper;
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @AfterReturning(pointcut = "@annotation(com.mrakin.usecases.UrlShortenedActiveMQEvent)", returning = "result")
    public void publishEvent(Object result) {
        if (result instanceof Url url) {
            String requestId = MDC.get(MDC_REQUEST_ID_KEY);
            log.debug("Intercepted URL shorten for code: {}, requestId: {}", url.getShortCode(), requestId);
            
            UrlShortenedEvent payload = new UrlShortenedEvent(
                    url.getOriginalUrl(),
                    url.getShortCode(),
                    requestId
            );
            
            try {
                String eventJson = objectMapper.writeValueAsString(payload);
                outbox.with().schedule(OutboxPublisher.class).publishUrlShortened(eventJson);
                log.debug("Scheduled ActiveMQ outbox event for code: {}", url.getShortCode());
            } catch (Exception e) {
                log.error("Failed to schedule ActiveMQ outbox event", e);
            }
        }
    }
}
