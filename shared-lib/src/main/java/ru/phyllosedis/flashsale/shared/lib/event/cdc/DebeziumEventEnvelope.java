package ru.phyllosedis.flashsale.shared.lib.event.cdc;

public record DebeziumEventEnvelope<T>(
        T before,
        T after,
        String op
) {

}
