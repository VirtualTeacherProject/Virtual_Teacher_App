package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/assignments")
public class StudentAssignmentMvcController {

    private final AssignmentService assignmentService;

    public StudentAssignmentMvcController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping({"/my", "/my/"})
    public String mySubmissions(Model model, Principal principal) {
        String email = principal.getName();
        var list = assignmentService.getMySubmissions(email);
        model.addAttribute("submissions", list);
        return "my-assignments";
    }
}

