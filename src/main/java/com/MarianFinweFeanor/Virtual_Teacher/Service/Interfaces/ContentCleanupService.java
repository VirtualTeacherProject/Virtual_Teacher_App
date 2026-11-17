package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

/**
 * SCRUM-15 deletion policy, in one place.
 *
 * - A lecture owns its submissions, its comments and the notes written on it (SCRUM-20).
 * - A course owns its lectures (and therefore their submissions, comments and notes)
 *   and its enrollments. Deleting a course never deletes a user.
 * - A user owns only what they produced as a participant: their enrollments,
 *   their submissions, their comments and their notes. Deleting a user never deletes a
 *   course or a lecture. A user who still teaches courses cannot be deleted
 *   until those courses are reassigned (UserService.deleteUserById).
 *
 * These methods remove the dependent rows (and uploaded submission files);
 * the calling service then deletes the lecture, course or user itself.
 */
public interface ContentCleanupService {

    /** Submissions, comments and notes of one lecture. */
    void deleteLectureContent(Long lectureId);

    /** Submissions, comments and notes of every lecture in the course. */
    void deleteCourseContent(Long courseId);

    /** Submissions, comments and notes written by the user. */
    void deleteUserContent(Long userId);
}
