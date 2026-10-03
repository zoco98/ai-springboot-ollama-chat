package com.csvprocessor.controller;

import com.csvprocessor.service.CsvService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/csv")
public class CsvController {

    private final CsvService csvService;

    public CsvController(CsvService csvService) {
        this.csvService = csvService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CSV Processor API is running");
    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadCsv(
            @RequestParam("newFile") MultipartFile file1,
            @RequestParam("oldFile") MultipartFile file2,
            @RequestParam("uri") String uri,
            @RequestParam("clientName") String clientName) {

        if (file1.isEmpty() || file2.isEmpty()) {
            return ResponseEntity.badRequest().body("Both files are required".getBytes());
        }

        String filename1 = file1.getOriginalFilename();
        String filename2 = file2.getOriginalFilename();
        if (filename1 == null || !filename1.endsWith(".csv")
                || filename2 == null || !filename2.endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Only CSV files are accepted".getBytes());
        }

        try {
            byte[] modifiedCsv = csvService.processCsv(
                    file1.getInputStream(), file2.getInputStream(), uri, clientName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modified_output.csv");

            return ResponseEntity.ok().headers(headers).body(modifiedCsv);

        } catch (Exception e) {
            return ResponseEntity.unprocessableEntity()
                    .body(("Error processing CSV: " + e.getMessage()).getBytes());
        }
    }
}
