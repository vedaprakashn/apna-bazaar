package com.apnabazaar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApnaBazaarApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApnaBazaarApplication.class, args);
    }
}
