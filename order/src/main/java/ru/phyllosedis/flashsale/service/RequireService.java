package ru.phyllosedis.flashsale.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RequireService {
    KafkaTemplate kafkaTemplate;
}
