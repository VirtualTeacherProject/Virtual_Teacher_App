package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.CourseServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.UserServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    private final UserService userService;        // interface
    private final CourseService courseService;    // interface
    private final EnrollmentService enrollmentService;

    // <-- constructor injection
    public EnrollmentMvcController(UserService userService,
                                   CourseService courseService,
                                   EnrollmentService enrollmentService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * List all courses in the catalog.
     *
     * Now open to anonymous users (Principal may be null), so:
     * 1. We fetch *all* courses for everyone.
     * 2. We only attempt to look up “enrolled” IDs if the user is logged in.
     * 3. This prevents NullPointerExceptions and lets anonymous visitors browse.
     */

    @GetMapping("")
    public String listCourses(Model model, Principal principal) {
        // Fetch all available courses
        List<Course> all = courseService.getAllCourses();

        // Determine enrolled course IDs (empty set if not logged in)
        Set<Long> enrolledIds = (principal == null)
                ? java.util.Collections.emptySet()
                : enrollmentService.getEnrolledCourseIds(principal.getName());

        // Check if the current user is a teacher
        boolean isTeacher = false;
        if (principal != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            isTeacher = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        }

        // Add data to the model for Thymeleaf
        model.addAttribute("courses", all);
        model.addAttribute("enrolledIds", enrolledIds);
        model.addAttribute("isTeacher", isTeacher);

        return "courses";
    }


    //  — Teacher: show “Add Course” form —
    @GetMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public String showAddCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "add-course";
    }

    //  — Teacher: handle “Add Course” submit —
    @PostMapping("/add")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitAddCourse(@ModelAttribute Course course,
                                  Principal principal,
                                  RedirectAttributes ra) {
        // set logged‑in teacher as owner
        User me = userService.findByEmail(principal.getName());
        course.setTeacher(me);
        courseService.createCourse(course);
        ra.addFlashAttribute("msg", "Course created!");
        return "redirect:/courses";
    }

    //  — Teacher: show “Edit Course” form —
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String showEditCourseForm(@PathVariable Long id, Model model) {
        Course c = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        model.addAttribute("course", c);
        return "edit-course";
    }

    //  — Teacher: handle “Edit Course” submit —
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitEditCourse(@PathVariable Long id,
                                   @ModelAttribute Course updated,
                                   RedirectAttributes ra) {
        courseService.updateCourse(id, updated);
        ra.addFlashAttribute("msg", "Course updated!");
        return "redirect:/courses/" + id;
    }


    /**
     * Show detail for a single course.
     *
     * Anonymous users can view the page; they simply never see an “Enroll” button
     * because enrolled=false when principal==null.
     */
    @GetMapping("/{id}")
    public String courseDetail(@PathVariable Long id,
                               Model model,
                               Principal principal) {
        // Load the course or 404
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        // Check enrollment only if logged in; otherwise keep false
        boolean enrolled = false;
        if (principal != null) {
            enrolled = userService.getEnrolledCourses(principal.getName())
                    .stream()
                    .map(Course::getCourseId)
                    .anyMatch(cid -> cid.equals(id));
        }

        model.addAttribute("course", course);
        model.addAttribute("enrolled", enrolled);

        return "course-detail";
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String courseStudents(@PathVariable Long id, Model model) {
        var course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        var students = enrollmentService.getStudentsInCourse(id); // service queries exactly what we need
        model.addAttribute("course", course);
        model.addAttribute("students", students);
        return "course-students"; // create this template
    }


    @PostMapping("/{id}/enroll")
    public String enroll(@PathVariable("id") Long id,
                         Principal principal,
                         RedirectAttributes ra) {
        String email = principal.getName();
        // check if already enrolled
        boolean already = enrollmentService.isEnrolled(email, id);

        if (enrollmentService.isEnrolled(email, id)) {
            ra.addFlashAttribute("msg", "You’re already enrolled in this course.");
        } else {
            enrollmentService.enroll(email, id);
            ra.addFlashAttribute("msg", "Enrolled successfully!");
        }
        return "redirect:/courses/" + id;
    }

    @PostMapping("/{id}/unenroll")
    public String unenroll(@PathVariable("id") Long id,
                           Principal principal,
                           RedirectAttributes ra) {
        enrollmentService.unenroll(principal.getName(), id);
        ra.addFlashAttribute("msg", "You have been unenrolled.");
        return "redirect:/courses/" + id;
    }

    @GetMapping("/my-courses")
    public String myCourses(Model m, Principal p) {
        m.addAttribute("enrolledCourses", userService.getEnrolledCourses(p.getName()));
        return "my-courses";
    }
}
