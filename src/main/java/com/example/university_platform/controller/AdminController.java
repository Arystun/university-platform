package com.example.university_platform.controller;


import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.dto.UserDto;
import com.example.university_platform.service.MessageService;
import com.example.university_platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.slf4j.Logger; // Импортируем SLF4J Logger
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final MessageService messageService;

    @Autowired
    public AdminController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        logger.info("AdminController: adminDashboard method CALLED!");

        List<UserDto> users = userService.getAllUsersForAdmin();
        List<MessageDto> messages = messageService.getAllMessagesForAdmin(); // Метод для получения всех сообщений

        logger.info("Loaded {} users and {} messages for admin dashboard.", users.size(), messages.size());


        model.addAttribute("users", users);
        model.addAttribute("messages", messages);
        model.addAttribute("pageTitle", "Admin Dashboard");

        logger.info("Returning view name: admin_dashboard");
        return "admin_dashboard"; // admin_dashboard.html
    }
}
