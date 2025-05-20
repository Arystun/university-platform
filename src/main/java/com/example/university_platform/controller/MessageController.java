package com.example.university_platform.controller;


import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public ResponseEntity<MessageDto> sendMessage(@RequestBody SendMessageRequest request, Principal principal) {
        // Principal principal - это текущий аутентифицированный пользователь
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String senderUsername = principal.getName();
        try {
            MessageDto sentMessage = messageService.sendMessage(senderUsername, request.getReceiverUsername(), request.getContent());
            return ResponseEntity.ok(sentMessage);
        } catch (RuntimeException e) {
            // Лучше обрабатывать конкретные исключения
            return ResponseEntity.badRequest().body(null); // Тело ответа можно сделать информативнее
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<MessageDto>> getMyMessages(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = principal.getName();
        List<MessageDto> messages = messageService.getMessagesForUser(username);
        return ResponseEntity.ok(messages);
    }

    // Внутренний класс для запроса на отправку сообщения
    static class SendMessageRequest {
        private String receiverUsername;
        private String content;

        // Getters and Setters
        public String getReceiverUsername() { return receiverUsername; }
        public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
