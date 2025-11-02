package com.swp391.gr3.ev_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EvManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvManagementApplication.class, args);
    }
}
