package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.CourseServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.UserServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses")
public class EnrollmentMvcController {

    private final UserService userService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public EnrollmentMvcController(UserService userService,
                                   CourseService courseService,
                                   EnrollmentService enrollmentService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    // --- List Courses (public) ----------------------------------------------

    /** GET /courses — everyone can browse */
    @GetMapping("")
    public String listCourses(Model model, Principal principal) {
        var all = courseService.getAllCourses();

        var enrolledIds = (principal == null)
                ? java.util.Collections.<Long>emptySet()
                : enrollmentService.getEnrolledCourseIds(principal.getName());

        boolean isTeacher = false;
        if (principal != null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            isTeacher = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
        }

        model.addAttribute("courses", all);
        model.addAttribute("enrolledIds", enrolledIds);
        model.addAttribute("isTeacher", isTeacher);
        return "courses";
    }

    // --- Create/Edit Course (teacher) ---------------------------------------

    /** GET /courses/add (TEACHER) */
    @GetMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public String showAddCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "add-course";
    }

    /** POST /courses/add (TEACHER) */
    @PostMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitAddCourse(@Valid @ModelAttribute("course") Course course,
                                  BindingResult br,
                                  Principal principal,
                                  RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "add-course";
        }
        // attach owner
        var me = userService.findByEmail(principal.getName());
        course.setTeacher(me);

        courseService.createCourse(course);
        ra.addFlashAttribute("msg", "Course created!");
        return "redirect:/courses";
    }

    /** GET /courses/{id}/edit (TEACHER) */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String showEditCourseForm(@PathVariable Long id, Model model) {
        var c = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        model.addAttribute("course", c);
        return "edit-course";
    }

    /** POST /courses/{id}/edit (TEACHER) */
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitEditCourse(@PathVariable Long id,
                                   @Valid @ModelAttribute("course") Course updated,
                                   BindingResult br,
                                   RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "edit-course";
        }
        courseService.updateCourse(id, updated);
        ra.addFlashAttribute("msg", "Course updated!");
        return "redirect:/courses/" + id;
    }

    // --- Course Detail (public) ---------------------------------------------

    /** GET /courses/{id} — public detail; enroll actions are gated in the view */
    @GetMapping("/{id}")
    public String courseDetail(@PathVariable Long id,
                               Model model,
                               Principal principal) {
        var course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(cid -> cid.equals(id));

        model.addAttribute("course", course);
        model.addAttribute("enrolled", enrolled);
        return "course-detail";
    }

    // --- Students in Course (teacher/admin) ---------------------------------

    /** GET /courses/{id}/students (TEACHER/ADMIN) */
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String courseStudents(@PathVariable Long id, Model model) {
        var course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        var students = enrollmentService.getStudentsInCourse(id);

        model.addAttribute("course", course);
        model.addAttribute("students", students);
        return "course-students";
    }

    // --- Enroll / Unenroll (authenticated) ----------------------------------

    /** POST /courses/{id}/enroll (authenticated) */
    @PostMapping("/{id}/enroll")
    public String enroll(@PathVariable("id") Long id,
                         Principal principal,
                         RedirectAttributes ra) {
        var email = principal.getName();
        if (enrollmentService.isEnrolled(email, id)) {
            ra.addFlashAttribute("msg", "You’re already enrolled in this course.");
        } else {
            enrollmentService.enroll(email, id);
            ra.addFlashAttribute("msg", "Enrolled successfully!");
        }
        return "redirect:/courses/" + id;
    }

    /** POST /courses/{id}/unenroll (authenticated) */
    @PostMapping("/{id}/unenroll")
    public String unenroll(@PathVariable("id") Long id,
                           Principal principal,
                           RedirectAttributes ra) {
        enrollmentService.unenroll(principal.getName(), id);
        ra.addFlashAttribute("msg", "You have been unenrolled.");
        return "redirect:/courses/" + id;
    }

    // --- My Courses (authenticated) -----------------------------------------

    /** GET /courses/my-courses (authenticated) */
    @GetMapping("/my-courses")
    public String myCourses(Model model, Principal principal) {
        model.addAttribute("enrolledCourses", userService.getEnrolledCourses(principal.getName()));
        return "my-courses";
    }
}

