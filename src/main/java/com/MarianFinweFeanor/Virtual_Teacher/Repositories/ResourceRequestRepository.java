package com.MarianFinweFeanor.Virtual_Teacher.Repositories;

import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, Long> {

    List<ResourceRequest> findByCourse_CourseIdOrderByCreatedAtDesc(Long courseId);

    List<ResourceRequest> findByCourse_CourseIdAndStudent_EmailOrderByCreatedAtDesc(
            Long courseId, String studentEmail);

    // SCRUM-36: requests a teacher assigned to a student
    List<ResourceRequest> findByCourse_CourseIdAndAssignee_EmailOrderByCreatedAtDesc(
            Long courseId, String assigneeEmail);

    // SCRUM-15: requests go with their course and with the student who wrote them
    List<ResourceRequest> findByCourse_CourseId(Long courseId);
    List<ResourceRequest> findByStudent_UserId(Long userId);
    List<ResourceRequest> findByAssignee_UserId(Long userId);
}
