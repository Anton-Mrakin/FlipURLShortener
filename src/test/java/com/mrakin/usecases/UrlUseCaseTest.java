package com.mrakin.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mrakin.domain.exception.UrlNotFoundException;
import com.mrakin.domain.model.Url;
import com.mrakin.domain.ports.UrlRepositoryPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlUseCaseTest {

    @Mock
    private UrlRepositoryPort urlRepositoryPort;

    private UrlUseCaseImpl urlUseCase;

    @BeforeEach
    void setUp() {
        urlUseCase = new UrlUseCaseImpl(urlRepositoryPort);
    }

    @ParameterizedTest
    @CsvSource({
        "https://google.com, goog123",
        "https://github.com, gith456",
        "https://openai.com, open789"
    })
    void shorten_NewUrl_ShouldSaveAndReturn(String originalUrl, String expectedShortCode) {
        // Given
        when(urlRepositoryPort.findByOriginalUrl(originalUrl)).thenReturn(Optional.empty());
        when(urlRepositoryPort.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Url result = urlUseCase.shorten(originalUrl);

        // Then
        assertNotNull(result);
        assertEquals(originalUrl, result.getOriginalUrl());
        assertNotNull(result.getShortCode());
        verify(urlRepositoryPort).save(any(Url.class));
    }

    @Test
    void shorten_ExistingUrl_ShouldReturnFromDb() {
        // Given
        String originalUrl = "https://existing.com";
        Url existingUrl = Url.builder()
                .originalUrl(originalUrl)
                .shortCode("exist123")
                .build();
        when(urlRepositoryPort.findByOriginalUrl(originalUrl)).thenReturn(Optional.of(existingUrl));

        // When
        Url result = urlUseCase.shorten(originalUrl);

        // Then
        assertEquals(existingUrl, result);
        verify(urlRepositoryPort, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
        "short1, https://url1.com",
        "short2, https://url2.com"
    })
    void getOriginal_ExistingCode_ShouldReturnUrl(String shortCode, String originalUrl) {
        // Given
        Url url = Url.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .build();
        when(urlRepositoryPort.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        // When
        Url result = urlUseCase.getOriginal(shortCode);

        // Then
        assertEquals(originalUrl, result.getOriginalUrl());
    }

    @Test
    void getOriginal_NonExistingCode_ShouldThrowException() {
        // Given
        String shortCode = "notfound";
        when(urlRepositoryPort.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UrlNotFoundException.class, () -> urlUseCase.getOriginal(shortCode));
    }
}
