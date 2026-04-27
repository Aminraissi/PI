package org.example.gestionvente.Services;

import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final String API_KEY = System.getenv("GROQ_API_KEY");

    public List<String> getRecommendations(String items) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
        {
          "model": "llama-3.1-8b-instant",
          "messages": [
            {
              "role": "user",
              "content": "List 3 agricultural products related to %s separated by commas only"
            }
          ]
        }
        """.formatted(items);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);

        // 🔥 extract AI text
        List<Map> choices = (List<Map>) response.getBody().get("choices");
        Map message = (Map) choices.get(0).get("message");
        String content = (String) message.get("content");

        // 👉 split into list
        return Arrays.stream(content.split(","))
                .map(String::trim)
                .toList();
    }


    public List<String> getProductRecommendations(String name, String category) {


        System.out.println("🔥 AI SERVICE CALLED");
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = """
    Suggest 5 agricultural products similar to:
    %s (%s)

    Rules:
    - Only product names
    - No explanation
    - Comma separated
    """.formatted(name, category);

        // 🔥 FIX JSON ISSUE
        prompt = prompt.replace("\n", " ");

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(API_URL, request, Map.class);

        List<Map> choices = (List<Map>) response.getBody().get("choices");
        Map msg = (Map) choices.get(0).get("message");

        String content = (String) msg.get("content");
        System.out.println("AI RAW RESPONSE: " + content);

        return Arrays.stream(content.split(","))
                .map(String::trim)
                .toList();
    }
}
