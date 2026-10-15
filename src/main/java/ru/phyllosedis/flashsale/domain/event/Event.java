package ru.phyllosedis.flashsale.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Table(name = "\"event\"")
@Entity
@Getter
public class Event {
    @Id
    Long id;
    @Column(name = "name", nullable = false, length = 50)
    String name;
    @Column(name = "type", nullable = false, length = 20)
    String type;
}
