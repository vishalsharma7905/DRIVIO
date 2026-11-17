package com.drivio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrivioApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrivioApplication.class, args);
    }
}

