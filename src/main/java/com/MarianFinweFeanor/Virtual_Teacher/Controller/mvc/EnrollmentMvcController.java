package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Service.CourseServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.UserServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses")
public class EnrollmentMvcController {

    private final UserService userService;        // interface
    private final CourseService courseService;    // interface

    // <-- constructor injection
    public EnrollmentMvcController(UserService userService,
                                   CourseService courseService) {
        this.userService = userService;
        this.courseService = courseService;
    }

    // 3.1 List all courses
    @GetMapping("")
    public String listCourses(Model model, Principal principal) {
        List<Course> all = courseService.getAllCourses();
        Set<Long> enrolledIds = userService.getEnrolledCourses(principal.getName())
                .stream()
                .map(Course::getCourseId)
                .collect(Collectors.toSet());

        model.addAttribute("courses", all);
        model.addAttribute("enrolledIds", enrolledIds);
        return "courses";
    }

    @GetMapping("/{id}")
    public String courseDetail(@PathVariable Long id,
                               Model model,
                               Principal principal) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        model.addAttribute("course", course);

        boolean enrolled = userService.getEnrolledCourses(principal.getName())
                .stream()
                .anyMatch(course1 -> course1.getCourseId().equals(id));
        model.addAttribute("enrolled", enrolled);

        return "course-detail";
    }


    @PostMapping("/{id}/enroll")
    public String enroll(@PathVariable("id") Long id,
                         Principal principal,
                         RedirectAttributes ra) {
        String email = principal.getName();
        // check if already enrolled
        boolean already = userService.getEnrolledCourses(email)
                .stream()
                .map(Course::getCourseId)
                .anyMatch(cid -> cid.equals(id));

        if (already) {
            ra.addFlashAttribute("msg", "You’re already enrolled in this course.");
        } else {
            userService.enrollInCourse(email, id);
            ra.addFlashAttribute("msg", "Enrolled successfully!");
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/unenroll")
    public String unenroll(@PathVariable("id") Long id,
                           Principal principal,
                           RedirectAttributes ra) {
        userService.unenrollFromCourse(principal.getName(), id);
        ra.addFlashAttribute("msg", "You have been unenrolled.");
        return "redirect:/courses/" + id;
    }

    @GetMapping("/my-courses")
    public String myCourses(Model m, Principal p) {
        m.addAttribute("enrolledCourses", userService.getEnrolledCourses(p.getName()));
        return "my-courses";
    }
}
