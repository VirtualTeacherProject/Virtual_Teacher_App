package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    // either...
    List<Lecture> findAllByCourse(Course course);
    // …or, if you prefer ID-based lookup…
    // List<Lecture> findByCourse_CourseId(Long courseId);


    @Query("select count(l) from Lecture l where l.course.courseId = :courseId")
    long countLecturesByCourseId(@Param("courseId") Long courseId);



}