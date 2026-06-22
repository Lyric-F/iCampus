package com.example.demo.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // 存储在线用户的 WebSocket 会话，key=studentId
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URL 参数中获取 studentId，例如 /ws/notifications?studentId=12345
        String query = session.getUri().getQuery();
        String studentId = null;
        if (query != null && query.startsWith("studentId=")) {
            studentId = query.substring("studentId=".length());
        }
        if (studentId != null) {
            sessions.put(studentId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 移除断开的会话
        sessions.values().remove(session);
    }

    /**
     * 主动向指定用户推送消息
     */
    public void sendToUser(String studentId, String message) {
        WebSocketSession session = sessions.get(studentId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}