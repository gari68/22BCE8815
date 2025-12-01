package com.example.webhooksolver.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SqlLoader {

    public String loadSqlForQuestion(int questionNumber) {
        String path = switch (questionNumber) {
            case 1 -> "sql/question1.sql";
            case 2 -> "sql/question2.sql";
            default -> throw new IllegalArgumentException("Unknown question number: " + questionNumber);
        };

        try {
            ClassPathResource r = new ClassPathResource(path);
            byte[] b = r.getInputStream().readAllBytes();
            return new String(b, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SQL file: " + path, e);
        }
    }
}
