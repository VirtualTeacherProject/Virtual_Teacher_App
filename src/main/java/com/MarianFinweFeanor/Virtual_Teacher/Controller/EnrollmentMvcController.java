package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
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
import java.time.LocalDateTime;
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
    private EnrollmentRepository enrollmentRepository;

    public EnrollmentMvcController(UserService userService,
                                   CourseService courseService,
                                   EnrollmentRepository enrollmentRepository,
                                   EnrollmentService enrollmentService) {
        this.userService = userService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.enrollmentRepository = enrollmentRepository;
    }

    // --- List Courses (public) ----------------------------------------------

    /** GET /courses — everyone can browse */
    @GetMapping("")
    public String listCourses(Model model, Principal principal) {
        System.out.println("LIST COURSES HIT, principal = " +
                (principal == null ? "anonymous" : principal.getName()));

        //var all = courseService.getAllCourses();

        boolean canManageCourses = canManageCourses(principal);

        var all = courseService.getVisibleCourses(
                principal == null ? null : principal.getName(),
                canManageCourses
        );

        var enrolledIds = (principal == null)
                ? java.util.Collections.<Long>emptySet()
                : enrollmentService.getEnrolledCourseIds(principal.getName());

        boolean isTeacher = hasRole("ROLE_TEACHER");
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

    @GetMapping("/search")
    public String searchCourses(@RequestParam(required = false) String query, Model model,
                                Principal principal) {
        boolean canManageCourses = canManageCourses(principal);

        List<Course> courses = courseService.searchVisibleCoursesByTitle(
                query,
                principal == null ? null : principal.getName(),
                canManageCourses
        );

        var enrolledIds = (principal == null)
                ? java.util.Collections.<Long>emptySet()
                : enrollmentService.getEnrolledCourseIds(principal.getName());

        boolean isTeacher = hasRole("ROLE_TEACHER");
        if (principal != null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            isTeacher = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
        }

        model.addAttribute("courses", courses);
        model.addAttribute("enrolledIds", enrolledIds);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("query", query);
        return "courses";
    }

    /** GET /courses/add (TEACHER) */
    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String showAddCourseForm(Model model, Principal principal) {
        userService.ensureApprovedTeacher(principal.getName());
        model.addAttribute("course", new Course());
        return "add-course";
    }

    /** POST /courses/add (TEACHER) */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String submitAddCourse(@Valid @ModelAttribute("course") Course course,
                                  BindingResult br,
                                  Principal principal,
                                  RedirectAttributes ra) {
        userService.ensureApprovedTeacher(principal.getName());

        if (br.hasErrors()) {
            return "add-course";
        }

        // attach owner
        var me = userService.findByEmail(principal.getName());
        course.setTeacher(me);

        // DEFAULT if left empty in the form
        if (course.getStartDate() == null) {
            course.setStartDate(LocalDateTime.now());  // or any business default
        }

        course.setStatus("DRAFT");

        courseService.createCourse(course);
        ra.addFlashAttribute("msg", "Course created as draft!");
        return "redirect:/courses";
    }

    /** GET /courses/{id}/edit (TEACHER) */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String showEditCourseForm(@PathVariable Long id, Model model, Principal principal) {

        userService.ensureApprovedTeacher(principal.getName());
        var c = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        model.addAttribute("course", c);
        return "edit-course";
    }

    /** POST /courses/{id}/edit (TEACHER) */
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String submitEditCourse(@PathVariable Long id,
                                   @ModelAttribute("course") Course updated,
                                   BindingResult br,
                                   RedirectAttributes ra,
                                   Model model,
                                   Principal principal) {
        userService.ensureApprovedTeacher(principal.getName());

        if (br.hasErrors()) {
            return "edit-course";
        }

        Course existing = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        existing.setTitle(updated.getTitle());
        existing.setTopic(updated.getTopic());
        existing.setDescription(updated.getDescription());

        if (updated.getPassingGrade() != null) {
            existing.setPassingGrade(updated.getPassingGrade());
        }

        if (existing.getStatus() == null || existing.getStatus().isBlank()) {
            existing.setStatus("DRAFT");
        }

        // Only overwrite startDate if user actually provided one
        if (updated.getStartDate() != null) {
            existing.setStartDate(updated.getStartDate());
        }

        courseService.updateCourse(id, existing);
        ra.addFlashAttribute("msg", "Course updated!");
        return "redirect:/courses/" + id;
    }

    // --- Course Detail (public) ---------------------------------------------

    /** GET /courses/{id} — public detail; enroll actions are gated in the view */
    @GetMapping("/{id:\\d+}")
    public String courseDetail(@PathVariable Long id,
                               Model model,
                               Principal principal) {
        var course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        if (!canViewCourse(course, principal)) {
            throw new EntityNotFoundException("Course", id);
        }


        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(cid -> cid.equals(id));

        Enrollment enrollment = null;
        if (principal != null) {
            enrollment = enrollmentRepository
                    .findByStudent_EmailAndCourse_CourseId(principal.getName(),
                            course.getCourseId())
                    .orElse(null);

            model.addAttribute("currentUser", userService
                    .findByEmail(principal.getName()));
        }

        model.addAttribute("enrollment", enrollment);

        model.addAttribute("course", course);
        model.addAttribute("enrolled", enrolled);
        return "course-detail";
    }

    // --- Students in Course (teacher/admin) ---------------------------------

    /** GET /courses/{id}/students (TEACHER/ADMIN) */
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public String courseStudents(@PathVariable Long id, Model model,
                                 Principal principal) {

        userService.ensureApprovedTeacher(principal.getName());
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

        var course = courseService.getCourseById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        if (!"PUBLISHED".equals(course.getStatus())) {
            ra.addFlashAttribute("error", "This course is not published yet.");
            return "redirect:/courses";
        }

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

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String publishCourse(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes ra) {

        userService.ensureApprovedTeacher(principal.getName());

        try {
            courseService.publishCourse(id);
            ra.addFlashAttribute("msg", "Course published successfully!");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/courses/" + id;
    }


    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
    }

    private boolean canManageCourses(Principal principal) {
        if (principal == null) {
            return false;
        }

        if (hasRole("ROLE_ADMIN")) {
            return true;
        }

        if (hasRole("ROLE_TEACHER")) {
            User user = userService.findByEmail(principal.getName());
            return Boolean.TRUE.equals(user.isTeacherApproved());
        }

        return false;
    }

    private boolean canViewCourse(Course course, Principal principal) {
        if ("PUBLISHED".equals(course.getStatus())) {
            return true;
        }

        return canManageCourses(principal);
    }
}

