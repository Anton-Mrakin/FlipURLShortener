package com.mrakin.infra.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrakin.domain.event.UrlShortenedEvent;
import com.mrakin.domain.model.Url;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public void publishUrlAccessed(String urlJson, String key) {
        try {
            Url url = objectMapper.readValue(urlJson, Url.class);
            String domain = (key != null && !key.isEmpty()) ? key : extractDomain(url.getOriginalUrl());
            kafkaTemplate.send("url-accessed", domain, url);
            log.info("Successfully published UrlAccessedEvent to Kafka for URL: {}", url.getOriginalUrl());
        } catch (Exception e) {
            log.error("Failed to publish UrlAccessedEvent to Kafka", e);
            throw new RuntimeException(e);
        }
    }

    public void publishUrlShortened(String eventJson) {
        try {
            UrlShortenedEvent event = objectMapper.readValue(eventJson, UrlShortenedEvent.class);
            jmsTemplate.convertAndSend("url-shortened", event);
            log.info("Successfully published UrlShortenedEvent to ActiveMQ for code: {}", event.getShortCode());
        } catch (Exception e) {
            log.error("Failed to publish UrlShortenedEvent to ActiveMQ", e);
            throw new RuntimeException(e);
        }
    }

    private String extractDomain(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return (host != null) ? host : "";
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
