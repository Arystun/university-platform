package com.example.university_platform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String roles;
    private boolean hasEncryptionKey; // Новое поле вместо прямого доступа к byte[]

    // Можно добавить конструктор, если нужно
    public UserDto(Long id, String username, String roles, boolean hasEncryptionKey) {
        this.id = id;
        this.username = username;
        this.roles = roles;
        this.hasEncryptionKey = hasEncryptionKey;
    }
}
