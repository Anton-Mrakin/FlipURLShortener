package com.mrakin.infra.db.repository;

import com.mrakin.infra.db.entity.UrlEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUrlRepository extends JpaRepository<UrlEntity, UUID> {
    Optional<UrlEntity> findByShortCode(String shortCode);
    Optional<UrlEntity> findByOriginalUrl(String originalUrl);
}
