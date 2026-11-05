package ru.phyllosedis.flashsale.user.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.phyllosedis.flashsale.shared.db.outbox.OutboxJdbcRepository;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupJob {

    private final OutboxJdbcRepository outboxJdbcRepository;

    @Value("${app.outbox.retention-days:7}")
    private long retentionDays;

    @Scheduled(
            fixedDelayString = "${app.outbox.cleanup-interval-ms:3600000}",
            initialDelayString = "${app.outbox.cleanup-initial-delay-ms:60000}")
    public void deleteOldOutboxRows() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(retentionDays);
        int deleted = outboxJdbcRepository.deleteCreatedBefore(threshold);
        if (deleted > 0) {
            log.info("Deleted {} outbox rows older than {}", deleted, threshold);
        }
    }
}
