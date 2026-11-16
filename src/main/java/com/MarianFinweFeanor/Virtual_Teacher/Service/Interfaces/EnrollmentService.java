package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
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

    void recalculateProgress(Long studentUserId, Long courseId);

    /** Recalculates every enrollment of the course, e.g. after a lecture was deleted. */
    void recalculateProgressForCourse(Long courseId);

    void ensureCanRateCourse(Long studentUserId, Long courseId);

    void ensureStudentEnrolled(String studentEmail, Long courseId);

    /** A review must have at least this many characters (SCRUM-31). */
    int MIN_REVIEW_LENGTH = 75;

    /** Upper limit, matching the review_text column. */
    int MAX_REVIEW_LENGTH = 1000;

    /**
     * Saves (or replaces) the student's rating of a course.
     * Throws IllegalArgumentException when the rating is not 1-5 or the review is
     * shorter than MIN_REVIEW_LENGTH, and IllegalStateException when the student
     * is not yet allowed to rate (see ensureCanRateCourse).
     */
    void rateCourse(String studentEmail, Long courseId, Integer rating, String reviewText);

    /** Enrollments of the course that carry a rating, newest first. */
    List<Enrollment> getCourseReviews(Long courseId);

    /** Average rating of the course, or null when nobody has rated it yet. */
    Double getAverageRating(Long courseId);


}
