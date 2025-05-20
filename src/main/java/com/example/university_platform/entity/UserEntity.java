package com.example.university_platform.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Table(name = "app_users") // 'user' часто зарезервированное слово в SQL
@Data
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // В реальном приложении храните зашифрованный пароль

    private String roles; // Например "ROLE_USER,ROLE_ADMIN" - упрощенно

    // Для MVP ключ шифрования можно хранить здесь (зашифрованным другим мастер-ключом или просто в Base64 для демо)
    // ВАЖНО: Это ОЧЕНЬ небезопасно для продакшена! Только для MVP!
    @Lob // Large Object Binary Data
    @Column(name = "encryption_key_material", columnDefinition = "BLOB") // Или BYTEA для PostgreSQL
    private byte[] encryptionKeyMaterial;


    public UserEntity(String username, String password, String roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }
}