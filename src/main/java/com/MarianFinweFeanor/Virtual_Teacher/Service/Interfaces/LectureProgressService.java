package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureState;
import com.MarianFinweFeanor.Virtual_Teacher.Model.StudentProgress;

import java.util.List;

/**
 * SCRUM-51 part B: self-paced lecture completion.
 * A student marks a lecture as finished; a lecture with an assignment can be marked
 * only after the assignment is submitted and counts as completed only once graded.
 * In a course with lecturesInOrder, a lecture opens only when every earlier lecture
 * is finished. Course managers (approved teachers, admins) are never locked out.
 */
public interface LectureProgressService {

    /**
     * Marks the lecture as finished for the enrolled student. Marking again does nothing.
     *
     * @throws IllegalStateException when the student is not enrolled, the lecture is
     *                               locked, or its assignment has not been submitted
     */
    void markFinished(Long courseId, Long lectureId, String studentEmail);

    /** True when the course wants lectures in order and an earlier lecture is not finished. */
    boolean isLocked(Lecture lecture, String userEmail);

    /** @throws IllegalStateException when the lecture is locked for the user */
    void ensureUnlocked(Lecture lecture, String userEmail);

    /** The course's lectures in order, with the user's state for each (nothing finished for null). */
    List<LectureState> getLectureStates(Long courseId, String userEmail);

    /** One row per enrolled student, sorted by name. */
    List<StudentProgress> getCourseProgress(Long courseId);
}
