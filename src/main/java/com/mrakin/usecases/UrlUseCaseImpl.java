package com.mrakin.usecases;

import com.mrakin.domain.exception.UrlNotFoundException;
import com.mrakin.domain.model.Url;
import com.mrakin.domain.ports.UrlRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UrlUseCaseImpl implements ShortenUrlUseCase, GetOriginalUrlUseCase {

    private final UrlRepositoryPort urlRepositoryPort;
    private final ShortCodeGenerator shortCodeGenerator = new ShortCodeGenerator();

    @Override
    public Url shorten(String originalUrl) {
        return urlRepositoryPort.findByOriginalUrl(originalUrl)
                .orElseGet(() -> {
                    String shortCode = shortCodeGenerator.generate(originalUrl);
                    Url url = Url.builder()
                            .originalUrl(originalUrl)
                            .shortCode(shortCode)
                            .build();
                    return urlRepositoryPort.save(url);
                });
    }

    @Override
    public Url getOriginal(String shortCode) {
        return urlRepositoryPort.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL with short code " + shortCode + " not found"));
    }
}
