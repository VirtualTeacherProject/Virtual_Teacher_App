package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface LectureCommentRepository extends JpaRepository<LectureComment, Long> {

    List<LectureComment> findByLecture_LectureIdOrderByCreatedAtAsc(Long lectureId1);
}
