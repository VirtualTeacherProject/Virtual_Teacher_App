package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    // Constructor Injection
    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    //  1. Create a New Course
    @Override
    public Course createCourse(Course course) {
        Long teacherId = course.getTeacher().getUserId();

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("User", teacherId));

        course.setTeacher(teacher); // Attach managed entity
        // todo Check if the course already exists after looking up filtering


        return courseRepository.save(course);
    }

    // 2. Get a Course by ID
    @Override
    public Optional<Course> getCourseById(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new EntityNotFoundException("Course", id);
        }
        return courseRepository.findById(id);
    }


    @Override
    public long countCourses() {
        return courseRepository.count();
    }



    @Override
    public Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }


    // 3. Get All Courses
//    @Override
//    public List<Course> getAllCourses() {
//        if(courseRepository.count() == 0) {
//            throw new EntityNotFoundException("Courses", "database");
//        }
//        return courseRepository.findAll();
//    }


    

    @Override
    public List<Course> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        if (courses.isEmpty()) {
            System.out.println("⚠️ No courses found in database.");
        }
        return courses;
    }

    // 4. Update a Course
    @Override
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
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
    }

    // 5. Delete a Course
    @Override
    public void deleteCourse(Long id) {
        if (courseRepository.existsById(id)) {
            courseRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException("Course", id);
        }
    }

    //  6. Get Courses by Status (e.g., Published, Draft)
    @Override
    public List<Course> getCoursesByStatus(String status) {
        //todo look up filtering and exception handling
        return courseRepository.findByStatus(status);
    }
}
