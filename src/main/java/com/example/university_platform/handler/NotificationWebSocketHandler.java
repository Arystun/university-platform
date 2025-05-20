package com.example.university_platform.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    // Потокобезопасная карта для хранения сессий пользователей
    // Ключ - username, Значение - WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = getUsernameFromSession(session);
        if (username != null) {
            sessions.put(username, session);
            logger.info("WebSocket connection established for user: {}. Session ID: {}", username, session.getId());
            // Можно отправить подтверждающее сообщение клиенту
            // session.sendMessage(new TextMessage("Connection established for user: " + username));
        } else {
            logger.warn("WebSocket connection established but username not found in principal. Session ID: {}. Closing session.", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Username not available in principal"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // В нашем случае сервер будет только отправлять сообщения, а не получать от клиента.
        // Но если бы получал, здесь была бы логика обработки.
        String username = getUsernameFromSession(session);
        logger.info("Received WebSocket message from user {}: {}", username, message.getPayload());
        // Пример ответа:
        // session.sendMessage(new TextMessage("Server received your message: " + message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = getUsernameFromSession(session);
        if (username != null) {
            sessions.remove(username);
            logger.info("WebSocket connection closed for user: {}. Session ID: {}. Status: {}", username, session.getId(), status);
        } else {
            // Если username не был определен при подключении, просто логируем закрытие сессии
            // Поиск по session ID, если нужно убрать из какой-то другой коллекции
            sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
            logger.info("WebSocket connection closed for unknown user. Session ID: {}. Status: {}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String username = getUsernameFromSession(session);
        logger.error("WebSocket transport error for user: {} (Session ID: {}): {}",
                username != null ? username : "unknown",
                session.getId(),
                exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason(exception.getMessage()));
        }
        // Также удаляем сессию, если она еще есть
        if (username != null) {
            sessions.remove(username);
        } else {
            sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        }
    }

    /**
     * Отправляет сообщение конкретному пользователю, если он подключен.
     * @param username Имя пользователя.
     * @param message Текстовое сообщение для отправки.
     * @return true, если сообщение было отправлено, false - если пользователь не найден или сессия закрыта.
     */
    public boolean sendMessageToUser(String username, String message) {
        WebSocketSession userSession = sessions.get(username);
        if (userSession != null && userSession.isOpen()) {
            try {
                logger.info("Sending WebSocket message to user {}: {}", username, message);
                userSession.sendMessage(new TextMessage(message));
                return true;
            } catch (IOException e) {
                logger.error("Failed to send WebSocket message to user {}: {}", username, e.getMessage());
                // Если ошибка отправки, возможно, сессия уже невалидна, удаляем
                sessions.remove(username);
                return false;
            }
        } else {
            if (userSession == null) {
                logger.warn("WebSocket session not found for user: {}. Message not sent.", username);
            } else { // userSession != null but not open
                logger.warn("WebSocket session for user: {} is not open. Removing session. Message not sent.", username);
                sessions.remove(username); // Удаляем закрытую сессию
            }
            return false;
        }
    }

    private String getUsernameFromSession(WebSocketSession session) {
        // Spring Security Principal должен быть доступен в атрибутах сессии,
        // если WebSocket хендшейк прошел через Spring Security (обычно так и есть)
        if (session.getPrincipal() != null) {
            return session.getPrincipal().getName();
        }
        return null; // Или извлекать из handshake-атрибутов, если необходимо
    }
}