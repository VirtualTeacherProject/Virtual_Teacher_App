package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseResourceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseResourceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CourseResourceServiceImpl implements CourseResourceService {

    private final CourseResourceRepository courseResourceRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final CourseAccessService courseAccessService;

    @Autowired
    public CourseResourceServiceImpl(CourseResourceRepository courseResourceRepository,
                                     CourseService courseService,
                                     UserService userService,
                                     CourseAccessService courseAccessService) {
        this.courseResourceRepository = courseResourceRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.courseAccessService = courseAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResource> getResources(Long courseId, String userEmail) {
        Course course = course(courseId);

        if (!courseAccessService.canAccessCourseContent(course, userEmail)) {
            throw new AccessDeniedException("Enroll in this course to see its resources");
        }

        return courseResourceRepository.findByCourse_CourseIdOrderByCreatedAtAsc(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasResources(Long courseId) {
        return courseResourceRepository.existsByCourse_CourseId(courseId);
    }

    @Override
    public CourseResource addResource(Long courseId, String userEmail,
                                      String title, String url, String description) {
        userService.ensureApprovedTeacher(userEmail);

        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("Resource title cannot be empty");
        }
        if (cleanTitle.length() > CourseResource.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Resource title cannot be longer than "
                    + CourseResource.MAX_TITLE_LENGTH + " characters");
        }

        String cleanUrl = url == null ? "" : url.trim();
        if (cleanUrl.length() > CourseResource.MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Resource link cannot be longer than "
                    + CourseResource.MAX_URL_LENGTH + " characters");
        }
        if (!isWebLink(cleanUrl)) {
            throw new IllegalArgumentException(
                    "Resource link must be a web address starting with http:// or https://");
        }

        String cleanDescription = description == null || description.isBlank()
                ? null : description.trim();
        if (cleanDescription != null
                && cleanDescription.length() > CourseResource.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Resource description cannot be longer than "
                    + CourseResource.MAX_DESCRIPTION_LENGTH + " characters");
        }

        // SCRUM-52: only the course's own teacher or an admin
        Course course = course(courseId);
        courseAccessService.ensureCanManageCourse(course, userEmail);

        CourseResource resource = new CourseResource();
        resource.setCourse(course);
        resource.setTitle(cleanTitle);
        resource.setUrl(cleanUrl);
        resource.setDescription(cleanDescription);
        resource.setCreatedAt(LocalDateTime.now());
        return courseResourceRepository.save(resource);
    }

    @Override
    public void deleteResource(Long courseId, Long resourceId, String userEmail) {
        userService.ensureApprovedTeacher(userEmail);

        CourseResource resource = courseResourceRepository.findById(resourceId)
                // a resource of another course is treated as missing
                .filter(found -> found.getCourse() != null
                        && courseId.equals(found.getCourse().getCourseId()))
                .orElseThrow(() -> new EntityNotFoundException("Resource", resourceId));
        courseAccessService.ensureCanManageCourse(resource.getCourse(), userEmail);   // SCRUM-52

        courseResourceRepository.delete(resource);
    }

    private Course course(Long courseId) {
        return courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
    }

    /** Only http(s) links with a host: the link is rendered as an href, so no javascript: etc. */
    private static boolean isWebLink(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
