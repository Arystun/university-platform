package com.example.university_platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MessageDto {
    private Long id;
    private String senderUsername;
    private String receiverUsername;
    private String content; // В DTO будет расшифрованный контент для отображения
    private LocalDateTime timestamp;
    private boolean sentByCurrentUser; // Полезно для UI, чтобы различать свои и чужие сообщения

    public MessageDto(Long id, String senderUsername, String receiverUsername, String content, LocalDateTime timestamp) {
        this.id = id;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.content = content;
        this.timestamp = timestamp;
    }
}