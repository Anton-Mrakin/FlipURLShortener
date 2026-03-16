package com.mrakin.usecases;

import com.mrakin.domain.model.Url;

public interface GetOriginalUrlUseCase {
    Url getOriginal(String shortCode);
}
