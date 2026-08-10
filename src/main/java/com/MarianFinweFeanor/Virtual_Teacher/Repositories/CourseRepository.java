package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(String status);

    List<Course> findByTitleContainingIgnoreCase(String title);

    List<Course> findByStatusAndTitleContainingIgnoreCase(String status, String title);

    List<Course> findByTeacher_Email(String email);

    List<Course> findByTeacher_EmailAndTitleContainingIgnoreCase(String email, String title);

    List<Course> findByStatusOrTeacher_Email(String status, String email);

    List<Course> findByStatusAndTitleContainingIgnoreCaseOrTeacher_EmailAndTitleContainingIgnoreCase(
            String status,
            String title,
            String email,
            String teacherTitle
    );

    //later if course list grows, switch to pageable
    // Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    long count();
}



