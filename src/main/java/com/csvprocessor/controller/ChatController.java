package com.csvprocessor.controller;

import com.csvprocessor.service.OllamaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final OllamaService ollamaService;

    public ChatController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestParam String question, @RequestBody MultipartFile file ) throws IOException {
    	String filename = file.getOriginalFilename();
    	 if (filename == null || !filename.endsWith(".csv")) {
             return ResponseEntity.badRequest().body(Map.of("question", question, "answer", "Only CSV files are accepted"));
         }
        String answer = ollamaService.askQuestion(question, file.getInputStream(), filename);
        answer =  answer.replace("\n", "").replace("\r", "").replace("-", ".").trim();
        return ResponseEntity.ok(Map.of("question", question, "answer", answer));
    }
}
