package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

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

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

//    @PostMapping("/register")
//    public String registerUser(@Valid @ModelAttribute("user") User user,
//                               BindingResult br,
//                               Model model,
//                               RedirectAttributes ra) {
//        if (br.hasErrors()) {
//            return "register";
//        }
//        try {
//            // Ensure here (or in userService.saveUser) that you hash the password:
//            // user.setPassword(passwordEncoder.encode(user.getPassword()));
//            userService.saveUser(user);
//            ra.addFlashAttribute("msg", "Account created! Please log in.");
//            return "redirect:/login";
//        } catch (EntityDuplicateException e) {
//            // attach a field error on email and re-render
//            br.rejectValue("email", "duplicate", e.getMessage());
//            return "register";
//        }
//    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult br,
                               Model model,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "register";
        }

        // Server-side defaults (since the form no longer has these fields)
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ACTIVE");                 // <-- default
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.STUDENT);           // <-- recommended default
        }

        try {
            userService.saveUser(user);
            ra.addFlashAttribute("msg", "Account created! Please log in.");
            return "redirect:/login";
        } catch (EntityDuplicateException e) {
            br.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        }
    }




}




