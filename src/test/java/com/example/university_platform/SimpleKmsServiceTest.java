package com.example.university_platform;


import com.example.university_platform.entity.UserEntity;
import com.example.university_platform.repository.UserRepository;
import com.example.university_platform.service.SimpleKmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder; // Нужен, так как он в конструкторе SimpleKmsService

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Инициализирует моки и инжектирует их
class SimpleKmsServiceTest {

    @Mock // Создаем мок для UserRepository
    private UserRepository userRepository;

    @Mock // Создаем мок для PasswordEncoder (хотя его методы тут не будут ключевыми)
    private PasswordEncoder passwordEncoder;

    @InjectMocks // Создаем экземпляр SimpleKmsService и инжектируем в него моки
    private SimpleKmsService kmsService;

    private UserEntity testUser;
    private String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        testUser = new UserEntity(testUsername, "password", "ROLE_USER");
        // kmsService = new SimpleKmsService(userRepository, passwordEncoder); // Не нужно при @InjectMocks
    }

    @Test
    void getOrCreateUserKey_whenKeyDoesNotExist_shouldCreateAndSaveKey() {
        // Arrange
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        // Имитируем, что save возвращает пользователя с обновленным ключом (для полноты)
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SecretKey generatedKey = kmsService.getOrCreateUserKey(testUsername);

        // Assert
        assertNotNull(generatedKey);
        assertEquals("AES", generatedKey.getAlgorithm());
        assertEquals(32, generatedKey.getEncoded().length); // 256 бит / 8 = 32 байта
        assertNotNull(testUser.getEncryptionKeyMaterial()); // Проверяем, что ключ сохранился в сущности
        assertArrayEquals(generatedKey.getEncoded(), testUser.getEncryptionKeyMaterial());

        verify(userRepository, times(1)).findByUsername(testUsername);
        verify(userRepository, times(1)).save(testUser); // Проверяем, что save был вызван
    }

    @Test
    void getOrCreateUserKey_whenKeyExists_shouldReturnExistingKey() {
        // Arrange
        byte[] existingKeyMaterial = new byte[32]; // 256 бит
        new SecureRandom().nextBytes(existingKeyMaterial);
        testUser.setEncryptionKeyMaterial(existingKeyMaterial);
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // Act
        SecretKey retrievedKey = kmsService.getOrCreateUserKey(testUsername);

        // Assert
        assertNotNull(retrievedKey);
        assertEquals("AES", retrievedKey.getAlgorithm());
        assertArrayEquals(existingKeyMaterial, retrievedKey.getEncoded());

        verify(userRepository, times(1)).findByUsername(testUsername);
        verify(userRepository, never()).save(any(UserEntity.class)); // Save не должен вызываться, если ключ уже есть
    }

    @Test
    void getOrCreateUserKey_whenUserNotFound_shouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            kmsService.getOrCreateUserKey("nonexistentuser");
        }, "User not found: nonexistentuser"); // Можно проверить и сообщение исключения

        verify(userRepository, times(1)).findByUsername("nonexistentuser");
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
