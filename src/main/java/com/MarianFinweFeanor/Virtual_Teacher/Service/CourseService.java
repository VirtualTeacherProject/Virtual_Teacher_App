package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    //  1. Create a New Course
    Course createCourse(Course course);

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
}
