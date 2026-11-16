package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;

/**
 * Single place for the "who may see what" rules of a course.
 *
 * userEmail is the logged-in user's email, or null for an anonymous visitor.
 */
public interface CourseAccessService {

    /** Admins and approved teachers. */
    boolean canManageCourses(String userEmail);

    /** Published courses are visible to everyone; drafts only to course managers. */
    boolean canViewCourse(Course course, String userEmail);

    /**
     * Protected course content (lecture videos): course managers, or users
     * enrolled in a published course.
     */
    boolean canAccessCourseContent(Course course, String userEmail);

    /** Throws EntityNotFoundException when the course must stay hidden from this user. */
    void ensureCanViewCourse(Course course, String userEmail);
}
