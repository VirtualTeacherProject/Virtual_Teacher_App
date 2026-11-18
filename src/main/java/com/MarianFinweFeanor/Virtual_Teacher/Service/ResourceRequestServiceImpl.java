package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.ResourceRequestRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
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
    private final EnrollmentService enrollmentService;

    @Autowired
    public ResourceRequestServiceImpl(ResourceRequestRepository resourceRequestRepository,
                                      CourseService courseService,
                                      UserService userService,
                                      CourseAccessService courseAccessService,
                                      EnrollmentService enrollmentService) {
        this.resourceRequestRepository = resourceRequestRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.courseAccessService = courseAccessService;
        this.enrollmentService = enrollmentService;
    }

    @Override
    public ResourceRequest createRequest(Long courseId, String studentEmail, String message) {
        String text = clean(message);

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

    @Override
    @Transactional(readOnly = true)
    public List<ResourceRequest> getAssignedRequests(Long courseId, String userEmail) {
        if (userEmail == null) {
            return List.of();
        }
        return resourceRequestRepository
                .findByCourse_CourseIdAndAssignee_EmailOrderByCreatedAtDesc(courseId, userEmail);
    }

    @Override
    public ResourceRequest updateStatus(Long courseId, Long requestId, String status, String userEmail) {
        userService.ensureApprovedTeacher(userEmail);
        if (status == null || !ResourceRequest.STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unknown request status");
        }

        ResourceRequest request = request(courseId, requestId);
        request.setStatus(status);
        return resourceRequestRepository.save(request);
    }

    @Override
    public ResourceRequest assign(Long courseId, Long requestId, Long assigneeId, String userEmail) {
        userService.ensureApprovedTeacher(userEmail);
        ResourceRequest request = request(courseId, requestId);

        if (request.isClosed()) {
            throw new IllegalStateException("Reopen the request before assigning it");
        }

        if (assigneeId == null) {
            request.clearAssignment();
            return resourceRequestRepository.save(request);
        }

        User assignee = userService.getUserById(assigneeId)
                .orElseThrow(() -> new EntityNotFoundException("User", assigneeId));
        if (assignee.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can be assigned to a request");
        }
        if (assignee.getUserId().equals(request.getStudent().getUserId())) {
            throw new IllegalArgumentException("Assign the request to a student other than its author");
        }
        if (!enrollmentService.isEnrolled(assignee.getEmail(), courseId)) {
            throw new IllegalArgumentException("The student must be enrolled in this course");
        }

        boolean sameAssignee = request.getAssignee() != null
                && request.getAssignee().getUserId().equals(assignee.getUserId());
        if (!sameAssignee) {
            // the previous assignee's answer goes with them
            request.setResponse(null);
            request.setRespondedAt(null);
        }
        request.setAssignee(assignee);
        if (ResourceRequest.STATUS_OPEN.equals(request.getStatus())) {
            request.setStatus(ResourceRequest.STATUS_IN_PROGRESS);
        }
        return resourceRequestRepository.save(request);
    }

    @Override
    public ResourceRequest respond(Long courseId, Long requestId, String studentEmail, String response) {
        String text = clean(response);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Your answer cannot be empty");
        }
        if (text.length() > ResourceRequest.MAX_RESPONSE_LENGTH) {
            throw new IllegalArgumentException("An answer cannot be longer than "
                    + ResourceRequest.MAX_RESPONSE_LENGTH + " characters");
        }

        ResourceRequest request = request(courseId, requestId);
        if (request.getAssignee() == null
                || studentEmail == null
                || !studentEmail.equalsIgnoreCase(request.getAssignee().getEmail())) {
            throw new AccessDeniedException("Only the assigned student can answer this request");
        }
        // still enrolled in the (published) course
        if (!courseAccessService.canAccessCourseContent(request.getCourse(), studentEmail)) {
            throw new AccessDeniedException("Enroll in this course to answer requests");
        }
        if (request.isClosed()) {
            throw new IllegalStateException("This request is closed");
        }

        request.setResponse(text);
        request.setRespondedAt(LocalDateTime.now());
        return resourceRequestRepository.save(request);
    }

    /** Browsers send line breaks as CRLF; store and count them as one character. */
    private static String clean(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").trim();
    }

    /** The request, only if it belongs to the course in the URL. */
    private ResourceRequest request(Long courseId, Long requestId) {
        return resourceRequestRepository.findById(requestId)
                .filter(r -> r.getCourse().getCourseId().equals(courseId))
                .orElseThrow(() -> new EntityNotFoundException("Resource request", requestId));
    }

    private Course course(Long courseId) {
        return courseService.getCourseById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
    }
}
