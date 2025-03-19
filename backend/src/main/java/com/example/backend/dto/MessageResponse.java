package com.example.backend.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private String content;
    private String sender;
    private LocalDateTime timestamp;
} 