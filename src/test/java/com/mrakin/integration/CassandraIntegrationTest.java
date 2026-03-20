package com.mrakin.integration;

import com.mrakin.infra.db.repository.CassandraUrlAccessRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class CassandraIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shortener")
            .withUsername("user")
            .withPassword("password")
            .withReuse(true);

    @Container
    static CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:4.1")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("cassandra.contact-points", cassandra::getHost);
        registry.add("cassandra.port", () -> cassandra.getMappedPort(9042));
        registry.add("cassandra.local-datacenter", () -> "datacenter1");

        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CassandraUrlAccessRepository cassandraRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private JmsTemplate jmsTemplate;

    @Test
    void shouldTrackUrlAccessInCassandraWithSharding() throws InterruptedException {
        String originalUrl = "https://example.com/cassandra-test-" + System.currentTimeMillis();

        // 1. Shorten URL
        ResponseEntity<String> shortenResponse = restTemplate.postForEntity("/api/v1/urls/shorten", originalUrl, String.class);
        assertThat(shortenResponse.getStatusCode().is2xxSuccessful()).isTrue();
        String shortCode = shortenResponse.getBody();
        assertThat(shortCode).isNotNull();

        // 2. Access URL multiple times
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> getResponse = restTemplate.getForEntity("/api/v1/urls/" + shortCode, String.class);
            assertThat(getResponse.getStatusCode().is2xxSuccessful()).isTrue();
        }

        // Wait for async Cassandra writes to complete
        Thread.sleep(2000);

        // 3. Verify sharding works correctly
        int shardId = cassandraRepository.calculateShardId(originalUrl);
        assertThat(shardId).isBetween(0, 9);

        // 4. Query lowest ranked URLs
        LocalDate now = LocalDate.now();
        LocalDate threeWeeksAgo = now.minusWeeks(3);
        Map<String, Double> rankedUrls = cassandraRepository.getLowestRankedUrls(threeWeeksAgo, now, 100);

        assertThat(rankedUrls).isNotEmpty();
        assertThat(rankedUrls).containsKey(originalUrl);
        assertThat(rankedUrls.get(originalUrl)).isGreaterThan(0.0);
    }

    @Test
    void shouldCalculateRankBasedOnWeeklyAccess() throws InterruptedException {
        String url1 = "https://example.com/rank-test-1-" + System.currentTimeMillis();
        String url2 = "https://example.com/rank-test-2-" + System.currentTimeMillis();

        // Create and access first URL
        ResponseEntity<String> shortenResponse1 = restTemplate.postForEntity("/api/v1/urls/shorten", url1, String.class);
        assertThat(shortenResponse1.getStatusCode().is2xxSuccessful()).isTrue();
        String shortCode1 = shortenResponse1.getBody();

        // Access first URL 10 times (should get higher rank)
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity("/api/v1/urls/" + shortCode1, String.class);
        }

        // Create and access second URL
        ResponseEntity<String> shortenResponse2 = restTemplate.postForEntity("/api/v1/urls/shorten", url2, String.class);
        assertThat(shortenResponse2.getStatusCode().is2xxSuccessful()).isTrue();
        String shortCode2 = shortenResponse2.getBody();

        // Access second URL 2 times (should get lower rank)
        for (int i = 0; i < 2; i++) {
            restTemplate.getForEntity("/api/v1/urls/" + shortCode2, String.class);
        }

        // Wait for async Cassandra writes
        Thread.sleep(2000);

        // Query and verify ranking - get lowest 2 URLs
        LocalDate now = LocalDate.now();
        LocalDate threeWeeksAgo = now.minusWeeks(3);
        Map<String, Double> lowestRankedUrls = cassandraRepository.getLowestRankedUrls(threeWeeksAgo, now, 2);

        assertThat(lowestRankedUrls).containsKeys(url1, url2);
        // url2 should have lower rank than url1 (0.5 * 2 = 1.0 vs 0.5 * 10 = 5.0)
        // Since we get lowest ranked, url2 should come first
        assertThat(lowestRankedUrls.get(url2)).isLessThan(lowestRankedUrls.get(url1));
    }

    @Test
    void shouldDistributeUrlsAcrossShards() {
        // Test that different URLs get distributed across different shards
        String[] testUrls = {
                "https://example.com/test1",
                "https://example.com/test2",
                "https://example.com/test3",
                "https://example.com/test4",
                "https://example.com/test5",
                "https://example.com/test6",
                "https://example.com/test7",
                "https://example.com/test8",
                "https://example.com/test9",
                "https://example.com/test10"
        };

        boolean[] shardsUsed = new boolean[10];
        for (String url : testUrls) {
            int shardId = cassandraRepository.calculateShardId(url);
            shardsUsed[shardId] = true;
        }

        // At least 5 different shards should be used for 10 different URLs
        int usedShardCount = 0;
        for (boolean used : shardsUsed) {
            if (used) usedShardCount++;
        }
        assertThat(usedShardCount).isGreaterThanOrEqualTo(5);
    }
}
