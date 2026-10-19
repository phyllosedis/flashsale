package ru.phyllosedis.flashsale.shared.db.inbox;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table(name = "inbox")
@Getter
@Setter
@Builder
public class Inbox {
    @Id
    private UUID id;
    private UUID entityId;
    private String eventType;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
}
