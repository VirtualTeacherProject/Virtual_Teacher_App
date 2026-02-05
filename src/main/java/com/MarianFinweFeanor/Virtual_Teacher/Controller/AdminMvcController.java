package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMvcController {
    String firstName;
    String lastName;
    String email;


    private final UserService userService;
    private final CourseService courseService;

    @GetMapping({"", "/", "/dashboard"})
    public String adminHome(Model model) {

        long totalUsers = userService.countUsers();
        long totalCourses = courseService.countCourses();
        long totalTeachers = userService.countByRole(UserRole.TEACHER);


        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("totalTeachers", totalTeachers);
        return "admin/dashboard";
    }
}

