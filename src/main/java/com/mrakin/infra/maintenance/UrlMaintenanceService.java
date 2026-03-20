package com.mrakin.infra.maintenance;

import com.mrakin.domain.ports.UrlRepositoryPort;
import com.mrakin.infra.db.repository.CassandraUrlAccessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlMaintenanceService {
    private final UrlRepositoryPort urlRepositoryPort;
    private final CassandraUrlAccessRepository cassandraRepository;

    @Value("${app.url-limit:10000}")
    private long urlLimit;

    /**
     * Periodic cleanup of old URLs to maintain the limit.
     * Uses Cassandra to calculate rank based on access patterns over the last 3 weeks.
     * Rank = 0.5 * (accesses last week) + 0.3 * (accesses 2 weeks ago) + 0.1 * (accesses 3 weeks ago)
     * Deletes URLs with lowest rank when exceeding the storage limit.
     * Timeout is set to 2 minutes to allow for Cassandra scanning.
     */
    @Transactional(timeout = 120)
    @Async("cleanupExecutor")
    @Scheduled(fixedRateString = "${app.maintenance.rate:PT1M}")
    public void cleanupOldUrls() {
        try {
            long currentCount = urlRepositoryPort.count();
            if (currentCount <= urlLimit) {
                log.debug("Current URL count ({}) is within limit ({}), no cleanup needed",
                        currentCount, urlLimit);
                return;
            }

            long toDelete = currentCount - urlLimit;
            log.info("Current count {} exceeds limit {}. Need to delete {} URLs based on rank",
                    currentCount, urlLimit, toDelete);

            // Get ONLY the lowest ranked URLs from Cassandra (last 3 weeks)
            // This is optimized to return only what we need for deletion
            LocalDate now = LocalDate.now();
            LocalDate threeWeeksAgo = now.minusWeeks(3);

            Map<String, Double> lowestRankedUrls = cassandraRepository.getLowestRankedUrls(
                    threeWeeksAgo,
                    now,
                    (int) toDelete
            );

            if (lowestRankedUrls.isEmpty()) {
                log.warn("No URLs found in Cassandra to delete, falling back to oldest URLs");
                long deletedCount = urlRepositoryPort.deleteOldest(urlLimit);
                if (deletedCount > 0) {
                    log.info("Fallback cleanup: {} records removed", deletedCount);
                }
                return;
            }

            // Delete URLs from PostgreSQL in batches
            int deletedCount = 0;
            int batchSize = 100;
            List<String> urlsToDelete = new ArrayList<>(lowestRankedUrls.keySet());

            for (int i = 0; i < urlsToDelete.size(); i += batchSize) {
                int end = Math.min(i + batchSize, urlsToDelete.size());
                List<String> batch = urlsToDelete.subList(i, end);

                for (String fullUrl : batch) {
                    try {
                        urlRepositoryPort.deleteByOriginalUrl(fullUrl);
                        deletedCount++;
                    } catch (Exception e) {
                        log.error("Failed to delete URL: {}", fullUrl, e);
                    }
                }

                log.debug("Deleted batch {}/{}, total deleted: {}",
                        (i / batchSize) + 1,
                        (urlsToDelete.size() + batchSize - 1) / batchSize,
                        deletedCount);
            }

            log.info("Rank-based cleanup completed: {} URLs deleted (target was {})",
                    deletedCount, toDelete);

        } catch (Exception e) {
            log.error("Error during scheduled URL cleanup", e);
        }
    }
}
