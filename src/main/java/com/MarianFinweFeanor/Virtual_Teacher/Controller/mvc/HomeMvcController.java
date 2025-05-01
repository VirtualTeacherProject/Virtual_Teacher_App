package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeMvcController {

    @GetMapping("/home")
    public String home() { return "home"; }
}
