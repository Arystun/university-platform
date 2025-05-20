package com.example.university_platform.service;

import com.example.university_platform.handler.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.entity.MessageEntity;
import com.example.university_platform.entity.UserEntity;
import com.example.university_platform.repository.MessageRepository;
import com.example.university_platform.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;



import javax.crypto.SecretKey;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final SimpleKmsService kmsService;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Autowired
    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          EncryptionService encryptionService, SimpleKmsService kmsService,
                          NotificationWebSocketHandler notificationWebSocketHandler) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.kmsService = kmsService;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @Transactional
    public MessageDto sendMessage(String senderUsername, String receiverUsername, String plainTextContent) {
        UserEntity sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Sender not found: " + senderUsername));
        UserEntity receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Receiver not found: " + receiverUsername));

        SecretKey senderKey = kmsService.getOrCreateUserKey(senderUsername);

        try {
            String encryptedContent = encryptionService.encrypt(plainTextContent, senderKey);
            MessageEntity messageEntity = new MessageEntity(sender, receiver, encryptedContent);
            messageEntity = messageRepository.save(messageEntity);

            // Отправляем уведомление получателю через WebSocket
            String notificationMessage = "You have a new message from " + senderUsername;
            // Уведомляем получателя
            notificationWebSocketHandler.sendMessageToUser(receiverUsername, notificationMessage);
            // Можно также уведомить и отправителя (например, для синхронизации в его UI, если он открыт на нескольких вкладках)
            // notificationWebSocketHandler.sendMessageToUser(senderUsername, "Your message to " + receiverUsername + " has been sent.");


            return convertToDto(messageEntity, senderUsername, true);
        } catch (Exception e) {
            throw new RuntimeException("Could not send message", e);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessagesForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Получаем все сообщения, где пользователь отправитель или получатель
        List<MessageEntity> messages = messageRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);

        return messages.stream()
                .map(msg -> convertToDto(msg, username, false)) // false, так как дешифруем для чтения
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getAllMessagesForAdmin() {
        List<MessageEntity> messages = messageRepository.findAllByOrderByTimestampDesc(); // Метод нужно добавить в репозиторий
        return messages.stream()
                .map(msg -> convertToDto(msg, "admin", true)) // Админ может видеть все
                .collect(Collectors.toList());
    }


    private MessageDto convertToDto(MessageEntity messageEntity, String currentUsername, boolean decryptForCurrentUserOnly) {
        String decryptedContent = "[Encrypted]"; // По умолчанию
        boolean sentByCurrentUser = messageEntity.getSender().getUsername().equals(currentUsername);

        // Определяем, чей ключ использовать для дешифрования
        // Если текущий пользователь - отправитель, используем его ключ.
        // Если текущий пользователь - получатель, по-хорошему, сообщение должно быть зашифровано его ключом
        // или общим ключом. В нашей MVP схеме, оно зашифровано ключом отправителя.
        // Значит, дешифровать может только отправитель или получатель, знающий ключ отправителя (что небезопасно в общем случае).
        // Для MVP: дешифруем, если текущий пользователь отправитель ИЛИ получатель, используя ключ отправителя.
        // Либо, если decryptForCurrentUserOnly=true, то только если он отправитель.
        // Либо, если currentUsername == "admin" (для админской панели, если у админа есть доступ к ключам, что для MVP упростим)

        UserEntity keyOwner = messageEntity.getSender(); // Ключом отправителя шифровали

        if (currentUsername.equals("admin") || // Админ видит все (упрощение для MVP)
                currentUsername.equals(messageEntity.getSender().getUsername()) ||
                currentUsername.equals(messageEntity.getReceiver().getUsername())) {
            try {
                SecretKey userKey = kmsService.getOrCreateUserKey(keyOwner.getUsername());
                decryptedContent = encryptionService.decrypt(messageEntity.getEncryptedContent(), userKey);
            } catch (Exception e) {
                // Логируем ошибку, но не прерываем процесс, просто показываем, что не удалось расшифровать
                System.err.println("Could not decrypt message " + messageEntity.getId() + ": " + e.getMessage());
                decryptedContent = "[Decryption Failed]";
            }
        }

        return new MessageDto(
                messageEntity.getId(),
                messageEntity.getSender().getUsername(),
                messageEntity.getReceiver().getUsername(),
                decryptedContent,
                messageEntity.getTimestamp()
        );
    }
}
