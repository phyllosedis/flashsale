package ru.phyllosedis.flashsale.domain.seat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "seat")
@Getter
public class Seat {
    @Id
    private Long id;
}
