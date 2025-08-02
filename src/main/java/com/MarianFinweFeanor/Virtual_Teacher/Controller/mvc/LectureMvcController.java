package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;


import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureServiceImpl;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
    private final UserService userService;    // for checking “enrolled”
    private final CourseService courseService;  // for breadcrumb back to course
    private final AssignmentService assignmentService;

    @Autowired
    public LectureMvcController(LectureService      lectureService,
                                UserService         userService,
                                CourseService       courseService,
                                AssignmentService   assignmentService) {
        this.lectureService   = lectureService;
        this.userService      = userService;
        this.courseService    = courseService;
        this.assignmentService = assignmentService;
    }


    // — Teacher-only: show “Add Lecture” form —
    @GetMapping("/add-lecture")
    @PreAuthorize("hasRole('TEACHER')")
    public String showAddForm(@PathVariable Long courseId, Model model) {
        Lecture lec = new Lecture();
        lec.setCourse(courseService.getCourseById(courseId).orElseThrow());
        model.addAttribute("lecture", lec);
        return "add-lecture";
    }

    // — Teacher-only: handle “Add Lecture” submit —
    @PostMapping("/add-lecture")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitAdd(@ModelAttribute Lecture lecture) {
        lectureService.createLectures(lecture);
        return "redirect:/courses/" + lecture.getCourse().getCourseId() + "/lectures";
    }

    // — Teacher-only: show “Edit Lecture” form —
    @GetMapping("/{lectureId}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String showEditForm(@PathVariable Long courseId,
                               @PathVariable Long lectureId,
                               Model model) {
        Lecture lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));
        model.addAttribute("lecture", lecture);
        return "edit-lecture";
    }

    // — Teacher-only: handle “Edit Lecture” submit —
    @PostMapping("/{lectureId}/edit")
    @PreAuthorize("hasRole('TEACHER')")
    public String submitEdit(@PathVariable Long courseId,
                             @PathVariable Long lectureId,
                             @ModelAttribute Lecture updated,
                             RedirectAttributes ra) {
        lectureService.updateLecture(lectureId, updated);
        ra.addFlashAttribute("msg", "Lecture updated!");
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId;
    }



    // — 1️⃣ List lectures for everyone —
    // now handles GET /courses/{courseId}/lectures
    @GetMapping("")
    public String listLectures(@PathVariable Long courseId,
                               Model model,
                               Principal principal) {
        var course   = courseService.getCourseById(courseId)
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



    // — 2️⃣ Lecture detail + assignment-upload form —
    @GetMapping("/{lectureId}")
    public String lectureDetail(@PathVariable Long courseId,
                                @PathVariable Long lectureId,
                                Model model,
                                Principal principal) {
        var lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));

        boolean enrolled = principal != null &&
                userService.getEnrolledCourses(principal.getName())
                        .stream()
                        .map(Course::getCourseId)
                        .anyMatch(id -> id.equals(courseId));

        List<Assignment> subs = List.of();
        if (enrolled) {
            subs = assignmentService
                    .getSubmissionsByLectureAndUser(lectureId, principal.getName());
        }

        model.addAttribute("courseId",    courseId);
        model.addAttribute("lecture",     lecture);
        model.addAttribute("enrolled",    enrolled);
        model.addAttribute("submissions", subs);
        model.addAttribute("newAssignment", new Assignment());
        return "lecture-detail";
    }

    // — 3️Handle file-upload (students only) —
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

