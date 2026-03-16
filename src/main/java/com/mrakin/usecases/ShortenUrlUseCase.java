package com.mrakin.usecases;

import com.mrakin.domain.model.Url;

public interface ShortenUrlUseCase {
    Url shorten(String originalUrl);
}
