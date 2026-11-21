package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureProgressService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;

/** SCRUM-51 part B: how far each enrolled student is in a course (teachers and admins). */
@Controller
public class CourseProgressMvcController {

    private final CourseService courseService;
    private final CourseAccessService courseAccessService;
    private final LectureProgressService lectureProgressService;
    private final UserService userService;

    @Autowired
    public CourseProgressMvcController(CourseService courseService,
                                       CourseAccessService courseAccessService,
                                       LectureProgressService lectureProgressService,
                                       UserService userService) {
        this.courseService = courseService;
        this.courseAccessService = courseAccessService;
        this.lectureProgressService = lectureProgressService;
        this.userService = userService;
    }

    /** GET /courses/{courseId}/progress (TEACHER/ADMIN) */
    @GetMapping("/courses/{courseId}/progress")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String showProgress(@PathVariable Long courseId, Principal principal, Model model) {
        userService.ensureApprovedTeacher(principal.getName());

        Course course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        courseAccessService.ensureCanManageCourse(course, principal.getName());

        model.addAttribute("course", course);
        model.addAttribute("rows", lectureProgressService.getCourseProgress(courseId));
        return "course-progress";
    }
}
