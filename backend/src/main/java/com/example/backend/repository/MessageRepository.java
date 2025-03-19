package com.example.backend.repository;

import com.example.backend.entity.Message;
import com.example.backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByContactOrderByIdDesc(Contact contact);
    // hoặc nếu muốn tìm bằng contactId
    List<Message> findByContact_IdOrderByIdDesc(Long contactId);
} 