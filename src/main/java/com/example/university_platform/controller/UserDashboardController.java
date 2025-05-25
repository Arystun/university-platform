package com.example.university_platform.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping; // Можно добавить, если все пути в этом контроллере будут начинаться с /user

import java.security.Principal;

@Controller
@RequestMapping("/user") // Базовый путь для всех методов этого контроллера
public class UserDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(UserDashboardController.class);

    @GetMapping("/dashboard") // Полный путь будет /user/dashboard
    public String userDashboard(Model model, Principal principal) {
        if (principal == null) {
            // Spring Security должен предотвратить это, но на всякий случай
            return "redirect:/login";
        }
        String currentUsername = principal.getName();
        model.addAttribute("pageTitle", "My Dashboard - " + currentUsername);
        logger.info("User {} accessing user dashboard.", currentUsername);
        // Здесь можно добавить загрузку какой-либо специфичной информации для дашборда,
        // например, количество непрочитанных сообщений, последние новости и т.д., если бы они были.
        // Для MVP просто отображаем страницу.
        return "user_dashboard"; // user_dashboard.html
    }

    // Сюда в будущем можно добавить другие методы, относящиеся к пользовательскому дашборду или профилю,
    // например, @GetMapping("/profile"), @PostMapping("/profile/update") и т.д.
}