package com.mrakin.infra.db;

import com.mrakin.domain.model.Url;
import com.mrakin.domain.ports.UrlRepositoryPort;
import com.mrakin.infra.db.entity.UrlEntity;
import com.mrakin.infra.db.mapper.UrlDbMapper;
import com.mrakin.infra.db.repository.JpaUrlRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlRepositoryAdapter implements UrlRepositoryPort {

    private final JpaUrlRepository jpaUrlRepository;
    private final UrlDbMapper urlDbMapper;

    @Override
    @Transactional
    public Url save(Url url) {
        UrlEntity entity = urlDbMapper.toEntity(url);
        UrlEntity saved = jpaUrlRepository.save(entity);
        return urlDbMapper.toDomain(saved);
    }

    @Override
    @Transactional
    @Retryable(value = ObjectOptimisticLockingFailureException.class, 
               maxAttempts = 10, 
               backoff = @Backoff(delay = 50, multiplier = 2))
    public Optional<Url> findByShortCode(String shortCode) {
        return jpaUrlRepository.findByShortCode(shortCode)
                .map(entity -> {
                    entity.setLastAccessed(LocalDateTime.now());
                    jpaUrlRepository.save(entity);
                    return urlDbMapper.toDomain(entity);
                });
    }

    @Override
    @Transactional
    @Retryable(value = ObjectOptimisticLockingFailureException.class, 
               maxAttempts = 10, 
               backoff = @Backoff(delay = 50, multiplier = 2))
    public Optional<Url> findByOriginalUrl(String originalUrl) {
        return jpaUrlRepository.findByOriginalUrl(originalUrl)
                .map(entity -> {
                    entity.setLastAccessed(LocalDateTime.now());
                    jpaUrlRepository.save(entity);
                    return urlDbMapper.toDomain(entity);
                });
    }

    @Override
    public long count() {
        return jpaUrlRepository.count();
    }

    @Override
    @Transactional
    public void deleteOldest() {
        jpaUrlRepository.findFirstByOrderByLastAccessedAsc()
                .ifPresent(jpaUrlRepository::delete);
    }

    @Override
    @Transactional
    @Retryable(value = ObjectOptimisticLockingFailureException.class, 
               maxAttempts = 10, 
               backoff = @Backoff(delay = 50, multiplier = 2))
    public void updateLastAccessed(String shortCode) {
        jpaUrlRepository.findByShortCode(shortCode)
                .ifPresent(entity -> {
                    entity.setLastAccessed(LocalDateTime.now());
                    jpaUrlRepository.save(entity);
                });
    }
}
