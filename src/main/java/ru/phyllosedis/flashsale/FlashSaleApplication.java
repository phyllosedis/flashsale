package ru.phyllosedis.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "ru.phyllosedis.flashsale")
public class FlashSaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashSaleApplication.class, args);
    }
}
