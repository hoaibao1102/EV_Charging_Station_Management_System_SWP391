package com.swp391.gr3.ev_management;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EvManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvManagementApplication.class, args);
    }
}
