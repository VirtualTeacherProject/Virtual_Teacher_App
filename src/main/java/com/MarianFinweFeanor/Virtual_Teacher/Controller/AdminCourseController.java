package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final UserService userService;

    @GetMapping
    public String listCourses(Model model) {
        //model.addAttribute("courses", courseService.findAll());
        model.addAttribute("courses", courseService.getAllCourses());
        return "admin/courses";
    }

    @PostMapping("/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes ra) {
        courseService.deleteCourse(id); // your service should handle cascades or rely on DB ON DELETE CASCADE
        ra.addFlashAttribute("msg", "Course deleted.");
        return "redirect:/admin/courses";
    }

    @GetMapping("/{id}/edit")
    public String showEditCourseForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        model.addAttribute("course", course);
        return "admin/course-edit";
    }

    @PostMapping("/{id}/edit")
    public String submitEditCourse(@PathVariable Long id,
                                   @ModelAttribute("course") @Valid Course form,
                                   BindingResult br,
                                   RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "admin/course-edit";
        }

        Course existing = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        existing.setTitle(form.getTitle());
        existing.setTopic(form.getTopic());
        existing.setDescription(form.getDescription());

        // normalize status similar to your teacher edit logic
        String incoming = form.getStatus();
        if (incoming == null || incoming.isBlank()) {
            existing.setStatus(existing.getStatus() != null ? existing.getStatus() : "ACTIVE");
        } else {
            var s = incoming.trim().toUpperCase();
            existing.setStatus(("ACTIVE".equals(s) || "PASSIVE".equals(s)) ? s : "ACTIVE");
        }

        courseService.updateCourse(id, existing);

        ra.addFlashAttribute("msg", "Course updated.");
        return "redirect:/admin/courses";
    }
}

