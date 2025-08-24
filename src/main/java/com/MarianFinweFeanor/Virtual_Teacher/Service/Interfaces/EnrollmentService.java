package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;

import java.util.List;
import java.util.Set;

public interface EnrollmentService {

    void enroll(String userEmail, Long courseId);
    void unenroll(String userEmail, Long courseId);
    boolean isEnrolled(String userEmail, Long courseId);

    // for UI badges, etc.
    Set<Long> getEnrolledCourseIds(String userEmail);

    // the one causing the error:
    Set<Course> getEnrolledCourses(String userEmail);


    List<User> getStudentsInCourse(Long courseId);
}
