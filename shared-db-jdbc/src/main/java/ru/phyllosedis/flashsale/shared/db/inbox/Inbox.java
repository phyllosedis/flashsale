package ru.phyllosedis.flashsale.shared.db.inbox;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table(name = "inbox")
public class Inbox {
    @Id
    UUID id;
    String eventType;
    String status;
    ZonedDateTime createdAt;
    ZonedDateTime processedAt;
}
