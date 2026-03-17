package com.mrakin.infra.rest.mapper;

import com.mrakin.domain.model.Url;
import org.springframework.stereotype.Component;

@Component
public class UrlRestMapper {

    public Url toDomain(String originalUrl) {
        if (originalUrl == null) {
            return null;
        }
        return Url.builder()
                .originalUrl(originalUrl)
                .build();
    }

    public String toShortCode(Url url) {
        if (url == null) {
            return null;
        }
        return url.getShortCode();
    }

    public String toOriginalUrl(Url url) {
        if (url == null) {
            return null;
        }
        return url.getOriginalUrl();
    }
}
