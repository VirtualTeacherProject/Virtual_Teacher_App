package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureNoteRepository extends JpaRepository<LectureNote, Long> {

    Optional<LectureNote> findByLecture_LectureIdAndUser_Email(Long lectureId, String email);

    // SCRUM-15: everything that has to go with a lecture, a course or a user
    List<LectureNote> findByLecture_LectureId(Long lectureId);
    List<LectureNote> findByLecture_Course_CourseId(Long courseId);
    List<LectureNote> findByUser_UserId(Long userId);
}
