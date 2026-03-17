package com.mrakin.usecases.validation;

import com.mrakin.domain.exception.UrlValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class MaxLengthUrlValidator implements UrlValidator {

    private final int maxLength;

    public MaxLengthUrlValidator(@Value("${app.max-url-length:2048}") int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void validate(String url) {
        if (url != null && url.length() > maxLength) {
            throw new UrlValidationException("URL length exceeds maximum limit of " + maxLength + " characters");
        }
    }
}
