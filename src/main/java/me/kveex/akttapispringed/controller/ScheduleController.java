package me.kveex.akttapispringed.controller;

import lombok.extern.slf4j.Slf4j;
import me.kveex.akttapispringed.service.impl.ScheduleParserServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(path = "/api/v1/schedule")
@Slf4j
public class ScheduleController {
    private final ScheduleParserServiceImpl scheduleParserService;

    public ScheduleController(ScheduleParserServiceImpl scheduleParserService) {
        this.scheduleParserService = scheduleParserService;
    }

    @PostMapping("/pdf")
    public ResponseEntity<String> loadPdf(@RequestParam("file") MultipartFile file) throws IOException {
            this.scheduleParserService.parsePdf(file.getBytes());
            return ResponseEntity.ok().build();
    }
}
