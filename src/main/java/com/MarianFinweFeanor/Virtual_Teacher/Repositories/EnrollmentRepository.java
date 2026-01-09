package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);
    List<Enrollment> findByCourse(Course course);

    boolean existsByStudentAndCourse(User student, Course course);

    // For quick checks
    boolean existsByStudent_EmailAndCourse_CourseId(String email, Long courseId);

    // For unenroll
    Optional<Enrollment> findByStudent_EmailAndCourse_CourseId(String email, Long courseId);

    Optional<Enrollment> findByStudent_UserIdAndCourse_CourseId
            (Long studentUserId, Long courseId);


    List<Enrollment> findByStudent_Email(String email);

    // Handy shortcuts:
    @Query("select e.course.courseId from Enrollment e where e.student.email = :email")
    Set<Long> findCourseIdsByStudentEmail(@Param("email") String email);

    @Query("select e.course.courseId from Enrollment e where e.student.email = :email")
    Set<Long> findEnrolledCourseIdsByEmail(@Param("email") String email);

    @Query("select e.student from Enrollment e where e.course.courseId = :courseId")
    List<User> findStudentsByCourseId(@Param("courseId") Long courseId);



}
