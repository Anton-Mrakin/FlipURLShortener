package com.mrakin.domain.ports;

import com.mrakin.domain.model.Url;

import java.util.Optional;

public interface UrlRepositoryPort {
    Url save(Url url);
    Optional<Url> findByShortCode(String shortCode);
    Optional<Url> findByOriginalUrl(String originalUrl);
    long deleteOldest(long urlLimit);
    void updateLastAccessed(String shortCode);
    long count();
    void deleteByOriginalUrl(String originalUrl);
}
