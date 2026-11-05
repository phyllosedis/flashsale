package ru.phyllosedis.flashsale.payment.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.phyllosedis.flashsale.shared.db.inbox.InboxJdbcRepository;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxCleanupJob {

    private final InboxJdbcRepository inboxJdbcRepository;

    @Value("${app.inbox.retention-days:14}")
    private long retentionDays;

    @Scheduled(
            fixedDelayString = "${app.inbox.cleanup-interval-ms:3600000}",
            initialDelayString = "${app.inbox.cleanup-initial-delay-ms:60000}")
    public void deleteOldProcessedEvents() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(retentionDays);
        int deleted = inboxJdbcRepository.deleteProcessedBefore(threshold);
        if (deleted > 0) {
            log.info("Deleted {} processed inbox rows older than {}", deleted, threshold);
        }
    }
}
