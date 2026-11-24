package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    //List<Course> searchActiveCoursesByTitle(String query);


    List<Course> searchVisibleCoursesByTitle(String query, String userEmail, boolean canManageCourses);

    List<Course> getVisibleCourses(String userEmail, boolean canManageCourses);

    Course publishCourse(Long id);
    Course moveCourseToDraft(Long id);

    //  1. Create a New Course
    Course createCourse(Course course);

    /**
     * Creates a draft course on behalf of the logged-in user (REST API, issue 11).
     * The creator becomes the course's teacher; only an admin may name another
     * teacher (teacher.userId), who must be an active admin or approved teacher.
     */
    Course createCourseAs(Course course, String creatorEmail);

    // 2. Get a Course by ID
    Optional<Course> getCourseById(Long id);

    // 3. Get All Courses
    List<Course> getAllCourses();

    // 4. Update a Course
    Course updateCourse(Long id, Course updatedCourse);

    // 5. Delete a Course
    void deleteCourse(Long id);

    //  6. Get Courses by Status (e.g., Published, Draft)
    List<Course> getCoursesByStatus(String status);

    long countCourses();
    Course getCourse(Long id);

    /**
     * Hands the course to another teacher (SCRUM-12). The new teacher must be an
     * active admin or an active, approved teacher; otherwise IllegalArgumentException.
     */
    Course assignTeacher(Long courseId, Long teacherId);


}
