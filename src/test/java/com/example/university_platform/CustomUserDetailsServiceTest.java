package com.example.university_platform;


import com.example.university_platform.entity.UserEntity;
import com.example.university_platform.repository.UserRepository;
import com.example.university_platform.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private UserEntity testUser;
    private String موجودUsername = "testuser";
    private String موجودPassword = "hashedPassword"; // В UserEntity хранится хешированный пароль
    private String موجودRoles = "ROLE_USER,ROLE_ADMIN";

    @BeforeEach
    void setUp() {
        testUser = new UserEntity(موجودUsername, موجودPassword, موجودRoles);
    }

    @Test
    void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByUsername(موجودUsername)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(موجودUsername);

        // Assert
        assertNotNull(userDetails);
        assertEquals(موجودUsername, userDetails.getUsername());
        assertEquals(موجودPassword, userDetails.getPassword()); // UserDetails хранит тот же пароль, что и в UserEntity
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN")));
        assertEquals(2, userDetails.getAuthorities().size());

        verify(userRepository, times(1)).findByUsername(موجودUsername);
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExist_shouldThrowUsernameNotFoundException() {
        // Arrange
        String nonexistentUsername = "unknownuser";
        when(userRepository.findByUsername(nonexistentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(nonexistentUsername);
        });

        verify(userRepository, times(1)).findByUsername(nonexistentUsername);
    }

    @Test
    void loadUserByUsername_whenRolesAreNull_shouldAssignDefaultRole() {
        // Arrange
        testUser.setRoles(null); // Устанавливаем роли в null
        when(userRepository.findByUsername(موجودUsername)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(موجودUsername);

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_USER")));
        assertEquals(1, userDetails.getAuthorities().size());
    }

    @Test
    void loadUserByUsername_whenRolesAreEmptyString_shouldAssignDefaultRole() {
        // Arrange
        testUser.setRoles(" "); // Устанавливаем роли в пустую строку с пробелом
        when(userRepository.findByUsername(موجودUsername)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(موجودUsername);

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_USER")));
        assertEquals(1, userDetails.getAuthorities().size());
    }
}