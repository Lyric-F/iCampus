package com.example.demo.service;

import com.example.demo.entity.Message;
import com.example.demo.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Transactional
    public Message sendMessage(String fromUser, String toUser, String content) {
        Message msg = new Message();
        msg.setFromUser(fromUser);
        msg.setToUser(toUser);
        msg.setContent(content);
        msg.setCreateTime(LocalDateTime.now());
        msg.setRead(false);
        return messageRepository.save(msg);
    }

    public List<Message> getMessages(String user1, String user2) {
        return messageRepository.findByFromUserAndToUserOrFromUserAndToUserOrderByCreateTimeAsc(
                user1, user2, user2, user1);
    }

    public List<Map<String, Object>> getConversations(String userId) {
        List<Message> latestMsgs = messageRepository.findLatestMessagesForUser(userId);
        if (latestMsgs.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取未读数
        List<Object[]> unreadResults = messageRepository.countUnreadByFromUser(userId);
        Map<String, Integer> unreadMap = new HashMap<>();
        for (Object[] row : unreadResults) {
            String fromUser = (String) row[0];
            Long count = (Long) row[1];
            unreadMap.put(fromUser, count.intValue());
        }

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Message msg : latestMsgs) {
            String otherUser = msg.getFromUser().equals(userId) ? msg.getToUser() : msg.getFromUser();
            int unread = unreadMap.getOrDefault(otherUser, 0);
            Map<String, Object> conv = new HashMap<>();
            conv.put("userId", otherUser);
            conv.put("lastMessage", msg.getContent());
            conv.put("lastTime", msg.getCreateTime());
            conv.put("unread", unread);
            conversations.add(conv);
        }
        return conversations;
    }

    @Transactional
    public void markMessagesAsRead(String currentUserId, String otherUserId) {
        // 标记从 otherUserId 发给 currentUserId 的消息为已读
        messageRepository.markMessagesAsRead(otherUserId, currentUserId);
    }

    public int getUnreadCount(String userId) {
        return messageRepository.countByToUserAndIsReadFalse(userId);
    }
    @Transactional
    public void deleteConversation(String userId, String otherUserId) {
        messageRepository.deleteByFromUserAndToUserOrFromUserAndToUser(userId, otherUserId, otherUserId, userId);
    }

    @Transactional
    public void deleteAllMessages(String userId) {
        messageRepository.deleteByFromUserOrToUser(userId);
    }
}