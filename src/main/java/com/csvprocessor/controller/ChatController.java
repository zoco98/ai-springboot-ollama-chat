package com.csvprocessor.controller;

import com.csvprocessor.service.AIService;
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
    private final AIService aiService;

    public ChatController(OllamaService ollamaService, AIService aiService) {
        this.ollamaService = ollamaService;
        this.aiService = aiService;
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
    
    @PostMapping("api/ai/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestParam String question, @RequestBody MultipartFile file ) throws IOException {

    	String filename = file.getOriginalFilename();
    	 if (filename == null || !filename.endsWith(".csv")) {
             return ResponseEntity.badRequest().body(Map.of("question", question, "answer", "Only CSV files are accepted"));
         }
        String answer = aiService.askQuestion(question, file.getInputStream(), filename);
        answer =  answer.replace("\n", "").replace("\r", "").replace("-", ".").trim();
        return ResponseEntity.ok(Map.of("question", question, "answer", answer));
    
    }
    
    @PostMapping("ai/ask")
    public String askAI(@RequestBody String question) {
        return aiService.askAI(question);
    }
}
