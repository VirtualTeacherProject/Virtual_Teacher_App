package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** SCRUM-48 */
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    List<CoursePrerequisite> findByCourse_CourseId(Long courseId);

    List<CoursePrerequisite> findByCourse_CourseIdAndRequiredTrue(Long courseId);

    Optional<CoursePrerequisite> findByCourse_CourseIdAndPrerequisiteCourse_CourseId(Long courseId,
                                                                                    Long prerequisiteCourseId);

    // SCRUM-15: links on either side go with a deleted course
    List<CoursePrerequisite> findByPrerequisiteCourse_CourseId(Long prerequisiteCourseId);
}
