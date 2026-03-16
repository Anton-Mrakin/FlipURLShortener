package com.mrakin.infra.db.mapper;

import com.mrakin.domain.model.Url;
import com.mrakin.infra.db.entity.UrlEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UrlDbMapper {
    UrlEntity toEntity(Url domain);
    Url toDomain(UrlEntity entity);
}
