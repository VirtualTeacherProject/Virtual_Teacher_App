package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ResourceRequestService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ResourceRequestServiceImpl implements ResourceRequestService {

    private final ResourceRequestRepository resourceRequestRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final CourseAccessService courseAccessService;

    @Autowired
    public ResourceRequestServiceImpl(ResourceRequestRepository resourceRequestRepository,
                                      CourseService courseService,
                                      UserService userService,
                                      CourseAccessService courseAccessService) {
        this.resourceRequestRepository = resourceRequestRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.courseAccessService = courseAccessService;
    }

    @Override
    public ResourceRequest createRequest(Long courseId, String studentEmail, String message) {
        // Browsers send line breaks as CRLF; store and count them as one character.
        String text = message == null ? "" : message.replace("\r\n", "\n").trim();

        if (text.length() < ResourceRequest.MIN_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Describe the resource you need in at least "
                    + ResourceRequest.MIN_MESSAGE_LENGTH + " characters");
        }
        if (text.length() > ResourceRequest.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("A resource request cannot be longer than "
                    + ResourceRequest.MAX_MESSAGE_LENGTH + " characters");
        }

        Course course = course(courseId);
        User student = userService.findByEmail(studentEmail);

        // Teachers and admins add resources themselves (SCRUM-34).
        if (student.getRole() != UserRole.STUDENT) {
            throw new IllegalStateException("Only students can request resources");
        }
        // Enrolled in a published course: the same rule as for the resources themselves.
        if (!courseAccessService.canAccessCourseContent(course, studentEmail)) {
            throw new IllegalStateException("Enroll in this course to request resources");
        }

        ResourceRequest request = new ResourceRequest();
        request.setCourse(course);
        request.setStudent(student);
        request.setMessage(text);
        request.setStatus(ResourceRequest.STATUS_OPEN);
        request.setCreatedAt(LocalDateTime.now());
        return resourceRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequest> getRequestsForCourse(Long courseId, String userEmail) {
        if (!courseAccessService.canManageCourses(userEmail)) {
            throw new AccessDeniedException("Only teachers and admins can see all resource requests");
        }
        return resourceRequestRepository.findByCourse_CourseIdOrderByCreatedAtDesc(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequest> getOwnRequests(Long courseId, String userEmail) {
        if (userEmail == null) {
            return List.of();
        }
        return resourceRequestRepository
                .findByCourse_CourseIdAndStudent_EmailOrderByCreatedAtDesc(courseId, userEmail);
    }

    private Course course(Long courseId) {
        return courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
    }
}
