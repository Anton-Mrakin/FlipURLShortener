package com.mrakin.infra.db.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("url_access_by_shard")
public class UrlAccessByShard {

    @PrimaryKeyColumn(name = "shard_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Integer shardId;

    @PrimaryKeyColumn(name = "access_day", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private LocalDate accessDay;

    @PrimaryKeyColumn(name = "full_url", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private String fullUrl;

    @Column("access_count")
    private Long accessCount;
}
