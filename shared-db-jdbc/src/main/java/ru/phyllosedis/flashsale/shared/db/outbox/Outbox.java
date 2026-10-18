package ru.phyllosedis.flashsale.shared.db.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.UUID;

@Table(name = "outbox")
@Getter
@Setter
public class Outbox {
    @Id
    private UUID id;
    private UUID entityId;
    private String eventType;
    private String status;
    private String payload;
    private ZonedDateTime createdAt;
}
