package com.mrakin.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrakin.infra.rest.dto.UrlRequestDto;
import com.mrakin.infra.rest.dto.UrlResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UrlShortenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shortener")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.test.iterations:1000}")
    private int iterations;

    @Value("${app.test.threads:100}")
    private int threadCount;

    @Value("${app.test.shorten-probability:0.2}")
    private double shortenProbability;

    @Value("${app.test.latency-threshold:200.0}")
    private double latencyThreshold;

    @Value("${app.test.throughput-threshold:500.0}")
    private double throughputThreshold;

    @Test
    void testShortenAndRetrieveUrl() throws Exception {
        String originalUrl = "https://example.com/very/long/url/that/needs/shortening";
        UrlRequestDto request = new UrlRequestDto(originalUrl);

        // 1. Shorten
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.shortCode").exists())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        UrlResponseDto response = objectMapper.readValue(content, UrlResponseDto.class);
        String shortCode = response.getShortCode();

        // 2. Retrieve
        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.shortCode").value(shortCode));
    }

    @Test
    void testPerformance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(iterations);
        CopyOnWriteArrayList<String> shortCodes = new CopyOnWriteArrayList<>();

        // 1. Initial data for reading
        for (int i = 0; i < 10; i++) {
            String url = "https://initial.com/" + i + "_" + UUID.randomUUID();
            MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UrlRequestDto(url))))
                    .andExpect(status().isOk())
                    .andReturn();
            String sc = objectMapper.readValue(result.getResponse().getContentAsString(), UrlResponseDto.class).getShortCode();
            shortCodes.add(sc);
        }

        AtomicLong totalLatency = new AtomicLong(0);
        AtomicLong shortenCount = new AtomicLong(0);
        AtomicLong getCount = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try {
                    long start = System.currentTimeMillis();
                    if (ThreadLocalRandom.current().nextDouble() < shortenProbability) {
                        String url = "https://example.com/p/" + UUID.randomUUID();
                        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new UrlRequestDto(url))))
                                .andExpect(status().isOk())
                                .andReturn();
                        String sc = objectMapper.readValue(result.getResponse().getContentAsString(), UrlResponseDto.class).getShortCode();
                        shortCodes.add(sc);
                        shortenCount.incrementAndGet();
                    } else {
                        String sc = shortCodes.get(ThreadLocalRandom.current().nextInt(shortCodes.size()));
                        mockMvc.perform(get("/api/v1/urls/" + sc))
                                .andExpect(status().isOk());
                        getCount.incrementAndGet();
                    }
                    long end = System.currentTimeMillis();
                    totalLatency.addAndGet(end - start);
                } catch (Exception e) {
                    log.error("Error during performance test", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        double totalDurationSec = (endTime - startTime) / 1000.0;
        double avgLatency = (double) totalLatency.get() / iterations;
        double throughput = (double) iterations / totalDurationSec;

        log.info("[DEBUG_LOG] Performance Results (Total Duration: {} s):", totalDurationSec);
        log.info("[DEBUG_LOG] Workload: Shorten = {}, Get = {}", shortenCount.get(), getCount.get());
        log.info("[DEBUG_LOG] Overall: Avg Latency = {} ms, Throughput = {} req/sec", avgLatency, throughput);

        assertTrue(avgLatency < latencyThreshold, "Average latency too high: " + avgLatency);
        assertTrue(throughput > throughputThreshold, "Throughput too low: " + throughput);
    }
}
