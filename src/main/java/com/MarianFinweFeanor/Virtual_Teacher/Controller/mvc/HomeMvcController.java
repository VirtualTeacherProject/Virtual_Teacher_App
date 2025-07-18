package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeMvcController {

    /**
     * Show the “home” template for both the root (/) and /home.
     * Anonymous users will see the landing section;
     * authenticated users see the dashboard section.
     */
    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }
}
