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

        course.setTeacher(teacher);
        course.setStatus("DRAFT");

        return courseRepository.save(course);
    }




    // 2. Get a Course by ID

    @Override
    public Optional<Course> getCourseById(Long id) {
        return Optional.of(courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id)));
    }

    @Override
    public Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
    }

    @Override
    public Course publishCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        if (course.getTitle() == null || course.getTitle().isBlank()) {
            throw new IllegalStateException("Course title is required before publishing.");
        }

        if (course.getTopic() == null || course.getTopic().isBlank()) {
            throw new IllegalStateException("Course topic is required before publishing.");
        }

        if (course.getDescription() == null || course.getDescription().isBlank()) {
            throw new IllegalStateException("Course description is required before publishing.");
        }

        course.setStatus("PUBLISHED");
        return courseRepository.save(course);
    }

    @Override
    public List<Course> getVisibleCourses(String userEmail, boolean canManageCourses) {
        if (canManageCourses && userEmail != null) {
            return courseRepository.findByStatusOrTeacher_Email("PUBLISHED", userEmail);
        }

        return courseRepository.findByStatus("PUBLISHED");
    }

    @Override
    public List<Course> searchVisibleCoursesByTitle(String title,
                                                    String userEmail,
                                                    boolean canManageCourses) {
        if (title == null || title.trim().isEmpty()) {
            return getVisibleCourses(userEmail, canManageCourses);
        }

        String trimmedTitle = title.trim();

        if (canManageCourses && userEmail != null) {
            return courseRepository
                    .findByStatusAndTitleContainingIgnoreCaseOrTeacher_EmailAndTitleContainingIgnoreCase(
                            "PUBLISHED",
                            trimmedTitle,
                            userEmail,
                            trimmedTitle
                    );
        }

        return courseRepository.findByStatusAndTitleContainingIgnoreCase("PUBLISHED", trimmedTitle);
    }


    @Override
    public long countCourses() {
        return courseRepository.count();
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
                    course.setPassingGrade(updatedCourse.getPassingGrade());

                    // Status is intentionally not changed here.
                    // Publishing is handled only through publishCourse().
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
