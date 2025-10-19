package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;


import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/lectures")
public class LectureMvcController {

    private final LectureService lectureService;
    private final UserService userService;          // enrollment checks
    private final CourseService courseService;      // breadcrumbs, ownership
    private final AssignmentService assignmentService;

    @Autowired
    public LectureMvcController(LectureService lectureService,
                                UserService userService,
                                CourseService courseService,
                                AssignmentService assignmentService) {
        this.lectureService = lectureService;
        this.userService = userService;
        this.courseService = courseService;
        this.assignmentService = assignmentService;
    }

    // --- Add Lecture (form) --------------------------------------------------

    /** GET /courses/{courseId}/lectures/add-lecture (TEACHER) */
    @GetMapping("/add-lecture")
    @PreAuthorize("hasRole('TEACHER')")
    public String showAddForm(@PathVariable Long courseId,
                              Model model,
                              Principal principal) {
        var course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        // (optional) Only the owning teacher may add to this course
        // if (!course.getTeacher().getEmail().equals(principal.getName())) {
        //     throw new AccessDeniedException("Not your course");
        // }

        model.addAttribute("course", course);
        model.addAttribute("lecture", new Lecture());   // course is attached on submit (server-side)
        return "add-lecture";
    }

    /** POST /courses/{courseId}/lectures/add-lecture (TEACHER) */
    @PostMapping("/add-lecture")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitAdd(@PathVariable Long courseId,
                            @Valid @ModelAttribute("lecture") Lecture lecture,
                            BindingResult br,
                            Principal principal,
                            Model model,
                            RedirectAttributes ra) {

        if (br.hasErrors()) {
            // view uses 'course' for form action/url
            model.addAttribute("course", courseService.getCourseById(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course", courseId)));
            return "add-lecture";
        }

        var course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        // (optional) ownership
        // if (!course.getTeacher().getEmail().equals(principal.getName())) {
        //     throw new AccessDeniedException("Not your course");
        // }

        lecture.setCourse(course);                 // URL is source of truth
        lectureService.createLectures(lecture);

        ra.addFlashAttribute("msg", "Lecture added!");
        return "redirect:/courses/" + courseId + "/lectures";
    }

    // --- Edit Lecture --------------------------------------------------------

    /** GET /courses/{courseId}/lectures/{lectureId}/edit (TEACHER) */
    @GetMapping("/{lectureId}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String showEditForm(@PathVariable Long courseId,
                               @PathVariable Long lectureId,
                               Model model,
                               Principal principal) {
        var lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        // ensure URL course matches lecture’s course (prevents cross-course edits)
        if (lecture.getCourse() == null || !lecture.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Lecture does not belong to this course");
        }

        // (optional) ownership
        // if (!lecture.getCourse().getTeacher().getEmail().equals(principal.getName())) {
        //     throw new AccessDeniedException("Not your course");
        // }

        model.addAttribute("lecture", lecture);
        model.addAttribute("course", lecture.getCourse());
        return "edit-lecture";
    }

    /** POST /courses/{courseId}/lectures/{lectureId}/edit (TEACHER) */
    @PostMapping("/{lectureId}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitEdit(@PathVariable Long courseId,
                             @PathVariable Long lectureId,
                             @Valid @ModelAttribute("lecture") Lecture updated,
                             BindingResult br,
                             RedirectAttributes ra,
                             Principal principal,
                             Model model) {

        if (br.hasErrors()) {
            model.addAttribute("course", courseService.getCourseById(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course", courseId)));
            return "edit-lecture";
        }

        // (optional) load + verify ownership before update
        // var existing = lectureService.getLecturesById(lectureId).orElseThrow(...);
        // if (!existing.getCourse().getCourseId().equals(courseId) ||
        //     !existing.getCourse().getTeacher().getEmail().equals(principal.getName())) {
        //     throw new AccessDeniedException("Not your course");
        // }

        lectureService.updateLecture(lectureId, updated);
        ra.addFlashAttribute("msg", "Lecture updated!");
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }

    // --- Lists & Detail ------------------------------------------------------

    /** GET /courses/{courseId}/lectures — everyone can view */
    @GetMapping("")
    public String listLectures(@PathVariable Long courseId,
                               Model model,
                               Principal principal) {
        var course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        var lectures = lectureService.getByCourseId(courseId);

        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(id -> id.equals(courseId));

        model.addAttribute("course", course);
        model.addAttribute("lectures", lectures);
        model.addAttribute("enrolled", enrolled);
        return "lectures";
    }

    /** GET /courses/{courseId}/lectures/{lectureId} — everyone can view */
    @GetMapping("/{lectureId}")
    public String lectureDetail(@PathVariable Long courseId,
                                @PathVariable Long lectureId,
                                Model model,
                                Principal principal) {
        var lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        // lecture must belong to the course in URL
        if (lecture.getCourse() == null || !lecture.getCourse().getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Lecture does not belong to this course");
        }

        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(id -> id.equals(courseId));

        List<Assignment> subs = enrolled
                ? assignmentService.getSubmissionsByLectureAndUser(lectureId, principal.getName())
                : List.of();

        model.addAttribute("courseId", courseId);
        model.addAttribute("lecture", lecture);
        model.addAttribute("enrolled", enrolled);
        model.addAttribute("submissions", subs);
        model.addAttribute("newAssignment", new Assignment());
        return "lecture-detail";
    }

    // --- Assignments ---------------------------------------------------------

    /** POST /courses/{courseId}/lectures/{lectureId}/assignments (STUDENT/TEACHER/ADMIN) */
    @PostMapping("/{lectureId}/assignments")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public String uploadAssignment(@PathVariable Long courseId,
                                   @PathVariable Long lectureId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("comment") String comment,
                                   Principal principal,
                                   RedirectAttributes ra) {
        try {
            assignmentService.submit(principal.getName(), lectureId, file, comment);
            ra.addFlashAttribute("msg", "Assignment submitted!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }
}


