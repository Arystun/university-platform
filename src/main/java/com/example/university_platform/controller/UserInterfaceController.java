package com.example.university_platform.controller;

import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.dto.UserDto; // Используем UserDto
import com.example.university_platform.service.MessageService;
import com.example.university_platform.service.UserService; // Используем UserService
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/messages") // Базовый путь для этого контроллера
public class UserInterfaceController {

    private static final Logger logger = LoggerFactory.getLogger(UserInterfaceController.class);

    private final MessageService messageService;
    private final UserService userService; // Нужен для списка контактов

    @Autowired
    public UserInterfaceController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    // Метод для отображения основной страницы сообщений и чата с конкретным пользователем
    @GetMapping(value = {"", "/with/{contactUsername}"})
    public String messagesPage(@PathVariable(required = false) String contactUsername, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        String currentUsername = principal.getName();
        logger.info("User {} accessing messages page, selected contact: {}", currentUsername, contactUsername);

        // Получаем список всех пользователей, кроме текущего, для списка контактов
        List<UserDto> contacts = userService.getAllUsersForAdmin().stream()
                .filter(userDto -> !userDto.getUsername().equals(currentUsername))
                .collect(Collectors.toList());
        model.addAttribute("contacts", contacts);
        model.addAttribute("selectedContactUsername", contactUsername);
        model.addAttribute("pageTitle", "My Messages");

        if (contactUsername != null) {
            // Загружаем историю сообщений с выбранным контактом
            List<MessageDto> chatMessages = messageService.getMessagesForUser(currentUsername).stream()
                    .filter(msg -> msg.getSenderUsername().equals(contactUsername) || msg.getReceiverUsername().equals(contactUsername))
                    .sorted(Comparator.comparing(MessageDto::getTimestamp)) // Сортируем по времени для отображения чата
                    .collect(Collectors.toList());
            model.addAttribute("chatMessages", chatMessages);
            logger.info("Loaded {} messages for chat between {} and {}", chatMessages.size(), currentUsername, contactUsername);
        }

        return "user_messages"; // user_messages.html
    }


    // Метод для обработки отправки сообщения из формы
    @PostMapping("/send")
    public String sendMessageFromForm(@RequestParam String receiverUsername,
                                      @RequestParam String content,
                                      Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        String senderUsername = principal.getName();
        logger.info("User {} sending message to {} from UI form.", senderUsername, receiverUsername);

        try {
            messageService.sendMessage(senderUsername, receiverUsername, content);
            // После отправки перенаправляем обратно на страницу чата с этим пользователем
            return "redirect:/messages/with/" + receiverUsername;
        } catch (Exception e) {
            logger.error("Error sending message from UI form by user {}: {}", senderUsername, e.getMessage(), e);
            // Добавляем сообщение об ошибке в модель и возвращаем на ту же страницу
            // (в идеале нужно также передать список контактов и текущего выбранного контакта)
            model.addAttribute("errorMessage", "Failed to send message: " + e.getMessage());
            // Нужно снова загрузить контакты для отображения страницы
            List<UserDto> contacts = userService.getAllUsersForAdmin().stream()
                    .filter(userDto -> !userDto.getUsername().equals(senderUsername))
                    .collect(Collectors.toList());
            model.addAttribute("contacts", contacts);
            model.addAttribute("selectedContactUsername", receiverUsername); // Остаемся на текущем контакте
            // Можно также загрузить предыдущие сообщения, если нужно их снова показать
            List<MessageDto> chatMessages = messageService.getMessagesForUser(senderUsername).stream()
                    .filter(msg -> msg.getSenderUsername().equals(receiverUsername) || msg.getReceiverUsername().equals(receiverUsername))
                    .sorted(Comparator.comparing(MessageDto::getTimestamp))
                    .collect(Collectors.toList());
            model.addAttribute("chatMessages", chatMessages);

            return "user_messages";
        }
    }
}
