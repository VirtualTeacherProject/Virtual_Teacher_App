package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(String status);  //

    List<Course> findByTitleContainingIgnoreCase(String title);

    List<Course> findByStatusAndTitleContainingIgnoreCase(String status, String title);

    //later if course list grows, switch to pageable
    // Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    long count();

}
