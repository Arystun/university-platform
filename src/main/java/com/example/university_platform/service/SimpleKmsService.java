package com.example.university_platform.service;

import com.example.university_platform.entity.UserEntity;
import com.example.university_platform.repository.UserRepository;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import jakarta.annotation.PostConstruct; // Для создания тестовых пользователей
import org.springframework.security.crypto.password.PasswordEncoder;


@Service
public class SimpleKmsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Для создания тестовых пользователей


    public SimpleKmsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Только для MVP: генерация ключа и сохранение его прямо в сущности пользователя (в Base64)
    // В реальной системе это должно быть гораздо сложнее и безопаснее (Vault, HSM и т.д.)
    public SecretKey getOrCreateUserKey(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (user.getEncryptionKeyMaterial() == null) {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, new SecureRandom()); // AES-256
                SecretKey secretKey = keyGen.generateKey();
                user.setEncryptionKeyMaterial(secretKey.getEncoded());
                userRepository.save(user);
                return secretKey;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("AES algorithm not found", e);
            }
        } else {
            return new javax.crypto.spec.SecretKeySpec(user.getEncryptionKeyMaterial(), "AES");
        }
    }

    // Для инициализации тестовых пользователей с ключами
    @PostConstruct
    public void initKeysForInMemoryUsers() {
        userRepository.findByUsername("user").ifPresent(this::generateKeyIfNotExists);
        userRepository.findByUsername("admin").ifPresent(this::generateKeyIfNotExists);
    }

    private void generateKeyIfNotExists(UserEntity user) {
        if (user.getEncryptionKeyMaterial() == null) {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                SecretKey secretKey = keyGen.generateKey();
                user.setEncryptionKeyMaterial(secretKey.getEncoded());
                userRepository.save(user);
                System.out.println("Generated AES key for user: " + user.getUsername());
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Failed to generate key for " + user.getUsername() + ": " + e.getMessage());
            }
        }
    }


    // Создадим тестовых пользователей в БД при старте, чтобы их можно было использовать
    // Это лучше делать через data.sql или кастомный UserDetailsService, который работает с БД
    // Но для быстрого старта MVP и синхронизации с InMemoryUserDetailsManager:
    @PostConstruct
    public void initDatabaseUsers() {
        if (userRepository.findByUsername("user").isEmpty()) {
            UserEntity user = new UserEntity("user", passwordEncoder.encode("password"), "ROLE_USER");
            userRepository.save(user);
            generateKeyIfNotExists(user); // Генерируем ключ для нового пользователя
        }
        if (userRepository.findByUsername("admin").isEmpty()) {
            UserEntity admin = new UserEntity("admin", passwordEncoder.encode("adminpassword"), "ROLE_ADMIN,ROLE_USER");
            userRepository.save(admin);
            generateKeyIfNotExists(admin); // Генерируем ключ для нового админа
        }
    }
}