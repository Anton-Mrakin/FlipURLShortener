package com.mrakin.integration;

import com.mrakin.domain.event.UrlShortenedEvent;
import com.mrakin.domain.model.Url;
import com.mrakin.usecases.ShortenUrlUseCase;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class ActiveMQOutboxIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shortener")
            .withUsername("user")
            .withPassword("password")
            .withReuse(true);

    @Container
    static GenericContainer<?> activemq = new GenericContainer<>(DockerImageName.parse("symptoma/activemq:5.18.0"))
            .withExposedPorts(61616)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.activemq.broker-url", () -> "tcp://" + activemq.getHost() + ":" + activemq.getMappedPort(61616));
        // Disable Kafka and Redis for this test
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    private ShortenUrlUseCase shortenUrlUseCase;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Test
    void testUrlShortenedEventSentToActiveMQ() throws Exception {
        String originalUrl = "https://example.com/activemq-test-" + UUID.randomUUID();
        String requestId = "req-" + UUID.randomUUID();
        
        // Simulating RequestIdFilter behavior
        MDC.put("requestId", requestId);
        try {
            // 1. Shorten URL (triggers event publication via Aspect + Modulith Outbox)
            Url url = shortenUrlUseCase.shorten(originalUrl);
            assertNotNull(url);
        } finally {
            MDC.remove("requestId");
        }

        // 2. Verify ActiveMQ message
        try (JMSContext context = connectionFactory.createContext()) {
            Queue queue = context.createQueue("url-shortened");
            Message message = context.createConsumer(queue).receive(10000); // 10s timeout
            
            assertNotNull(message, "No message received from ActiveMQ");
            
            UrlShortenedEvent event = message.getBody(UrlShortenedEvent.class);
            log.info("Received message from ActiveMQ: {}", event);
            
            assertNotNull(event);
            assertTrue(event.getOriginalUrl().contains(originalUrl));
            assertTrue(event.getRequestId().contains(requestId));
        }
    }
}
