package com.example.demo.service;

import com.example.demo.entity.Notification;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationWebSocketHandler webSocketHandler;

    // 获取通知列表
    public List<Notification> getNotifications(String studentId) {
        return notificationRepository.findByTargetStudentIdOrderByCreateTimeDesc(studentId);
    }

    // 创建通知并实时推送
    @Transactional
    public void createNotification(String targetStudentId, String sourceStudentId,
                                   Long postId, String type, String message) {
        if (targetStudentId.equals(sourceStudentId)) return;
        Notification notification = new Notification();
        notification.setTargetStudentId(targetStudentId);
        notification.setSourceStudentId(sourceStudentId);
        notification.setPostId(postId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        // 实时推送消息给目标用户（如果在线）
        webSocketHandler.sendToUser(targetStudentId, message);
    }

    // 删除通知
    @Transactional
    public void deleteNotification(Long notificationId, String studentId) {
        notificationRepository.deleteByIdAndTargetStudentId(notificationId, studentId);
    }

    // 标记单条为已读
    @Transactional
    public void markAsRead(Long notificationId, String studentId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        if (!notification.getTargetStudentId().equals(studentId)) {
            throw new RuntimeException("无权操作");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // 标记所有为已读
    @Transactional
    public void markAllAsRead(String studentId) {
        notificationRepository.markAllAsReadByStudentId(studentId);
    }

    // 获取未读数量
    public int getUnreadCount(String studentId) {
        return notificationRepository.countByTargetStudentIdAndIsReadFalse(studentId);
    }

    // 删除帖子相关的所有通知
    @Transactional
    public void deleteByPostId(Long postId) {
        notificationRepository.deleteByPostId(postId);
    }
}