package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite;

import java.util.List;

/**
 * SCRUM-48: prerequisite courses. A required prerequisite must be a published
 * course with the same topic and must be completed (enrolment COMPLETED, whatever
 * the grade) before a student can enrol. A recommended prerequisite can be any
 * published course and never blocks. Managed by the course's own teacher or an admin.
 */
public interface CoursePrerequisiteService {

    /** The course's prerequisites, required first, then by title. */
    List<CoursePrerequisite> getPrerequisites(Long courseId);

    /**
     * Published courses that could still be added as a prerequisite of the course
     * (not the course itself, not already linked), by title.
     */
    List<Course> getCandidates(Long courseId);

    /**
     * @throws IllegalArgumentException for the course itself, a draft, a course already
     *         linked, a required course with another topic, or a required link that
     *         would make the two courses require each other
     * @throws org.springframework.security.access.AccessDeniedException unless the
     *         user manages the course
     */
    CoursePrerequisite addPrerequisite(Long courseId, Long prerequisiteCourseId, boolean required,
                                       String managerEmail);

    void removePrerequisite(Long courseId, Long prerequisiteCourseId, String managerEmail);

    /** Required prerequisite courses the user has not completed yet; all of them for null. */
    List<Course> getMissingRequired(Long courseId, String userEmail);

    /**
     * The enrolment check (also the place for future rules such as SCRUM-16's
     * entrance exam).
     *
     * @throws IllegalStateException naming the required courses still to complete
     */
    void ensurePrerequisitesMet(String userEmail, Long courseId);
}
