package com.example.backend.service;

import com.microsoft.playwright.*;
import com.example.backend.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessengerCrawlerService {
    @Value("${app.playwright.messenger-url}")
    private String messengerUrl;

    @Value("${app.chromium.user-data-dir}")
    private String userDataDir;
    
    private Page page;
    private Browser browser;
    private ChatService chatService;

    @Autowired
    public void setChatService(ChatService chatService) {
        this.chatService = chatService;
    }

    public void sendMessage(String contactId, String message) {
        try {
            log.info("Đang gửi tin nhắn đến {}: {}", contactId, message);
            
            // Đợi khung chat load
            page.waitForSelector("(//div[@class='html-div xexx8yu x4uap5 x18d9i69 xkhd6sd x1gslohp x11i5rnm x12nagc x1mh8g0r x1yc453h x126k92a x18lvrbx'])[last()]", 
                new Page.WaitForSelectorOptions().setTimeout(5000));
                
            // Tìm và click vào khung soạn tin nhắn
            Locator messageBox = page.locator("//div[@class='xzsf02u x1a2a7pz x1n2onr6 x14wi4xw x1iyjqo2 x1gh3ibb xisnujt xeuugli x1odjw0f notranslate']");
            messageBox.click();
            
            // Nhập tin nhắn
            page.type("div[class = 'x78zum5 x13a6bvl']", message, new Page.TypeOptions().setDelay(30));
            
            // Đợi 1 giây
            page.waitForTimeout(1000);
            
            // Gửi tin nhắn
            page.keyboard().press("Enter");
            
            // Đợi tin nhắn gửi xong
            page.waitForTimeout(1000);
            
            log.info("Đã gửi tin nhắn thành công");
            
        } catch (Exception e) {
            log.error("Lỗi khi gửi tin nhắn: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi tin nhắn: " + e.getMessage());
        }
    }
    
    public void watchForNewMessages() {
        try {
            // Tìm thẻ div chứa tin nhắn chưa đọc
            Locator unreadMessagesContainer = page.locator("//div[@class='x9f619 x1ja2u2z x78zum5 x2lah0s x1n2onr6 x1qughib x6s0dn4 xozqiw3 x1q0g3np'][.//div[@class='x6s0dn4 x78zum5 xozqiw3']][.//div[@class='x1i10hfl x1qjc9v5 xjbqb8w xjqpnuy xa49m3k xqeqjp1 x2hbi6w x13fuv20 xu3j5b3 x1q0q8m5 x26u7qi x972fbf xcfux6l x1qhh985 xm0m39n x9f619 x1ypdohk xdl72j9 x2lah0s xe8uvvx xdj266r x11i5rnm xat24cr x1mh8g0r x2lwn1j xeuugli xexx8yu x4uap5 x18d9i69 xkhd6sd x1n2onr6 x16tdsg8 x1hl2dhg xggy1nq x1ja2u2z x1t137rt x1o1ewxj x3x9cwd x1e5q0jg x13rtm0m x1q0g3np x87ps6o x1lku1pv x78zum5 x1a2a7pz']]");
            
            // Kiểm tra xem có tin nhắn chưa đọc không
            int unreadCount = unreadMessagesContainer.count();
            
            if (unreadCount > 0) {
                log.info("Phát hiện {} tin nhắn chưa đọc", unreadCount);
                
                // Click vào tin nhắn chưa đọc
                unreadMessagesContainer.first().click();
                
                // Đợi 1 giây để tin nhắn load
                page.waitForTimeout(1000);
                
                log.info("Đã click vào tin nhắn chưa đọc");
                
                // Lấy nội dung tin nhắn mới nhất
                String messageContent = page.locator("(//div[@class='html-div xexx8yu x4uap5 x18d9i69 xkhd6sd x1gslohp x11i5rnm x12nagc x1mh8g0r x1yc453h x126k92a x18lvrbx'])[last()]")
                    .textContent();
                
                // Lấy tên người gửi
                String senderName = page.locator("//span[@class='html-span xdj266r x11i5rnm xat24cr x1mh8g0r xexx8yu x4uap5 x18d9i69 xkhd6sd x1hl2dhg x16tdsg8 x1vvkbs xxymvpz x1dyh7pn']")
                    .textContent();

                // Lấy thời gian hiện tại khi đọc được tin nhắn
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
                
                log.info("Tin nhắn từ {}: {} vào lúc {}", senderName, messageContent, timestamp);
                
                // Chuyển đổi timestamp từ dạng text sang LocalDateTime
                LocalDateTime messageTime = parseMessageTime(timestamp);
                
                chatService.processNewMessage(senderName, messageContent, messageTime);
            }

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra tin nhắn mới: {}", e.getMessage());
        }
    }

    private LocalDateTime parseMessageTime(String timestamp) {
        try {
            // Xử lý các format thời gian khác nhau từ Messenger
            if (timestamp.contains("vừa xong")) {
                return LocalDateTime.now();
            } else if (timestamp.contains("phút")) {
                int minutes = Integer.parseInt(timestamp.replaceAll("\\D+", ""));
                return LocalDateTime.now().minusMinutes(minutes);
            } else if (timestamp.contains("giờ")) {
                int hours = Integer.parseInt(timestamp.replaceAll("\\D+", ""));
                return LocalDateTime.now().minusHours(hours);
            } else {
                // Format mặc định cho các timestamp khác
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                return LocalDateTime.parse(timestamp, formatter);
            }
        } catch (Exception e) {
            log.error("Lỗi parse timestamp: {}", e.getMessage());
            return LocalDateTime.now(); // Trả về thời gian hiện tại nếu không parse được
        }
    }

    @PostConstruct
    public void init() {
        try {
            Playwright playwright = Playwright.create();
            
            // Đường dẫn đến thư mục profile Chromium
            String userDataDir = System.getProperty("user.home") + 
                "/Library/Application Support/Chromium/Default";
            
            // Cấu hình cho Chromium với profile có sẵn
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setArgs(Arrays.asList(
                    "--user-data-dir=" + userDataDir,  // Sử dụng profile có sẵn
                    "--start-maximized",
                    "--disable-notifications"
                ))
                .setSlowMo(50);
                
                   // Khởi tạo browser với persistent context
            BrowserContext context = playwright.chromium().launchPersistentContext(
                Paths.get(userDataDir),
                new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(false)
                    .setViewportSize(960, 540)
                    .setTimeout(60000)
            );
            
            page = context.newPage();
            
            // Điều hướng đến Messenger
            page.navigate(messengerUrl);
            page.waitForLoadState();
            log.info("Browser initialized successfully");
            
            // Bắt đầu theo dõi tin nhắn
            startWatching();
            
        } catch (Exception e) {
            log.error("Failed to initialize browser: {}", e.getMessage());
        }
    }

    private volatile boolean isCheckingNewMessages = true; // Biến để theo dõi lượt kiểm tra

    public void startWatching() {
        Thread watchThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isCheckingNewMessages) {
                        // Kiểm tra tin nhắn thường
                        log.info("Đang kiểm tra tin nhắn thường...");
                        watchForNewMessages();
                    } else {
                        // Kiểm tra tin nhắn chờ
                        log.info("Đang kiểm tra tin nhắn chờ...");
                        checkPendingMessages();
                    }
                    
                    // Đảo trạng thái cho lần kiểm tra tiếp theo
                    isCheckingNewMessages = !isCheckingNewMessages;
                    
                    // Đợi 30 giây trước khi kiểm tra tiếp
                    Thread.sleep(30000);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Thread bị ngắt: {}", e.getMessage());
                    break;
                } catch (Exception e) {
                    log.error("Lỗi trong thread theo dõi: {}", e.getMessage());
                    try {
                        // Nếu có lỗi, đợi 5 giây trước khi thử lại
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Thread bị ngắt khi xử lý lỗi: {}", ie.getMessage());
                        break;
                    } finally {
                        try {
                            // Đảm bảo quay lại trang chính sau mỗi lần kiểm tra
                            page.navigate(messengerUrl);
                            page.waitForLoadState();
                        } catch (Exception ex) {
                            log.error("Lỗi khi quay về trang chính: {}", ex.getMessage());
                        }
                    }
                }
            }
        });
        
        watchThread.setDaemon(true);
        watchThread.setName("MessageWatcherThread");
        watchThread.start();
        
        log.info("Bắt đầu theo dõi tin nhắn (chế độ luân phiên)");
    }

    public void checkPendingMessages() {
        try {
            log.info("Bắt đầu kiểm tra tin nhắn chờ");
            
            // Click vào nút "Messenger" để mở menu
            Locator messengerButton = page.locator("div[class = 'x16n37ib']").first();
            if (messengerButton.isVisible()) {
                messengerButton.click();
                page.waitForTimeout(2000);
            }
            
            // Click vào "Tin nhắn đang chờ"
            Locator pendingButton = page.locator("//div[not(@class)]/div[@class='x1i10hfl xjbqb8w x1ejq31n xd10rxx x1sy0etr x17r0tee x972fbf xcfux6l x1qhh985 xm0m39n xe8uvvx x1hl2dhg xggy1nq x1o1ewxj x3x9cwd x1e5q0jg x13rtm0m x87ps6o x1lku1pv x1a2a7pz xjyslct x9f619 x1ypdohk x78zum5 x1q0g3np x2lah0s x1i6fsjq xfvfia3 xnqzcj9 x1gh759c x1n2onr6 x16tdsg8 x1ja2u2z x6s0dn4 x1y1aw1k xwib8y2 x1q8cg2c xnjli0'][1]");
            if (pendingButton.isVisible()) {
                pendingButton.click();
                log.info("Đã click vào Tin nhắn đang chờ");
                page.waitForTimeout(2000);
            }
            
            // Tìm tin nhắn chưa đọc trong danh sách chờ
            Locator pendingMessages = page.locator("//div[@class='x9f619 x1ja2u2z x78zum5 x2lah0s x1n2onr6 x1qughib x6s0dn4 xozqiw3 x1q0g3np'][.//div[@class='x6s0dn4 x78zum5 xozqiw3']][.//div[@class='x1i10hfl x1qjc9v5 xjbqb8w xjqpnuy xa49m3k xqeqjp1 x2hbi6w x13fuv20 xu3j5b3 x1q0q8m5 x26u7qi x972fbf xcfux6l x1qhh985 xm0m39n x9f619 x1ypdohk xdl72j9 x2lah0s xe8uvvx xdj266r x11i5rnm xat24cr x1mh8g0r x2lwn1j xeuugli xexx8yu x4uap5 x18d9i69 xkhd6sd x1n2onr6 x16tdsg8 x1hl2dhg xggy1nq x1ja2u2z x1t137rt x1o1ewxj x3x9cwd x1e5q0jg x13rtm0m x1q0g3np x87ps6o x1lku1pv x78zum5 x1a2a7pz']]");
            int pendingCount = pendingMessages.count();
            
            if (pendingCount > 0) {
                log.info("Phát hiện {} tin nhắn chờ chưa đọc", pendingCount);
                
                // Click vào tin nhắn đầu tiên
                pendingMessages.first().click();
                page.waitForTimeout(2000);
                
                log.info("Đã click vào tin nhắn chờ");
                
                // Lấy nội dung tin nhắn mới nhất
                String messageContent = page.locator("(//div[@class='html-div xexx8yu x4uap5 x18d9i69 xkhd6sd x1gslohp x11i5rnm x12nagc x1mh8g0r x1yc453h x126k92a x18lvrbx'])[last()]")
                    .last()
                    .textContent();
                    
                // Lấy tên người gửi
                String senderName = page.locator("//span[@class='html-span xdj266r x11i5rnm xat24cr x1mh8g0r xexx8yu x4uap5 x18d9i69 xkhd6sd x1hl2dhg x16tdsg8 x1vvkbs xxymvpz x1dyh7pn']")
                    .first()
                    .textContent();
                    
                // Lấy thời gian hiện tại
                LocalDateTime messageTime = LocalDateTime.now();
                
                log.info("Tin nhắn chờ từ {}: {} vào lúc {}", 
                    senderName, messageContent, messageTime);
                    
                // Xử lý tin nhắn chờ
                chatService.processNewMessage(senderName, messageContent, messageTime);
                
                // Quay lại 
                Locator back = page.locator("div[class = 'x9f619 x1n2onr6 x1ja2u2z x78zum5 xdt5ytf x2lah0s x193iq5w xeuugli x150jy0e x1e558r4 x10b6aqq x1yrsyyn']");
                if (back.isVisible()) {
                    back.click();
                    log.info("Đã quay lại");
                    page.waitForTimeout(2000);
                }
                page.waitForTimeout(1000);
            } else {
                log.info("Không có tin nhắn chờ mới");
            }
            
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra tin nhắn chờ: {}", e.getMessage());
            log.error("Chi tiết lỗi:", e);
        }
    }
} 