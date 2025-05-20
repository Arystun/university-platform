package com.example.university_platform;


import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.entity.MessageEntity;
import com.example.university_platform.entity.UserEntity;
import com.example.university_platform.handler.NotificationWebSocketHandler;
import com.example.university_platform.repository.MessageRepository;
import com.example.university_platform.repository.UserRepository;
import com.example.university_platform.service.EncryptionService;
import com.example.university_platform.service.MessageService;
import com.example.university_platform.service.SimpleKmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private SimpleKmsService kmsService;
    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @InjectMocks
    private MessageService messageService;

    private UserEntity sender;
    private UserEntity receiver;
    private SecretKey senderKey;
    private String plainTextContent = "Hello World!";
    private String encryptedContent = "EncryptedHelloWorld";

    @BeforeEach
    void setUp() {
        sender = new UserEntity("sender", "pass", "ROLE_USER");
        sender.setId(1L);
        receiver = new UserEntity("receiver", "pass", "ROLE_USER");
        receiver.setId(2L);

        // Генерируем фейковый ключ для тестов
        byte[] keyBytes = new byte[32]; // 256-bit key
        senderKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Test
    void sendMessage_shouldEncryptAndSaveMessageAndNotifyReceiver() throws Exception {
        // Arrange
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(kmsService.getOrCreateUserKey("sender")).thenReturn(senderKey);
        when(encryptionService.encrypt(plainTextContent, senderKey)).thenReturn(encryptedContent);

        // Мокаем сохранение сообщения
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
            MessageEntity msg = invocation.getArgument(0);
            msg.setId(100L); // Присваиваем ID, как будто оно сохранилось в БД
            msg.setTimestamp(LocalDateTime.now()); // Устанавливаем timestamp
            return msg;
        });
        // Мокаем дешифрование для DTO (потому что convertToDto его вызывает)
        when(encryptionService.decrypt(encryptedContent, senderKey)).thenReturn(plainTextContent);


        // Act
        MessageDto resultDto = messageService.sendMessage("sender", "receiver", plainTextContent);

        // Assert
        assertNotNull(resultDto);
        assertEquals("sender", resultDto.getSenderUsername());
        assertEquals("receiver", resultDto.getReceiverUsername());
        assertEquals(plainTextContent, resultDto.getContent()); // В DTO должно быть расшифровано

        verify(userRepository, times(1)).findByUsername("sender");
        verify(userRepository, times(1)).findByUsername("receiver");
        verify(kmsService, times(2)).getOrCreateUserKey("sender");
        verify(encryptionService, times(1)).encrypt(plainTextContent, senderKey);
        verify(messageRepository, times(1)).save(any(MessageEntity.class));
        verify(notificationWebSocketHandler, times(1))
                .sendMessageToUser("receiver", "You have a new message from sender");
        verify(encryptionService, times(1)).decrypt(encryptedContent, senderKey); // Проверяем вызов decrypt для DTO
    }

    @Test
    void sendMessage_whenEncryptionFails_shouldThrowRuntimeException() throws Exception {
        // Arrange
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(kmsService.getOrCreateUserKey("sender")).thenReturn(senderKey);
        when(encryptionService.encrypt(plainTextContent, senderKey)).thenThrow(new Exception("Encryption failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageService.sendMessage("sender", "receiver", plainTextContent);
        });
        assertTrue(exception.getMessage().contains("Could not send message"));

        verify(messageRepository, never()).save(any(MessageEntity.class));
        verify(notificationWebSocketHandler, never()).sendMessageToUser(anyString(), anyString());
    }


    @Test
    void getMessagesForUser_shouldReturnDecryptedMessages() throws Exception {
        // Arrange
        MessageEntity msg1 = new MessageEntity(sender, receiver, encryptedContent);
        msg1.setId(1L); msg1.setTimestamp(LocalDateTime.now().minusHours(1));
        MessageEntity msg2 = new MessageEntity(receiver, sender, encryptedContent + "2"); // Другой зашифрованный контент
        msg2.setId(2L); msg2.setTimestamp(LocalDateTime.now());

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        // Предположим, что ключ отправителя используется для дешифровки в convertToDto
        when(kmsService.getOrCreateUserKey("sender")).thenReturn(senderKey);
        when(kmsService.getOrCreateUserKey("receiver")).thenReturn(senderKey); // Для простоты теста, тот же ключ

        when(encryptionService.decrypt(encryptedContent, senderKey)).thenReturn(plainTextContent);
        when(encryptionService.decrypt(encryptedContent + "2", senderKey)).thenReturn(plainTextContent + "2");

        when(messageRepository.findBySenderOrReceiverOrderByTimestampDesc(sender, sender))
                .thenReturn(Arrays.asList(msg2, msg1)); // Сортировка по убыванию timestamp

        // Act
        List<MessageDto> resultDtos = messageService.getMessagesForUser("sender");

        // Assert
        assertNotNull(resultDtos);
        assertEquals(2, resultDtos.size());

        assertEquals("receiver", resultDtos.get(0).getSenderUsername()); // msg2
        assertEquals(plainTextContent + "2", resultDtos.get(0).getContent());

        assertEquals("sender", resultDtos.get(1).getSenderUsername()); // msg1
        assertEquals(plainTextContent, resultDtos.get(1).getContent());

        verify(encryptionService, times(1)).decrypt(encryptedContent, senderKey);
        verify(encryptionService, times(1)).decrypt(encryptedContent + "2", senderKey);
    }

    // TODO: Добавить тесты для getAllMessagesForAdmin
    // TODO: Добавить тесты для случаев, когда пользователь не найден в sendMessage и getMessagesForUser
    // TODO: Добавить тесты для случаев, когда дешифрование в convertToDto не удается
}