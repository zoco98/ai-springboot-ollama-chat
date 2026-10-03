package com.csvprocessor.service;

import java.io.InputStream;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.csvprocessor.util.ReadFileUtil;

/*
 *  prompt(question)
      ↓
	Create AI prompt
	      ↓
	call()
	      ↓
	Send request to LLM
	      ↓
	content()
	      ↓
	Extract generated text
 * */

@Service
public class AIService {

    private final ChatClient chatClient;
    /*
     * Spring AI auto-configures a ChatClient.Builder for us when the appropriate chat model is configured.
     * */
    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String askAI(String question) {

        return chatClient
                .prompt(question)
                .call()
                .content();
    }
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
    	String lastMetricsContext = "";
    	try {
			List<String[]> fileRows = ReadFileUtil.readAll(inputStream);
			setMetricsContext(fileRows, filename);
		} catch (Exception e) {
			e.printStackTrace();
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
    	
        return chatClient
                .prompt() 
                .user(prompt) 
                .call() 
                .content();
    }
}