package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

        import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.data.jpa.repository.Query;
        import org.springframework.data.repository.query.Param;

        import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    // find all submissions for a given lecture by a given student email
    //List<Assignment> findByLecture_LectureIdAndStudent_Email(Long lectureId, String email);

    List<Assignment> findByLecture_LectureIdAndStudent_EmailOrderBySubmittedAtDesc(Long lectureId, String email);

    List<Assignment> findByStudent_EmailOrderBySubmittedAtDesc(String email);

    List<Assignment> findByLecture_LectureIdOrderBySubmittedAtDesc(Long lectureId);

    // SCRUM-15: everything that has to go with a lecture, a course or a user
    List<Assignment> findByLecture_LectureId(Long lectureId);
    List<Assignment> findByLecture_Course_CourseId(Long courseId);
    List<Assignment> findByStudent_UserId(Long userId);

    @Query("""
  select avg(a.grade)
  from Assignment a
  where a.student.userId = :studentUserId
    and a.lecture.course.courseId = :courseId
    and a.grade is not null
""")
    Double avgGradeForCourse(@Param("studentUserId") Long studentUserId,
                             @Param("courseId") Long courseId);

    // countGradedLectures was replaced by LectureProgressRepository.countCompleted (SCRUM-51 part B).



}

