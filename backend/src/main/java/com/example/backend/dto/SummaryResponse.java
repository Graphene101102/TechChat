package com.example.backend.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SummaryResponse {
    private Long contactId;
    private String summary;
} 