package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStatus(String status);  //

    long count();

}
