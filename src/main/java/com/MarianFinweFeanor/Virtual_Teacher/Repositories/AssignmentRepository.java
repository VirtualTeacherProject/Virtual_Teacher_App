package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

        import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
        import org.springframework.data.jpa.repository.JpaRepository;

        import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    // find all submissions for a given lecture by a given student email
    //List<Assignment> findByLecture_LectureIdAndStudent_Email(Long lectureId, String email);

    List<Assignment> findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(Long lectureId, String email);

    List<Assignment> findByStudent_EmailOrderBySubmittedAtDesc(String email);


}

