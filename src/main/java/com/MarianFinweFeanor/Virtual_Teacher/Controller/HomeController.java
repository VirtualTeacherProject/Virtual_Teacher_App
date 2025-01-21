package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index"; // This points to src/main/resources/templates/index.html
    }
}
