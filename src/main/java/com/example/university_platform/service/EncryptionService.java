package com.example.university_platform.service;


import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator; // Не используется напрямую здесь, но полезно знать для генерации ключей
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException; // Для KeyGenerator, если бы мы его здесь использовали
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays; // Для операций с массивами, если потребуется

@Service
public class EncryptionService {

    // Алгоритм шифрования: AES, режим CBC (Cipher Block Chaining), дополнение PKCS5Padding
    // CBC требует вектор инициализации (IV)
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH_BYTES = 16; // Для AES, размер IV обычно 16 байт (128 бит)

    /**
     * Генерирует случайный вектор инициализации (IV).
     * IV должен быть уникальным для каждой операции шифрования с одним и тем же ключом.
     * @return IvParameterSpec объект, содержащий сгенерированный IV.
     */
    private IvParameterSpec generateIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv); // Заполняем массив случайными байтами
        return new IvParameterSpec(iv);
    }

    /**
     * Шифрует предоставленные данные с использованием указанного ключа SecretKey.
     * IV генерируется случайным образом и добавляется в начало зашифрованных данных.
     * @param data Данные для шифрования (строка).
     * @param key Секретный ключ AES.
     * @return Строка в Base64, содержащая IV + зашифрованные данные.
     * @throws Exception Если происходит ошибка во время шифрования.
     */
    public String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivParameterSpec = generateIv(); // Генерируем новый IV для этой операции

        // Инициализируем шифр в режиме шифрования с ключом и IV
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);

        // Шифруем данные
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Получаем байты IV, чтобы добавить их к результату
        byte[] ivBytes = ivParameterSpec.getIV();

        // Создаем массив для хранения IV и зашифрованных данных вместе
        byte[] combinedPayload = new byte[ivBytes.length + encryptedBytes.length];

        // Копируем IV в начало combinedPayload
        System.arraycopy(ivBytes, 0, combinedPayload, 0, ivBytes.length);
        // Копируем зашифрованные данные после IV
        System.arraycopy(encryptedBytes, 0, combinedPayload, ivBytes.length, encryptedBytes.length);

        // Кодируем combinedPayload (IV + шифртекст) в Base64 для удобной передачи/хранения
        return Base64.getEncoder().encodeToString(combinedPayload);
    }

    /**
     * Дешифрует данные, зашифрованные методом encrypt.
     * Ожидается, что входная строка encryptedDataWithIv содержит IV в начале, за которым следуют зашифрованные данные.
     * @param encryptedDataWithIv Строка в Base64 (IV + шифртекст).
     * @param key Секретный ключ AES, который использовался для шифрования.
     * @return Исходная расшифрованная строка.
     * @throws Exception Если происходит ошибка во время дешифрования.
     */
    public String decrypt(String encryptedDataWithIv, SecretKey key) throws Exception {
        // Декодируем Base64 строку обратно в байты
        byte[] combinedPayload = Base64.getDecoder().decode(encryptedDataWithIv);

        // Проверяем, достаточна ли длина для IV (хотя бы)
        if (combinedPayload.length < IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted data is too short to contain IV.");
        }

        // Извлекаем IV из начала combinedPayload
        byte[] ivBytes = new byte[IV_LENGTH_BYTES];
        System.arraycopy(combinedPayload, 0, ivBytes, 0, IV_LENGTH_BYTES);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        // Извлекаем фактические зашифрованные данные (все, что после IV)
        int encryptedBytesLength = combinedPayload.length - IV_LENGTH_BYTES;
        byte[] encryptedBytes = new byte[encryptedBytesLength];
        System.arraycopy(combinedPayload, IV_LENGTH_BYTES, encryptedBytes, 0, encryptedBytesLength);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        // Инициализируем шифр в режиме дешифрования с ключом и извлеченным IV
        cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);

        // Дешифруем данные
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // Преобразуем дешифрованные байты обратно в строку
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}