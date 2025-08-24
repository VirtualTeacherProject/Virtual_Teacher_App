package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    @Autowired
    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepo,
                                 UserRepository userRepo,
                                 CourseRepository courseRepo) {
        this.enrollmentRepo = enrollmentRepo;
        this.userRepo       = userRepo;
        this.courseRepo     = courseRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getEnrolledCourseIds(String userEmail) {
        //return enrollmentRepo.findEnrolledCourseIdsByEmail(userEmail);

        return enrollmentRepo.findByStudent_Email(userEmail).stream()
                .map(e -> e.getCourse().getCourseId())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Course> getEnrolledCourses(String userEmail) {
        return enrollmentRepo.findByStudent_Email(userEmail)
                .stream()
                .map(Enrollment::getCourse)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getStudentsInCourse(Long courseId) {
        return enrollmentRepo.findStudentsByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(String userEmail, Long courseId) {
        return enrollmentRepo.existsByStudent_EmailAndCourse_CourseId(userEmail, courseId);
    }

    @Override
    public void enroll(String userEmail, Long courseId) {
        if (isEnrolled(userEmail, courseId)) return; // already enrolled

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User", userEmail));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        Enrollment e = new Enrollment();
        e.setStudent(user);
        e.setCourse(course);
        e.setCompletionStatus(Enrollment.CompletionStatus.IN_PROGRESS);  // defaults; adjust to your enum/strings
        e.setAverageGrade(null);

        enrollmentRepo.save(e);
    }

    @Override
    public void unenroll(String userEmail, Long courseId) {
        Enrollment e = enrollmentRepo.findByStudent_EmailAndCourse_CourseId(userEmail, courseId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment for user", userEmail + " & course " + courseId));
        enrollmentRepo.delete(e);
    }
}
