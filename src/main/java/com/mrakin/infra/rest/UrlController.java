package com.mrakin.infra.rest;

import com.mrakin.infra.rest.dto.UrlRequestDto;
import com.mrakin.infra.rest.dto.UrlResponseDto;
import com.mrakin.infra.rest.mapper.UrlRestMapper;
import com.mrakin.usecases.GetOriginalUrlUseCase;
import com.mrakin.usecases.ShortenUrlUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final ShortenUrlUseCase shortenUrlUseCase;
    private final GetOriginalUrlUseCase getOriginalUrlUseCase;
    private final UrlRestMapper urlRestMapper;

    @PostMapping("/shorten")
    public ResponseEntity<UrlResponseDto> shorten(@Valid @RequestBody UrlRequestDto request) {
        var url = shortenUrlUseCase.shorten(request.getOriginalUrl());
        return ResponseEntity.ok(urlRestMapper.toResponse(url));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlResponseDto> getOriginal(@PathVariable String shortCode) {
        var url = getOriginalUrlUseCase.getOriginal(shortCode);
        return ResponseEntity.ok(urlRestMapper.toResponse(url));
    }
}
