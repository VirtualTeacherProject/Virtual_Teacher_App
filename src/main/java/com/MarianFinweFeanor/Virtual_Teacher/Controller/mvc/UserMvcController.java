package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import ch.qos.logback.classic.encoder.JsonEncoder;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityDuplicateException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserMvcController {

    private final UserService userService;

    public UserMvcController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("user") //factory method
    public User prepareUser() {
        User u = new User();
        u.setStatus("ACTIVE");
        u.setRole(UserRole.STUDENT);
        return u;
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register"; // model already has "user" via @ModelAttribute
    }


    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "register";
        }
        try {
            userService.saveUser(user);
            ra.addFlashAttribute("msg", "Account created! Please log in.");
            return "redirect:/login";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            br.rejectValue("email", "duplicate", "Email is already registered.");
            return "register";
        }
    }

}




