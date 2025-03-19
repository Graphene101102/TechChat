package com.example.backend.service;

import com.example.backend.dto.MessageResponse;
import com.example.backend.dto.SummaryResponse;
import com.example.backend.entity.Contact;
import com.example.backend.entity.Message;
import com.example.backend.entity.ChatSummary;
import com.example.backend.repository.ChatSummaryRepository;
import com.example.backend.repository.ContactRepository;
import com.example.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final ChatSummaryRepository chatSummaryRepository;
    private final GeminiService geminiService;
    private final SimpMessagingTemplate messagingTemplate;
    private MessengerCrawlerService messengerCrawlerService;

    @Autowired
    public void setMessengerCrawlerService(MessengerCrawlerService messengerCrawlerService) {
        this.messengerCrawlerService = messengerCrawlerService;
    }

    // @PostConstruct
    // public void init() {
    //     // Khởi tạo theo dõi tin nhắn
    //     messengerCrawlerService.startWatching(this);
    // }

    @Transactional
    public Message processNewMessage(String senderName, String messageContent, LocalDateTime timestamp) {
        if (!StringUtils.hasText(messageContent)) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (!StringUtils.hasText(senderName)) {
            throw new IllegalArgumentException("Sender name cannot be empty");
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        
        try {
            log.info("Processing new message from: {} at {}", senderName, timestamp);
            
            // 1. Tìm hoặc tạo contact
            Contact contact = findOrCreateContact(senderName, messageContent);
            
            // 2. Lưu tin nhắn người dùng với timestamp
            Message userMessage = saveUserMessage(contact, senderName, messageContent, timestamp);
            
            // 3. Lấy chat summary hiện tại
            String currentSummary = getCurrentSummary(contact);
            
            // 4. Tạo câu trả lời từ AI
            String aiResponse = generateAIResponse(currentSummary, userMessage.getContent(), messageContent);
            
            // 5. Lưu tin nhắn trả lời
            Message botMessage = saveBotMessage(contact, aiResponse);
            
            // 6. Gửi tin nhắn qua Messenger
            messengerCrawlerService.sendMessage(senderName, aiResponse);
            
            // 7. Gửi WebSocket updates
            sendWebSocketUpdates(contact, botMessage);
            
            log.info("Successfully processed message for: {}", senderName);
            return botMessage;
            
        } catch (Exception e) {
            log.error("Error processing message: sender={}, content={}, timestamp={}", 
                senderName, messageContent, timestamp, e);
            throw new RuntimeException("Failed to process message: " + e.getMessage(), e);
        }
    }

    @Transactional
    private Message saveUserMessage(Contact contact, String sender, String content, LocalDateTime timestamp) {
        try {
            Message message = new Message();
            message.setContent(content);
            message.setSender(sender);
            message.setContact(contact);
            message.setTimestamp(timestamp);
            Message savedMessage = messageRepository.save(message);
            
            // Cập nhật last message của contact
            contact.setLastMessage(content);
            contactRepository.save(contact);
            
            return savedMessage;
        } catch (Exception e) {
            log.error("Lỗi khi lưu tin nhắn user: {}", e.getMessage());
            throw new RuntimeException("Không thể lưu tin nhắn user", e);
        }
    }
    

    private void sendWebSocketUpdates(Contact contact, Message botMessage) {
        try {
            // Gửi tin nhắn mới
            messagingTemplate.convertAndSend(
                "/topic/messages/" + contact.getId(), 
                MessageResponse.builder()
                    .id(botMessage.getId())
                    .content(botMessage.getContent())
                    .sender(botMessage.getSender())
                    .timestamp(botMessage.getTimestamp())
                    .build()
            );
            
            // Gửi summary mới
            messagingTemplate.convertAndSend(
                "/topic/summary/" + contact.getId(),
                SummaryResponse.builder()
                    .contactId(contact.getId())
                    .summary(getCurrentSummary(contact))
                    .build()
            );
        } catch (Exception e) {
            log.error("Error sending WebSocket updates for contact {}: {}", contact.getId(), e.getMessage());
            // Không throw exception vì đây là chức năng phụ
        }
    }

    private String getCurrentSummary(Contact contact) {
        ChatSummary chatSummary = chatSummaryRepository.findByContact(contact);
        return chatSummary != null ? chatSummary.getSummary() : "";
    }

    private String generateAIResponse(String currentSummary, String lastMessage, String newMessage) {
        String prompt = String.format(
            "Dựa vào:\n" +
            "1. Tóm tắt lịch sử chat:\n%s\n\n" +
            "2. Tin nhắn cuối cùng:\n%s\n\n" +
            "3. Tin nhắn mới:\n%s\n\n" +
            "Hãy trả lời như một người bạn thân thiện, tự nhiên và gần gũi. " +
            "Nếu không có thông tin về lịch sử chat, hãy tập trung vào tin nhắn mới nhất.",
            StringUtils.hasText(currentSummary) ? currentSummary : "Chưa có lịch sử chat",
            StringUtils.hasText(lastMessage) ? lastMessage : "Chưa có tin nhắn trước đó",
            newMessage
        );
        return geminiService.generateResponse(prompt);
    }

    @Transactional
    private Message saveBotMessage(Contact contact, String content) {
        try {
            Message botMessage = new Message();
            botMessage.setContent(content);
            botMessage.setSender("BOT");
            botMessage.setContact(contact);
            botMessage.setTimestamp(LocalDateTime.now());
            return messageRepository.save(botMessage);
        } catch (Exception e) {
            log.error("Lỗi khi lưu tin nhắn bot: {}", e.getMessage());
            throw new RuntimeException("Không thể lưu tin nhắn bot", e);
        }
    }

    private void updateChatSummary(Contact contact, String currentSummary, String userMessage, String botResponse) {
        String prompt = String.format(
            "Dựa vào:\n" +
            "1. Tóm tắt lịch sử chat hiện tại:\n%s\n\n" +
            "2. Tin nhắn mới:\n" +
            "User: %s\n" +
            "Bot: %s\n\n" +
            "Hãy tạo một tóm tắt mới về toàn bộ cuộc trò chuyện 1 cách ngắn gọn và dễ hiểu",
            currentSummary,
            userMessage,
            botResponse
        );

        ChatSummary summary = chatSummaryRepository.findByContact(contact);
		if (summary == null) {
			summary = new ChatSummary();
            summary.setContact(contact);
		}
        
        String newSummary = geminiService.generateSummary(prompt);
		summary.setSummary(newSummary);
		chatSummaryRepository.save(summary);
	}

    private Contact findOrCreateContact(String name, String initialMessage) {
        Contact contact = contactRepository.findByName(name);
        if (contact == null) {
            contact = new Contact();
            contact.setName(name);
            contact.setLastMessage(initialMessage);
            contact = contactRepository.save(contact);
        }
        return contact;
    }

    private void updateContactLastMessage(Contact contact, String lastMessage) {
        contact.setLastMessage(lastMessage);
        contactRepository.save(contact);
    }

    // Các phương thức truy vấn
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    public List<Message> getMessageHistory(Contact contact) {
        return messageRepository.findByContactOrderByIdDesc(contact);
    }

    public List<Message> getMessageHistory(Long contactId) {
        return messageRepository.findByContact_IdOrderByIdDesc(contactId);
    }

    public ChatSummary findChatSummaryByContact(Contact contact) {
        return chatSummaryRepository.findByContact(contact);
    }

    public ChatSummary saveChatSummary(ChatSummary summary) {
        return chatSummaryRepository.save(summary);
    }

    // Các phương thức hỗ trợ
    public Contact findContactByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Contact name cannot be empty");
        }
        return contactRepository.findByName(name);
    }

    public Contact saveContact(Contact contact) {
        return contactRepository.save(contact);
    }

    public Message saveMessage(Message message) {
        return messageRepository.save(message);
	}

    public Contact findContactById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Contact ID cannot be null");
        }
        return contactRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));
    }

    public String getCurrentSummary(Long contactId) {
        Contact contact = findContactById(contactId);
        return getCurrentSummary(contact);
    }

    public ChatSummary getChatSummaryByContactId(Long contactId) {
        Contact contact = findContactById(contactId);
        return findChatSummaryByContact(contact);
	}
}