package com.example.university_platform;

import com.example.university_platform.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension; // Можно добавить, если планируете моки, но для этого теста не обязательно

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; // Для генерации ключа, если нужно

import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class) // Не строго обязательно, если нет моков @Mock или @InjectMocks
class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        encryptionService = new EncryptionService(); // Создаем реальный экземпляр сервиса

        // Генерируем тестовый AES ключ
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom()); // AES-256
        secretKey = keyGen.generateKey();
    }

    @Test
    void encryptDecrypt_shouldReturnOriginalData_whenDataIsValid() throws Exception {
        // Arrange
        String originalData = "This is a super secret message!";

        // Act
        String encryptedData = encryptionService.encrypt(originalData, secretKey);
        // Assertions for encryption
        assertNotNull(encryptedData, "Encrypted data should not be null");
        assertNotEquals(originalData, encryptedData, "Encrypted data should not be the same as original data");

        // Act
        String decryptedData = encryptionService.decrypt(encryptedData, secretKey);
        // Assertions for decryption
        assertEquals(originalData, decryptedData, "Decrypted data should match the original data");
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertext_forSameDataAndKey_dueToRandomIV() throws Exception {
        // Arrange
        String originalData = "Consistent data for IV test";

        // Act
        String encryptedData1 = encryptionService.encrypt(originalData, secretKey);
        String encryptedData2 = encryptionService.encrypt(originalData, secretKey);

        // Assert
        assertNotNull(encryptedData1, "First encrypted data should not be null");
        assertNotNull(encryptedData2, "Second encrypted data should not be null");
        // Из-за случайного IV для CBC режима, шифртексты должны быть разными
        assertNotEquals(encryptedData1, encryptedData2, "AES/CBC with random IV should produce different ciphertexts for the same plaintext and key");
    }

    @Test
    void decrypt_withInvalidEncryptedDataFormat_shouldThrowException() {
        // Arrange
        String invalidEncryptedData = "thisIsNotBase64OrValidFormat"; // Невалидные данные

        // Act & Assert
        // Ожидаем исключение, так как данные не могут быть корректно декодированы/дешифрованы
        // Это может быть IllegalArgumentException из Base64.getDecoder().decode()
        // или какое-то специфичное криптографическое исключение (AEADBadTagException, BadPaddingException и т.д.)
        // Точный тип исключения зависит от того, на каком этапе произойдет сбой.
        // Для простоты можно ловить общее Exception или более специфичное, если известно.
        assertThrows(Exception.class, () -> {
            encryptionService.decrypt(invalidEncryptedData, secretKey);
        }, "Decrypting invalid data format should throw an exception");
    }

    @Test
    void decrypt_withTamperedIv_shouldIdeallyFailOrProduceGarbage() {
        // Arrange
        String originalData = "Data to test IV tampering";
        // Этот тест сложнее написать корректно без глубокого понимания структуры шифртекста,
        // так как IV является частью Base64 строки. Проще проверить общее поведение.
        // Для MVP можно пропустить или упростить.
        // Основная идея: если IV поврежден, дешифрование не должно дать исходный текст.

        assertDoesNotThrow(() -> { // Просто проверяем, что не падает с NullPointerException и т.п.
            String encrypted = encryptionService.encrypt(originalData, secretKey);
            // Попытка "испортить" IV (первая часть Base64 строки) - очень грубый способ
            char[] chars = encrypted.toCharArray();
            if (chars.length > 0) {
                chars[0] = (chars[0] == 'A' ? 'B' : 'A'); // Меняем первый символ Base64 строки
            }
            String tamperedEncryptedData = new String(chars);

            if (!tamperedEncryptedData.equals(encrypted)) { // Убедимся, что данные действительно изменены
                String decryptedWithTamperedIv = encryptionService.decrypt(tamperedEncryptedData, secretKey);
                assertNotEquals(originalData, decryptedWithTamperedIv, "Decryption with tampered IV should not yield original data");
            } else {
                // Если шифртекст слишком короткий и не изменился, тест не имеет смысла
                System.out.println("Skipping IV tampering test due to short/unchanged ciphertext");
            }
        }, "Tampering IV test caused an unexpected exception during execution.");
    }
}