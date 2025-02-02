package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    // Constructor Injection
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    //  1. Create a New Course
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    // 2. Get a Course by ID
    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    // 3. Get All Courses
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // 4. Update a Course
    public Course updateCourse(Long id, Course updatedCourse) {
        return courseRepository.findById(id)
                .map(course -> {
                    course.setTitle(updatedCourse.getTitle());
                    course.setTopic(updatedCourse.getTopic());
                    course.setDescription(updatedCourse.getDescription());
                    course.setStartDate(updatedCourse.getStartDate());
                    course.setStatus(updatedCourse.getStatus());
                    return courseRepository.save(course);
                })
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + id));
    }

    // 5. Delete a Course
    public void deleteCourse(Long id) {
        if (courseRepository.existsById(id)) {
            courseRepository.deleteById(id);
        } else {
            throw new RuntimeException("Course not found with ID: " + id);
        }
    }

    //  6. Get Courses by Status (e.g., Published, Draft)
    public List<Course> getCoursesByStatus(String status) {
        return courseRepository.findByStatus(status);
    }
}
