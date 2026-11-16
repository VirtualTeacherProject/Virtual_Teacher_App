package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/**
 * SCRUM-6: student search for approved teachers (and admins).
 * Uses the SCRUM-19 user search, restricted to the STUDENT role.
 */
@Controller
@RequestMapping("/students")
public class StudentSearchMvcController {

    private final UserService userService;

    @Autowired
    public StudentSearchMvcController(UserService userService) {
        this.userService = userService;
    }

    /** GET /students?q=... (approved TEACHER / ADMIN) */
    @GetMapping({"", "/"})
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String searchStudents(@RequestParam(name = "q", required = false) String q,
                                 Model model,
                                 Principal principal) {
        // Teachers still waiting for approval are refused here.
        userService.ensureApprovedTeacher(principal.getName());

        model.addAttribute("students", userService.searchUsers(q, UserRole.STUDENT));
        model.addAttribute("q", q == null ? "" : q.trim());
        return "students";
    }
}
