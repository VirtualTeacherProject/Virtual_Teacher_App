package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseResourceRepository extends JpaRepository<CourseResource, Long> {

    List<CourseResource> findByCourse_CourseIdOrderByCreatedAtAsc(Long courseId);

    boolean existsByCourse_CourseId(Long courseId);

    // SCRUM-15: resources go with their course
    List<CourseResource> findByCourse_CourseId(Long courseId);
}
