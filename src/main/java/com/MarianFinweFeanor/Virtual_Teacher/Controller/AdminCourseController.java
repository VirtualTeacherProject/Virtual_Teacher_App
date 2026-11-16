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

import java.util.ArrayList;
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
        courseService.deleteCourse(id); // SCRUM-15: removes the course's lectures and enrollments, never users
        ra.addFlashAttribute("msg", "Course deleted.");
        return "redirect:/admin/courses";
    }

    @GetMapping("/{id}/edit")
    public String showEditCourseForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        model.addAttribute("course", course);
        addTeacherOptions(model, course.getTeacher(),
                course.getTeacher() == null ? null : course.getTeacher().getUserId());
        return "admin/course-edit";
    }

    @PostMapping("/{id}/edit")
    public String submitEditCourse(@PathVariable Long id,
                                   @ModelAttribute("course") @Valid Course form,
                                   BindingResult br,
                                   @RequestParam(name = "teacherId", required = false) Long teacherId,
                                   Model model,
                                   RedirectAttributes ra) {

        Course existing = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        String requestedStatus = form.getStatus();

        if (!br.hasErrors()
                && !"DRAFT".equalsIgnoreCase(requestedStatus)
                && !"PUBLISHED".equalsIgnoreCase(requestedStatus)) {
            br.rejectValue(
                    "status",
                    "invalid.status",
                    "Status must be DRAFT or PUBLISHED."
            );
        }

        // SCRUM-12: the admin can hand the course to another teacher
        boolean teacherChanged = teacherId != null
                && (existing.getTeacher() == null
                || !teacherId.equals(existing.getTeacher().getUserId()));

        if (!br.hasErrors() && teacherChanged) {
            try {
                courseService.assignTeacher(id, teacherId);
            } catch (IllegalArgumentException | EntityNotFoundException ex) {
                br.reject("invalid.teacher", ex.getMessage());
                model.addAttribute("error", ex.getMessage());
            }
        }

        if (br.hasErrors()) {
            form.setCourseId(id); // the form posts back to this course
            addTeacherOptions(model, existing.getTeacher(), teacherId);
            return "admin/course-edit";
        }

        courseService.updateCourse(id, form);

        if ("PUBLISHED".equalsIgnoreCase(requestedStatus)) {
            courseService.publishCourse(id);
        } else {
            courseService.moveCourseToDraft(id);
        }

        ra.addFlashAttribute("msg", "Course updated.");
        return "redirect:/admin/courses";
    }

    /**
     * Options for the teacher select: everyone who may teach, plus the current
     * teacher even if they could not be picked today (e.g. deactivated).
     */
    private void addTeacherOptions(Model model, User currentTeacher, Long selectedTeacherId) {
        List<User> teachers = new ArrayList<>(userService.findAssignableTeachers());

        if (currentTeacher != null && !teachers.contains(currentTeacher)) {
            teachers.add(0, currentTeacher);
        }

        model.addAttribute("teachers", teachers);
        model.addAttribute("selectedTeacherId",
                selectedTeacherId != null
                        ? selectedTeacherId
                        : (currentTeacher == null ? null : currentTeacher.getUserId()));
    }
}
