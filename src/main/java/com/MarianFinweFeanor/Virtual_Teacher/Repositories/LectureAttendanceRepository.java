package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureAttendanceRepository extends JpaRepository<LectureAttendance, Long> {

    List<LectureAttendance> findByLecture_LectureId(Long lectureId);

    // SCRUM-15: everything that has to go with a lecture, a course or a user
    List<LectureAttendance> findByLecture_Course_CourseId(Long courseId);
    List<LectureAttendance> findByStudent_UserId(Long userId);

    // SCRUM-29: a student's own records in one course
    List<LectureAttendance> findByStudent_UserIdAndLecture_Course_CourseId(Long userId, Long courseId);
}
