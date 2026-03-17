package com.mrakin.usecases.validation;

import com.mrakin.domain.exception.UrlValidationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class NotEmptyUrlValidator implements UrlValidator {
    @Override
    public void validate(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new UrlValidationException("URL cannot be empty");
        }
    }
}
