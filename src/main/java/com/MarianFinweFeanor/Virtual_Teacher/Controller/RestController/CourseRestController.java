package com.MarianFinweFeanor.Virtual_Teacher.Controller.RestController;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import jakarta.validation.Valid;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/courses")
public class CourseRestController {

    private final CourseService courseService;
    private final CourseAccessService courseAccessService;

    @Autowired
    public CourseRestController(CourseService courseService,
                                CourseAccessService courseAccessService) {
        this.courseService = courseService;
        this.courseAccessService = courseAccessService;
    }

    //Create or Update a course
    @PostMapping
    public ResponseEntity<?> createCourse(@Valid @RequestBody Course course, BindingResult br) {
            try {
            Course saveCourse = courseService.createCourse(course);
            return ResponseEntity.status(HttpStatus.CREATED).body(saveCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // SCRUM-22: same visibility as the /courses page — published courses,
    // plus the caller's own drafts if they can manage courses.
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses(Principal principal) {
        String email = principal == null ? null : principal.getName();
        boolean canManage = courseAccessService.canManageCourses(email);
        return ok(courseService.getVisibleCourses(email, canManage));
    }

    // SCRUM-22: a draft the caller may not view answers 404, like a missing course.
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id, Principal principal) {
        String email = principal == null ? null : principal.getName();
        return courseService.getCourseById(id)
                .filter(course -> courseAccessService.canViewCourse(course, email))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete a course by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id, Principal principal) {
        ensureCanManage(id, principal);   // SCRUM-52
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();


    }
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course updatedCourse,
                                               Principal principal) {
        ensureCanManage(id, principal);   // SCRUM-52
        Course course = courseService.updateCourse(id, updatedCourse);
        return ResponseEntity.ok(course);
    }

    /** SCRUM-52: only the course's own teacher or an admin changes or deletes it. */
    private void ensureCanManage(Long courseId, Principal principal) {
        Course course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        courseAccessService.ensureCanManageCourse(course, principal == null ? null : principal.getName());
    }
}
