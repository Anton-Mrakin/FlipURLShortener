package com.mrakin.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrakin.infra.rest.dto.UrlRequestDto;
import com.mrakin.infra.rest.dto.UrlResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
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
        int iterations = 1000;
        long startShorten = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            String url = "https://example.com/" + i;
            UrlRequestDto request = new UrlRequestDto(url);
            mockMvc.perform(post("/api/v1/urls/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        long endShorten = System.currentTimeMillis();
        System.out.println("[DEBUG_LOG] Time to shorten " + iterations + " URLs: " + (endShorten - startShorten) + " ms");
        System.out.println("[DEBUG_LOG] Avg shorten time: " + (double)(endShorten - startShorten) / iterations + " ms");

        // Для теста получения возьмем последний код
        String lastUrl = "https://example.com/" + (iterations - 1);
        MvcResult lastResult = mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UrlRequestDto(lastUrl))))
                .andReturn();
        String shortCode = objectMapper.readValue(lastResult.getResponse().getContentAsString(), UrlResponseDto.class).getShortCode();

        long startRetrieve = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            mockMvc.perform(get("/api/v1/urls/" + shortCode))
                    .andExpect(status().isOk());
        }
        long endRetrieve = System.currentTimeMillis();
        System.out.println("[DEBUG_LOG] Time to retrieve " + iterations + " URLs: " + (endRetrieve - startRetrieve) + " ms");
        System.out.println("[DEBUG_LOG] Avg retrieve time: " + (double)(endRetrieve - startRetrieve) / iterations + " ms");
    }
}
