package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {

    boolean existsByLecture_LectureIdAndStudent_Email(Long lectureId, String email);

    List<LectureProgress> findByStudent_EmailAndLecture_Course_CourseId(String email, Long courseId);

    List<LectureProgress> findByStudent_UserIdAndLecture_Course_CourseId(Long userId, Long courseId);

    /**
     * Lectures of the course the student has completed: marked as finished and,
     * when the lecture has an assignment, graded.
     */
    @Query("""
            select count(p) from LectureProgress p
            where p.student.userId = :studentUserId
              and p.lecture.course.courseId = :courseId
              and (p.lecture.assignmentRequired = false
                   or exists (select a from Assignment a
                              where a.lecture = p.lecture and a.student = p.student and a.grade is not null))
            """)
    long countCompleted(@Param("studentUserId") Long studentUserId, @Param("courseId") Long courseId);

    // SCRUM-15: everything that has to go with a lecture, a course or a user
    List<LectureProgress> findByLecture_LectureId(Long lectureId);
    List<LectureProgress> findByLecture_Course_CourseId(Long courseId);
    List<LectureProgress> findByStudent_UserId(Long userId);
}
