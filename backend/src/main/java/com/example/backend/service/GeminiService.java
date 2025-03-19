package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
    @Value("${app.gemini.api-key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public String generateResponse(String prompt) {
        try {
            log.info("Gọi Gemini API với prompt: {}", prompt);
            
            // Tạo request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            contents.put("role", "user");
            contents.put("parts", Collections.singletonList(
                Collections.singletonMap("text", prompt)
            ));
            requestBody.put("contents", Collections.singletonList(contents));
            
            // Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Tạo URL với API key
            String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-pro-exp-02-05:generateContent?key=%s",
                apiKey
            );
            
            // Gọi API
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                Map.class
            );
            
            // Parse response
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");
            
            log.info("Gemini response: {}", text);
            return text;
            
        } catch (Exception e) {
            log.error("Error generating response: {}", e.getMessage());
            return "Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau.";
        }
    }
    
    public String generateSummary(String prompt) {
        try {
            log.info("Tạo summary với prompt: {}", prompt);
            return generateResponse(prompt); // Sử dụng cùng logic với generateResponse
        } catch (Exception e) {
            log.error("Error generating summary: {}", e.getMessage());
            return "Không thể tạo tóm tắt do lỗi hệ thống.";
        }
    }
}