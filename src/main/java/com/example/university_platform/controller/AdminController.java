package com.example.university_platform.controller;


import com.example.university_platform.dto.MessageDto;
import com.example.university_platform.entity.UserEntity;
import com.example.university_platform.repository.UserRepository;
import com.example.university_platform.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final MessageService messageService;

    @Autowired
    public AdminController(UserRepository userRepository, MessageService messageService) {
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<UserEntity> users = userRepository.findAll();
        List<MessageDto> messages = messageService.getAllMessagesForAdmin(); // Метод для получения всех сообщений

        model.addAttribute("users", users);
        model.addAttribute("messages", messages);
        model.addAttribute("pageTitle", "Admin Dashboard");
        return "admin_dashboard"; // admin_dashboard.html
    }
}
