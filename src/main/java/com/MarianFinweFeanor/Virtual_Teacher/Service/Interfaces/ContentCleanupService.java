package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

/**
 * SCRUM-15 deletion policy, in one place.
 *
 * - A lecture owns its submissions and its comments.
 * - A course owns its lectures (and therefore their submissions and comments)
 *   and its enrollments. Deleting a course never deletes a user.
 * - A user owns only what they produced as a participant: their enrollments,
 *   their submissions and their comments. Deleting a user never deletes a
 *   course or a lecture. A user who still teaches courses cannot be deleted
 *   until those courses are reassigned (UserService.deleteUserById).
 *
 * These methods remove the dependent rows (and uploaded submission files);
 * the calling service then deletes the lecture, course or user itself.
 */
public interface ContentCleanupService {

    /** Submissions and comments of one lecture. */
    void deleteLectureContent(Long lectureId);

    /** Submissions and comments of every lecture in the course. */
    void deleteCourseContent(Long courseId);

    /** Submissions and comments written by the user. */
    void deleteUserContent(Long userId);
}
