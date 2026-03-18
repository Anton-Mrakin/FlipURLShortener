package com.mrakin.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlShortenedEvent implements Serializable {
    private String originalUrl;
    private String shortCode;
    private String requestId;
}
