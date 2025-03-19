package com.example.backend.repository;

import com.example.backend.entity.ChatSummary;
import com.example.backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSummaryRepository extends JpaRepository<ChatSummary, Long> {
    ChatSummary findByContact(Contact contact);
} 