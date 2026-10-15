package ru.phyllosedis.flashsale.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "ru.phyllosedis.flashsale.domain")
public class Config {
}
