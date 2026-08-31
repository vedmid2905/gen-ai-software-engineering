package com.example.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Intelligent Customer Support System.
 *
 * <p>Run with {@code mvn spring-boot:run} or execute the generated JAR.
 * Set the {@code spring.profiles.active=prod} JVM property to activate the
 * PostgreSQL profile ({@code application-prod.properties}).
 */
@SpringBootApplication
public class SupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportApplication.class, args);
    }
}
