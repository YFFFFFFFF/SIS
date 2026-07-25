package com.sis.iids;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IidsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IidsApplication.class, args);
    }
}
