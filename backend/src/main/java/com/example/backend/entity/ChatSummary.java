package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_summaries")
public class ChatSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "contact_id")
    private Contact contact;
    
    @Column(columnDefinition = "TEXT")
    private String summary;

    public String getContactId() {
        return contact.getId().toString();
    }

    public void setContactId(String contactId) {
        // This method is no longer used as the contact ID is now managed by the Contact entity
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
} 