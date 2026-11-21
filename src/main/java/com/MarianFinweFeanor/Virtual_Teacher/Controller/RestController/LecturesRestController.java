package com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LecturesRestController {
    private final LectureService lectureService;
    private final CourseService courseService;               // SCRUM-52
    private final CourseAccessService courseAccessService;   // SCRUM-52

    @Autowired
    public LecturesRestController(LectureService lectureService,
                                  CourseService courseService,
                                  CourseAccessService courseAccessService) {
        this.lectureService = lectureService;
        this.courseService = courseService;
        this.courseAccessService = courseAccessService;
    }
    @GetMapping
    public List<Lecture> getAllLectures() {
        return lectureService.getAllLectures();
    }
    @GetMapping("/{id}")
    public Lecture getLectureById(@PathVariable Long id) {
        return lectureService.getLecturesById(id).orElseThrow(()->new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, String.format("Lecture with id %d not found", id)));
    }
    @PostMapping
    public Lecture createLecture(@RequestBody Lecture lecture, Principal principal) {
        // SCRUM-52: only into a course the caller manages
        Long courseId = lecture.getCourse() == null ? null : lecture.getCourse().getCourseId();
        if (courseId == null) {
            throw new IllegalArgumentException("A lecture needs a course");
        }
        Course course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        courseAccessService.ensureCanManageCourse(course, emailOf(principal));
        try {
            return lectureService.createLectures(lecture);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error creating lecture", e);
        }

    }
    @PutMapping("/{id}")
    public Lecture updateLecture(@PathVariable Long id, @RequestBody Lecture updatedLecture,
                                 Principal principal) {
        ensureCanManageLecture(id, principal);   // SCRUM-52
        try {
            return lectureService.updateLecture(id, updatedLecture);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error updating lecture", e);
        }
    }
    @DeleteMapping("/{id}")
    public void deleteLecture(@PathVariable Long id, Principal principal) {
        ensureCanManageLecture(id, principal);   // SCRUM-52
        try {
            lectureService.delete(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting lecture", e);
        }
    }

    /** SCRUM-52: only the course's own teacher or an admin changes its lectures. */
    private void ensureCanManageLecture(Long lectureId, Principal principal) {
        Lecture lecture = lectureService.getLecturesById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", lectureId));
        courseAccessService.ensureCanManageCourse(lecture.getCourse(), emailOf(principal));
    }

    private static String emailOf(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
