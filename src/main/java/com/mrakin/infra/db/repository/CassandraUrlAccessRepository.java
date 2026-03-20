package com.mrakin.infra.db.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CassandraUrlAccessRepository {

    private final CqlSession cqlSession;
    private final int cassandraShardCount;

    private static final int MAX_URLS_TO_SCAN = 100_000; // Safety limit
    private static final int FETCH_SIZE_PER_SHARD = 10_000; // Per-shard fetch size

    private PreparedStatement incrementStatement;
    private PreparedStatement selectRangeStatement;

    private void ensurePreparedStatements() {
        if (incrementStatement == null) {
            incrementStatement = cqlSession.prepare(
                    "UPDATE url_access_by_shard SET access_count = access_count + 1 " +
                            "WHERE shard_id = ? AND access_day = ? AND full_url = ?"
            );
        }
        if (selectRangeStatement == null) {
            selectRangeStatement = cqlSession.prepare(
                    "SELECT full_url, access_day, access_count FROM url_access_by_shard " +
                            "WHERE shard_id = ? AND access_day >= ? AND access_day <= ?"
            );
        }
    }

    public int calculateShardId(String fullUrl) {
        CRC32 crc32 = new CRC32();
        crc32.update(fullUrl.getBytes());
        return (int) (Math.abs(crc32.getValue()) % cassandraShardCount);
    }

    public void incrementAccess(String shortCode, String fullUrl, LocalDate accessDay) {
        ensurePreparedStatements();
        int shardId = calculateShardId(fullUrl);

        try {
            cqlSession.execute(incrementStatement.bind(shardId, accessDay, fullUrl));
            log.debug("Incremented access count for fullUrl={}, shardId={}, day={}", fullUrl, shardId, accessDay);
        } catch (Exception e) {
            log.error("Failed to increment access count for fullUrl={}: {}", fullUrl, e.getMessage());
        }
    }

    /**
     * Get URLs ranked by access patterns, with built-in safety limits.
     * Returns bottom N URLs (lowest rank) for cleanup purposes.
     *
     * @param fromDate start date for query
     * @param toDate end date for query
     * @param limit maximum number of URLs to return
     * @return map of fullUrl -> rank, sorted by rank (lowest first)
     */
    public Map<String, Double> getLowestRankedUrls(LocalDate fromDate, LocalDate toDate, int limit) {
        ensurePreparedStatements();

        LocalDate now = LocalDate.now();
        LocalDate weekAgo = now.minusWeeks(1);
        LocalDate twoWeeksAgo = now.minusWeeks(2);
        LocalDate threeWeeksAgo = now.minusWeeks(3);

        // Use min-heap to keep only bottom N URLs by rank
        PriorityQueue<UrlRank> lowestRanked = new PriorityQueue<>(
                limit + 1,
                Comparator.comparingDouble(UrlRank::rank).reversed() // Max heap (highest rank on top)
        );

        int totalProcessed = 0;
        Map<String, WeeklyAccessCount> urlAccessMap = new HashMap<>();

        // Query all shards with fetch size limit
        for (int shardId = 0; shardId < cassandraShardCount; shardId++) {
            if (totalProcessed >= MAX_URLS_TO_SCAN) {
                log.warn("Reached safety limit of {} URLs, stopping scan at shard {}",
                        MAX_URLS_TO_SCAN, shardId);
                break;
            }

            ResultSet rs = cqlSession.execute(
                    selectRangeStatement.bind(shardId, fromDate, toDate)
                            .setPageSize(FETCH_SIZE_PER_SHARD)
            );

            int processedInShard = 0;
            for (Row row : rs) {
                if (totalProcessed >= MAX_URLS_TO_SCAN) {
                    break;
                }

                String fullUrl = row.getString("full_url");
                LocalDate accessDay = row.getLocalDate("access_day");
                long count = row.getLong("access_count");

                urlAccessMap.putIfAbsent(fullUrl, new WeeklyAccessCount());
                WeeklyAccessCount weeklyCount = urlAccessMap.get(fullUrl);

                // Categorize by week
                if (accessDay.isAfter(weekAgo) || accessDay.isEqual(weekAgo)) {
                    weeklyCount.week1Count += count;
                } else if (accessDay.isAfter(twoWeeksAgo) || accessDay.isEqual(twoWeeksAgo)) {
                    weeklyCount.week2Count += count;
                } else if (accessDay.isAfter(threeWeeksAgo) || accessDay.isEqual(threeWeeksAgo)) {
                    weeklyCount.week3Count += count;
                }

                processedInShard++;
                totalProcessed++;
            }

            log.debug("Processed {} URLs from shard {}", processedInShard, shardId);
        }

        log.info("Processed {} total URLs across {} shards", totalProcessed, cassandraShardCount);

        // Calculate ranks and keep only lowest N
        for (Map.Entry<String, WeeklyAccessCount> entry : urlAccessMap.entrySet()) {
            WeeklyAccessCount counts = entry.getValue();
            double rank = 0.5 * counts.week1Count + 0.3 * counts.week2Count + 0.1 * counts.week3Count;

            lowestRanked.offer(new UrlRank(entry.getKey(), rank));

            // Keep only bottom N by removing highest rank
            if (lowestRanked.size() > limit) {
                lowestRanked.poll();
            }
        }

        // Convert to map, sorted by rank (lowest first)
        return lowestRanked.stream()
                .sorted(Comparator.comparingDouble(UrlRank::rank))
                .collect(Collectors.toMap(
                        UrlRank::url,
                        UrlRank::rank,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private record UrlRank(String url, double rank) {}

    private static class WeeklyAccessCount {
        long week1Count = 0;
        long week2Count = 0;
        long week3Count = 0;
    }
}
