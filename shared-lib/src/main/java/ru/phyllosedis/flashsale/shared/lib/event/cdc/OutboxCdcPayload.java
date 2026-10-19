package ru.phyllosedis.flashsale.shared.lib.event.cdc;

import java.util.UUID;

public record OutboxCdcPayload(
        UUID id,
        UUID entity_id,
        String event_type,
        String status,
        String payload
) {
}
