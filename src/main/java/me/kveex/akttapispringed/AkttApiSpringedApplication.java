package me.kveex.akttapispringed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AkttApiSpringedApplication {

    static void main(String[] args) {
        SpringApplication.run(AkttApiSpringedApplication.class, args);
    }
}
