package com.example.university_platform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Для автоматического заполнения времени создания

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // Одно сообщение от одного отправителя
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne // Одно сообщение одному получателю (для MVP, можно расширить до групповых чатов)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserEntity receiver;

    @Lob // Для хранения потенциально длинного зашифрованного текста
    @Column(nullable = false, columnDefinition = "TEXT") // TEXT для большинства БД, или CLOB
    private String encryptedContent;

    @CreationTimestamp // Автоматически устанавливает время создания записи
    @Column(updatable = false)
    private LocalDateTime timestamp;

    // Конструктор для удобства
    public MessageEntity(UserEntity sender, UserEntity receiver, String encryptedContent) {
        this.sender = sender;
        this.receiver = receiver;
        this.encryptedContent = encryptedContent;
    }
}
