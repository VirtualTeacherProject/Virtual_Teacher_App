package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;

import java.util.List;

/**
 * SCRUM-34: additional course resources (links).
 *
 * Reading needs access to the course content (enrolled in a published course,
 * approved teacher, or admin). Adding and deleting is for approved teachers and admins.
 */
public interface CourseResourceService {

    /** The course's resources, oldest first. Throws AccessDeniedException without content access. */
    List<CourseResource> getResources(Long courseId, String userEmail);

    /** Whether the course has any resources. Reveals nothing about them, so it needs no access. */
    boolean hasResources(Long courseId);

    CourseResource addResource(Long courseId, String userEmail,
                               String title, String url, String description);

    void deleteResource(Long courseId, Long resourceId, String userEmail);
}
