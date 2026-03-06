package com.example.autoservice;

import com.example.autoservice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AutoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdmin(UserService userService) {
        return args -> {
            try {
                userService.registerUser("admin", "Admin123!", "ROLE_ADMIN");
                System.out.println("Admin user created successfully");
            } catch (Exception e) {
                System.out.println("Admin user already exists or creation failed: " + e.getMessage());
            }
        };
    }
}