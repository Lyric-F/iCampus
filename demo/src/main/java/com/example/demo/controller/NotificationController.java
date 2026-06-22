package com.example.demo.controller;

import com.example.demo.entity.Notification;
import com.example.demo.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 获取通知列表
    @GetMapping("/list")
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam String studentId) {
        List<Notification> notifications = notificationService.getNotifications(studentId);
        return ResponseEntity.ok(notifications);
    }

    // 删除单条通知
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteNotification(@RequestParam Long notificationId,
                                                                  @RequestParam String studentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.deleteNotification(notificationId, studentId);
            response.put("success", true);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // 标记单条通知为已读
    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@RequestParam Long notificationId,
                                                          @RequestParam String studentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.markAsRead(notificationId, studentId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // 标记所有通知为已读
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam String studentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            notificationService.markAllAsRead(studentId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // 获取未读通知数量（用于红点）
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@RequestParam String studentId) {
        int count = notificationService.getUnreadCount(studentId);
        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}