package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;

/**
 * Single place for the "who may see what" rules of a course.
 *
 * userEmail is the logged-in user's email, or null for an anonymous visitor.
 */
public interface CourseAccessService {

    /**
     * Admins and approved teachers: may create courses and search students.
     * Also used for viewing other teachers' drafts and content (kept open in SCRUM-52).
     */
    boolean canManageCourses(String userEmail);

    /**
     * SCRUM-52: may manage this course (edit, publish, lectures, grading, attendance,
     * resources, progress): admins, or the approved teacher the course belongs to.
     */
    boolean canManageCourse(Course course, String userEmail);

    /** Published courses are visible to everyone; drafts only to course managers. */
    boolean canViewCourse(Course course, String userEmail);

    /**
     * Protected course content (lecture videos): course managers, or users
     * enrolled in a published course.
     */
    boolean canAccessCourseContent(Course course, String userEmail);

    /** Throws EntityNotFoundException when the course must stay hidden from this user. */
    void ensureCanViewCourse(Course course, String userEmail);

    /** SCRUM-52: throws AccessDeniedException unless canManageCourse. */
    void ensureCanManageCourse(Course course, String userEmail);
}
