package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class ProfileMvcController {

    private final UserService userService;

    @Autowired
    public ProfileMvcController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        String email = principal.getName();  // Logged-in user's email
        User user = userService.findByEmail(email);  // You need this method in UserService
        model.addAttribute("user", user);
        return "profile"; // maps to profile
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User user) {
        userService.saveUser(user); // assumes saveUser() updates if ID exists
        return "redirect:/home";
    }
}

