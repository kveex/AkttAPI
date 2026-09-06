package me.kveex.akttapispringed.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {
    @GetMapping("/")
    public ResponseEntity<String> start() {
        log.info("Hiiii");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/secure/test")
    public ResponseEntity<String> securityTest() {
        String securityTest = "Security Test";
        log.info(securityTest);
        return ResponseEntity.ok().body(securityTest);
    }
}
