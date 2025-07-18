package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

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
    public String updateProfile(@ModelAttribute("user") User formUser,
                                Principal principal,
                                RedirectAttributes attrs) {
        // 1) Fetch the real user from the DB
        User existing = userService.findByEmail(principal.getName());

        // 2) Copy only the updatable fields
        existing.setFirstName(formUser.getFirstName());
        existing.setLastName(formUser.getLastName());
        existing.setProfilePicture(formUser.getProfilePicture());
        existing.setStatus(formUser.getStatus());

        // 3) Save via a dedicated update method
        userService.updateUser(existing);

        // 4) Flash a success message and stay on /profile
        attrs.addFlashAttribute("successMessage", "Profile updated!");
        return "redirect:/profile";
    }

}

