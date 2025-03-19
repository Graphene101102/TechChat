package com.example.backend.controller;

import com.example.backend.entity.*;
import com.example.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {
    private final ChatService chatService;
    
    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<List<Contact>>> getAllContacts() {
        try {
            List<Contact> contacts = chatService.getAllContacts();
            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách liên hệ thành công", contacts));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách liên hệ: ", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Lỗi khi lấy danh sách liên hệ: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/messages/{contactId}")
    public ResponseEntity<ApiResponse<List<Message>>> getMessageHistory(@PathVariable Long contactId) {
        try {
            List<Message> messages = chatService.getMessageHistory(contactId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy lịch sử tin nhắn thành công", messages));
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử tin nhắn cho contact {}: ", contactId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Lỗi khi lấy lịch sử tin nhắn: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/summary/{contactName}")
    public ResponseEntity<ApiResponse<ChatSummary>> getChatSummary(@PathVariable String contactName) {
        try {
            Contact contact = chatService.findContactByName(contactName);
            if (contact == null) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Không tìm thấy liên hệ với tên: " + contactName, null));
            }
            ChatSummary summary = chatService.findChatSummaryByContact(contact);
            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy tóm tắt chat thành công", summary));
        } catch (Exception e) {
            log.error("Lỗi khi lấy tóm tắt chat cho {}: ", contactName, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Lỗi khi lấy tóm tắt chat: " + e.getMessage(), null));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Message>> sendMessage(@RequestBody MessageRequest request) {
        try {
            Message message = chatService.processNewMessage(
                request.getSenderName(), 
                request.getContent(),
                request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now()
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Gửi tin nhắn thành công", message));
        } catch (Exception e) {
            log.error("Lỗi khi gửi tin nhắn: ", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Lỗi khi gửi tin nhắn: " + e.getMessage(), null));
        }
    }
}

@Data
class MessageRequest {
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
}

@Data
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
} 