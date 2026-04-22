package com.bank.payee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // CWE-613 — enables @Scheduled session-eviction in MfaService
public class PayeeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayeeApplication.class, args);
    }
}
