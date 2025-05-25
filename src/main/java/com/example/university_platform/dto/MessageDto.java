package com.example.university_platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(description = "Data Transfer Object for a message")
public class MessageDto {
    @Schema(description = "Message ID", example = "1")
    private Long id;
    @Schema(description = "Username of the sender", example = "user")
    private String senderUsername;
    @Schema(description = "Username of the receiver", example = "admin")
    private String receiverUsername;
    @Schema(description = "Decrypted content of the message", example = "Hello Admin!")
    private String content; // В DTO будет расшифрованный контент для отображения
    @Schema(description = "Timestamp of when the message was sent")
    private LocalDateTime timestamp;
    @Schema(description = "Indicates if the message was sent by the currently authenticated user", example = "true")
    private boolean sentByCurrentUser; // Полезно для UI, чтобы различать свои и чужие сообщения

    public MessageDto(Long id, String senderUsername, String receiverUsername, String content, LocalDateTime timestamp) {
        this.id = id;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.content = content;
        this.timestamp = timestamp;
    }
}