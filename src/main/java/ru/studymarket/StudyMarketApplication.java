package ru.studymarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class StudyMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyMarketApplication.class, args);
    }
}
