package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // maps to login.html or login.jsp in templates
    }

    @GetMapping("/home")
    public String homePage() {
        return "home"; // // post-login landing page
    }

}
