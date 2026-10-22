package com.csvprocessor.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/ollama")
public class OllamaSpringAIController {

	private ChatClient chatClient;
	
	public OllamaSpringAIController(OllamaChatModel chatModel) {
		this.chatClient = ChatClient.create(chatModel);
	}
	
	@GetMapping("/ask/{prompt}")
	public ResponseEntity<String, String> getResponse(@PathVariable String prompt){
		ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
		String modelName = chatResponse.getMetadata().getModel();
		String response = chatResponse.getResult().getOutput().getText();
		
		return new ResponseEntity<>(modelName,response);
		
	}
}
