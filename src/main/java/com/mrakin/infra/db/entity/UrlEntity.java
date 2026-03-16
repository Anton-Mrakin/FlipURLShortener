package com.mrakin.infra.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "urls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_url", nullable = false, unique = true)
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true)
    private String shortCode;
}
