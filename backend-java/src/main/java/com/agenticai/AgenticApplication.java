package com.agenticai;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class AgenticApplication {

    public static void main(String[] args) {
        // Load .env variables if .env file exists
        Path envPath = Paths.get("../.env");
        if (Files.exists(envPath)) {
             Dotenv dotenv = Dotenv.configure().directory("..").load();
             dotenv.entries().forEach(entry -> {
                 System.setProperty(entry.getKey(), entry.getValue());
             });
        }
        
        SpringApplication.run(AgenticApplication.class, args);
    }
}
