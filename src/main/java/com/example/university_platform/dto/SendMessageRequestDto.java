package com.example.university_platform.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@Schema(description = "Request object for sending a new message")
public class SendMessageRequestDto {

    @Schema(description = "Username of the message receiver", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    // @NotBlank(message = "Receiver username cannot be blank") // Пример валидации
    private String receiverUsername;

    @Schema(description = "Content of the message", requiredMode = Schema.RequiredMode.REQUIRED, example = "Hello there!")
    // @NotBlank(message = "Message content cannot be blank") // Пример валидации
    // @Size(max = 1000, message = "Message content cannot exceed 1000 characters") // Пример валидации
    private String content;

    // Конструктор, если нужен для тестов или других целей (Lombok @Data уже создает)
    public SendMessageRequestDto(String receiverUsername, String content) {
        this.receiverUsername = receiverUsername;
        this.content = content;
    }
}