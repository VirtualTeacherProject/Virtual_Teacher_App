package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginMvcController {

    @GetMapping("/login")
    public String loginPage() { return "login"; }



}
