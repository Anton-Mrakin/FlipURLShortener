package com.mrakin.usecases.validation;

import com.mrakin.domain.exception.UrlValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    @Test
    void testNotEmptyUrlValidator() {
        UrlValidator validator = new NotEmptyUrlValidator();
        
        assertDoesNotThrow(() -> validator.validate("http://google.com"));
        
        UrlValidationException ex = assertThrows(UrlValidationException.class, 
                () -> validator.validate(""));
        assertEquals("URL cannot be empty", ex.getMessage());
        
        assertThrows(UrlValidationException.class, () -> validator.validate(null));
        assertThrows(UrlValidationException.class, () -> validator.validate("   "));
    }

    @Test
    void testMaxLengthUrlValidator() {
        UrlValidator validator = new MaxLengthUrlValidator(10);
        
        assertDoesNotThrow(() -> validator.validate("short"));
        assertDoesNotThrow(() -> validator.validate("1234567890"));
        
        UrlValidationException ex = assertThrows(UrlValidationException.class, 
                () -> validator.validate("too long url"));
        assertTrue(ex.getMessage().contains("exceeds maximum limit of 10"));
    }
}
