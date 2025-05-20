package com.example.university_platform.config;


import com.example.university_platform.handler.NotificationWebSocketHandler; // Мы создадим этот класс дальше
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // Включаем поддержку WebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Autowired
    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Регистрируем наш хендлер для эндпоинта /ws/notifications
        // withAllowedOrigins("*") - разрешает подключения с любых доменов (для разработки).
        // В продакшене лучше указать конкретные домены.
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications").setAllowedOrigins("*");
    }
}