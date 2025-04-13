package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseServiceImpl implements CourseService{

    private final CourseRepository courseRepository;
    private final UserRepository userRepository; // Add this

    // Constructor Injection
    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    //  1. Create a New Course
    public Course createCourse(Course course) {
        Long teacherId = course.getTeacher().getUserId();

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with ID: " + teacherId));

        course.setTeacher(teacher); // Attach managed entity

        System.out.println("DEBUG - Title: " + course.getTitle());
        System.out.println("DEBUG - Topic: " + course.getTopic());
        System.out.println("DEBUG - Description: " + course.getDescription());
        System.out.println("DEBUG - StartDate: " + course.getStartDate());
        System.out.println("DEBUG - Status: " + course.getStatus());
        System.out.println("DEBUG - Teacher ID: " + course.getTeacher().getUserId());

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
