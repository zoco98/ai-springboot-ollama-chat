package com.csvprocessor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.csvprocessor.util.ReadFileUtil;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private String lastMetricsContext = "";

    public void setMetricsContext(List<String[]> fileRows, String filename) {
    	StringBuilder sb = new StringBuilder();

    	sb.append(filename + ":\n\n");

    	if (!fileRows.isEmpty()) {

    	    String[] headers = fileRows.get(0);

    	    sb.append("Columns: ")
    	      .append(String.join(", ", headers))
    	      .append("\n\n");

    	    sb.append("Records:\n");

    	    for (int i = 1; i < fileRows.size(); i++) {
    	        String[] row = fileRows.get(i);

    	        for (int j = 0; j < headers.length && j < row.length; j++) {
    	            sb.append(headers[j])
    	              .append("=")
    	              .append(row[j]);

    	            if (j < headers.length - 1) {
    	                sb.append(", ");
    	            }
    	        }

    	        sb.append("\n");
    	    }
    	}

    	this.lastMetricsContext = sb.toString();
    }

    public String askQuestion(String question, InputStream inputStream, String filename) {
        if (lastMetricsContext.isEmpty()) {
            try {
				List<String[]> fileRows = ReadFileUtil.readAll(inputStream);
				setMetricsContext(fileRows, filename);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }

        String prompt = """
                You are a CSV data analysis assistant.

				Use ONLY the provided CSV data.
				
				User question:
				%s
				
				CSV data:
				%s
				
				Rules:
				1. Carefully inspect every record before answering.
				2. Use the exact column names from the CSV.
				3. For "highest", "maximum", or "most" questions, compare ALL values in the requested column.
				4. For "lowest", "minimum", or "least" questions, compare ALL values in the requested column.
				5. For average or total questions, calculate using ALL records.
				6. Never include unrelated months or values.
				7. Never guess or invent data.
				8. Return only the final answer.
				9. Do not use Markdown.
				10. Do not use bullet points.
				11. Keep the answer to one sentence.
				
				Example:
				Question: Which month had the highest healthcare expense?
				Answer: December had the highest healthcare expense at $1800.
				
				Now answer the user's question.
                """.formatted(question, lastMetricsContext);

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(ollamaUrl, HttpMethod.POST, request, Map.class);
            return (String) response.getBody().get("response");

        } catch (Exception e) {
            return "Ollama is not running. Start it with: ollama serve\nError: " + e.getMessage();
        }
    }
}
