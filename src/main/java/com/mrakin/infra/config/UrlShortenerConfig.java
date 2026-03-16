package com.mrakin.infra.config;

import com.mrakin.domain.ports.UrlRepositoryPort;
import com.mrakin.usecases.UrlUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UrlShortenerConfig {

    @Bean
    public UrlUseCaseImpl urlUseCase(UrlRepositoryPort urlRepositoryPort) {
        return new UrlUseCaseImpl(urlRepositoryPort);
    }
}
