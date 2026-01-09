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

    @Query("""
  select avg(a.grade)
  from Assignment a
  where a.student.userId = :studentUserId
    and a.lecture.course.courseId = :courseId
    and a.grade is not null
""")
    Double avgGradeForCourse(@Param("studentUserId") Long studentUserId,
                             @Param("courseId") Long courseId);

    @Query("""
  select count(distinct a.lecture.lectureId)
  from Assignment a
  where a.student.userId = :studentUserId
    and a.lecture.course.courseId = :courseId
    and a.grade is not null
""")
    long countGradedLectures(@Param("studentUserId") Long studentUserId,
                             @Param("courseId") Long courseId);



}

