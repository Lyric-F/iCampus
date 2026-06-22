package com.example.demo.controller;

import com.example.demo.entity.Message;
import com.example.demo.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestParam String from, @RequestParam String to, @RequestParam String content) {
        Message msg = messageService.sendMessage(from, to, content);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", msg);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<Message>> getMessages(@RequestParam String user1, @RequestParam String user2) {
        return ResponseEntity.ok(messageService.getMessages(user1, user2));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(@RequestParam String userId) {
        return ResponseEntity.ok(messageService.getConversations(userId));
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markRead(@RequestParam String currentUserId, @RequestParam String otherUserId) {
        messageService.markMessagesAsRead(currentUserId, otherUserId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@RequestParam String userId) {
        int count = messageService.getUnreadCount(userId);
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete-conversation")
    public ResponseEntity<Map<String, Object>> deleteConversation(@RequestParam String userId, @RequestParam String otherUserId) {
        messageService.deleteConversation(userId, otherUserId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/delete-all")
    public ResponseEntity<Map<String, Object>> deleteAllMessages(@RequestParam String userId) {
        messageService.deleteAllMessages(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}