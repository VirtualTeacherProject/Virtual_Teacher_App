package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface LectureCommentRepository extends JpaRepository<LectureComment, Long> {

    List<LectureComment> findByLecture_LectureIdOrderByCreatedAtAsc(Long lectureId1);

    // SCRUM-15: everything that has to go with a lecture, a course or a user
    List<LectureComment> findByLecture_LectureId(Long lectureId);
    List<LectureComment> findByLecture_Course_CourseId(Long courseId);
    List<LectureComment> findByAuthor_UserId(Long userId);
}
