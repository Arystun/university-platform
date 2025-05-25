package com.example.university_platform.controller;

import io.swagger.v3.oas.annotations.Operation; // Новый импорт
import io.swagger.v3.oas.annotations.media.Content; // Новый импорт
import io.swagger.v3.oas.annotations.media.Schema; // Новый импорт
import io.swagger.v3.oas.annotations.parameters.RequestBody; // Новый импорт для описания тела запроса
import io.swagger.v3.oas.annotations.responses.ApiResponse; // Новый импорт
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Новый импорт
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // Новый импорт
import io.swagger.v3.oas.annotations.tags.Tag; // Новый импорт


import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.dto.SendMessageRequestDto;
import com.example.university_platform.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/messages")
@Tag(name = "Message API", description = "API for sending and retrieving messages") // Тег для группировки API в Swagger UI
@SecurityRequirement(name = "JSESSIONID")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "Send a new message", description = "Sends a message from the authenticated user to a specified receiver.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDto.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad request (e.g., receiver not found, validation error)",
                    content = @Content(schema = @Schema(implementation = Map.class))), // Указываем Map для тела ошибки
            @ApiResponse(responseCode = "401", description = "Unauthorized (user not authenticated)",
                    content = @Content(schema = @Schema(implementation = Map.class))), // Указываем Map для тела ошибки
            @ApiResponse(responseCode = "404", description = "Receiver not found",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody( // Явное указание аннотации для тела запроса
                    description = "Request object for sending a message",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendMessageRequestDto.class))
            )
            @org.springframework.web.bind.annotation.RequestBody SendMessageRequestDto request,
            Principal principal) {// Principal principal - это текущий аутентифицированный пользователь

        logger.info("sendMessage called. Principal: {}", (principal != null ? principal.getName() : "null"));
        if (principal == null) {
            logger.warn("principal is null, returning UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        String senderUsername = principal.getName();
        try {
            MessageDto sentMessage = messageService.sendMessage(senderUsername, request.getReceiverUsername(), request.getContent());
            return ResponseEntity.ok(sentMessage);
        } catch (RuntimeException e) {
            logger.error("Error sending message for user {}: {}", senderUsername, e.getMessage(), e);
            // Лучше обрабатывать конкретные исключения
            if (e instanceof org.springframework.security.core.userdetails.UsernameNotFoundException || e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get my messages", description = "Retrieves all messages for the authenticated user (sent and received).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved messages",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = MessageDto.class)) }),
            @ApiResponse(responseCode = "401", description = "Unauthorized (user not authenticated)",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<?> getMyMessages(Principal principal) {
        logger.info("getMyMessages called. Principal: {}", (principal != null ? principal.getName() : "null"));
        if (principal == null) {
            logger.warn("Principal is null, returning UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        String username = principal.getName();
        try {
            List<MessageDto> messages = messageService.getMessagesForUser(username);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error retrieving messages for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve messages."));
        }
    }
}
