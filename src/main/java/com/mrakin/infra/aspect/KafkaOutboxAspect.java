package com.mrakin.infra.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gruelbox.transactionoutbox.TransactionOutbox;
import com.mrakin.domain.model.Url;
import com.mrakin.infra.outbox.OutboxPublisher;
import com.mrakin.usecases.UrlAccessedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class KafkaOutboxAspect {

    private final TransactionOutbox outbox;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();

    @AfterReturning(pointcut = "@annotation(urlAccessed)", returning = "result")
    public void publishEvent(JoinPoint joinPoint, UrlAccessedKafkaEvent urlAccessed, Object result) {
        if (result instanceof Url url) {
            log.debug("Intercepted URL access for short code: {}", url.getShortCode());
            
            String key = evaluateKey(joinPoint, urlAccessed.key());
            
            try {
                String urlJson = objectMapper.writeValueAsString(url);
                outbox.with().schedule(OutboxPublisher.class).publishUrlAccessed(urlJson, key);
                log.debug("Scheduled Kafka outbox event for URL: {}", url.getOriginalUrl());
            } catch (Exception e) {
                log.error("Failed to schedule Kafka outbox event", e);
            }
        }
    }

    private String evaluateKey(JoinPoint joinPoint, String expression) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = signature.getParameterNames();

            StandardEvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            return parser.parseExpression(expression).getValue(context, String.class);
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL expression: {}", expression, e);
            return null;
        }
    }
}
