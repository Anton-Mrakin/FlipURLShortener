package com.mrakin.infra.rest.mapper;

import com.mrakin.domain.model.Url;
import com.mrakin.infra.rest.dto.UrlResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UrlRestMapper {
    UrlResponseDto toResponse(Url domain);
}
