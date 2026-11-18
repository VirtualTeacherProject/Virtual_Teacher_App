package com.MarianFinweFeanor.Virtual_Teacher;

import com.MarianFinweFeanor.Virtual_Teacher.Controller.EnrollmentMvcController;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CourseResource;
import com.MarianFinweFeanor.Virtual_Teacher.Model.ResourceRequest;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseResourceService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ResourceRequestService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Renders the public course page: reviews (SCRUM-31), additional resources (SCRUM-34)
 * and resource requests (SCRUM-35, SCRUM-36).
 */
@ActiveProfiles("test")
@WebMvcTest(EnrollmentMvcController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseDetailPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private CourseService courseService;
    @MockBean private EnrollmentRepository enrollmentRepository;
    @MockBean private EnrollmentService enrollmentService;
    @MockBean private UserRepository userRepository;
    @MockBean private CourseAccessService courseAccessService;
    @MockBean private CourseResourceService courseResourceService;
    @MockBean private ResourceRequestService resourceRequestService;

    private Course course;

    @BeforeEach
    void setUp() {
        User teacher = new User();
        teacher.setFirstName("Tina");
        teacher.setLastName("Teacher");
        teacher.setEmail("tina@test.com");
        teacher.setRole(UserRole.TEACHER);

        course = new Course();
        course.setCourseId(10L);
        course.setTitle("Java Basics");
        course.setTopic("Programming");
        course.setDescription("Learn Java");
        course.setStatus("PUBLISHED");
        course.setTeacher(teacher);

        given(courseService.getCourseById(10L)).willReturn(Optional.of(course));
    }

    private Enrollment review(String first, String last, int rating, String text) {
        User student = new User();
        student.setFirstName(first);
        student.setLastName(last);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setRating(rating);
        enrollment.setReviewText(text);
        enrollment.setRatedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        return enrollment;
    }

    @Test
    void courseDetail_shouldShowReviewsAndAverage_toAnonymousVisitor() throws Exception {
        given(enrollmentService.getCourseReviews(10L)).willReturn(List.of(
                review("Anna", "Petrova", 5, "Clear explanations and useful assignments."),
                review("Boris", "Annenkov", 4, "Good pace, would recommend.")));
        given(enrollmentService.getAverageRating(10L)).willReturn(4.5);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("course-detail"))
                .andExpect(content().string(containsString("4.5 / 5 (2)")))
                .andExpect(content().string(containsString("Anna Petrova")))
                .andExpect(content().string(containsString("Clear explanations and useful assignments.")))
                .andExpect(content().string(containsString("2026-11-01")));
    }

    @Test
    void courseDetail_shouldSayNoReviewsYet_whenCourseHasNoRatings() throws Exception {
        given(enrollmentService.getCourseReviews(10L)).willReturn(List.of());
        given(enrollmentService.getAverageRating(10L)).willReturn(null);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No reviews yet.")));
    }

    // --- SCRUM-34: additional resources --------------------------------------

    private static final String STUDENT = "student@test.com";
    private static final String TEACHER = "tina@test.com";
    private static final String RESOURCE_URL = "https://example.com/week-1-slides";

    private CourseResource resource() {
        CourseResource resource = new CourseResource();
        resource.setResourceId(7L);
        resource.setCourse(course);
        resource.setTitle("Week 1 slides");
        resource.setUrl(RESOURCE_URL);
        resource.setDescription("Read before the first lecture");
        return resource;
    }

    private User user(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(role);
        user.setTeacherApproved(true);
        return user;
    }

    private Principal loggedIn(String email, UserRole role) {
        User user = user(email, role);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userService.findByEmail(email)).willReturn(user);
        given(userService.getEnrolledCourses(email)).willReturn(Set.of());
        return new TestingAuthenticationToken(email, "n/a", "ROLE_" + role.name());
    }

    @Test
    void courseDetail_shouldHideResources_fromAnonymousVisitor() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, null)).willReturn(false);
        given(courseResourceService.hasResources(10L)).willReturn(true);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"course-resources-locked\"")))
                .andExpect(content().string(not(containsString("id=\"course-resources\""))))
                .andExpect(content().string(not(containsString(RESOURCE_URL))));

        verify(courseResourceService, never()).getResources(any(), any());
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldHideResources_fromStudentWhoIsNotEnrolled() throws Exception {
        Principal principal = loggedIn(STUDENT, UserRole.STUDENT);
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(false);
        given(courseResourceService.hasResources(10L)).willReturn(true);

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"course-resources-locked\"")))
                .andExpect(content().string(not(containsString(RESOURCE_URL))));

        verify(courseResourceService, never()).getResources(any(), any());
    }

    @Test
    void courseDetail_shouldNotShowLockedNotice_whenCourseHasNoResources() throws Exception {
        given(courseAccessService.canAccessCourseContent(course, null)).willReturn(false);
        given(courseResourceService.hasResources(10L)).willReturn(false);

        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("course-resources"))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldShowResources_toEnrolledStudent_withoutManagementControls() throws Exception {
        Principal principal = loggedIn(STUDENT, UserRole.STUDENT);
        given(userService.getEnrolledCourses(STUDENT)).willReturn(Set.of(course));
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(true);
        given(courseResourceService.hasResources(10L)).willReturn(true);
        given(courseResourceService.getResources(10L, STUDENT)).willReturn(List.of(resource()));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"course-resources\"")))
                .andExpect(content().string(containsString("href=\"" + RESOURCE_URL + "\"")))
                .andExpect(content().string(containsString("Week 1 slides")))
                .andExpect(content().string(containsString("Read before the first lecture")))
                .andExpect(content().string(not(containsString("course-resources-locked"))))
                .andExpect(content().string(not(containsString("add-resource-form"))))
                .andExpect(content().string(not(containsString("/resources/7/delete"))));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldShowResourcesAndManagementControls_toApprovedTeacher() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
        given(courseResourceService.getResources(10L, TEACHER)).willReturn(List.of(resource()));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"" + RESOURCE_URL + "\"")))
                .andExpect(content().string(containsString("id=\"add-resource-form\"")))
                .andExpect(content().string(containsString("/courses/10/resources/7/delete")));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldSayNoResourcesYet_andOfferTheForm_toTeacherOfEmptyCourse() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
        given(courseResourceService.getResources(10L, TEACHER)).willReturn(List.of());

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No additional resources yet.")))
                .andExpect(content().string(containsString("id=\"add-resource-form\"")));
    }

    // --- SCRUM-35: resource requests -----------------------------------------

    private ResourceRequest request(String firstName, String message) {
        User author = new User();
        author.setFirstName(firstName);
        author.setLastName("Student");

        ResourceRequest request = new ResourceRequest();
        request.setRequestId(3L);
        request.setCourse(course);
        request.setStudent(author);
        request.setMessage(message);
        request.setStatus(ResourceRequest.STATUS_OPEN);
        request.setCreatedAt(LocalDateTime.of(2026, 11, 17, 9, 30));
        return request;
    }

    private Principal enrolledStudent() {
        Principal principal = loggedIn(STUDENT, UserRole.STUDENT);
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        given(enrollmentRepository.findByStudent_EmailAndCourse_CourseId(STUDENT, 10L))
                .willReturn(Optional.of(enrollment));
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(true);
        return principal;
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldOfferRequestFormAndOwnRequests_toEnrolledStudent() throws Exception {
        Principal principal = enrolledStudent();
        given(resourceRequestService.getOwnRequests(10L, STUDENT))
                .willReturn(List.of(request("Anna", "Please add more <b>practice</b> exercises.")));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canRequestResources", true))
                .andExpect(content().string(containsString("id=\"resource-request-section\"")))
                .andExpect(content().string(containsString("minlength=\"100\"")))
                .andExpect(content().string(containsString("id=\"own-resource-requests\"")))
                // plain text: markup in a request is escaped
                .andExpect(content().string(containsString("Please add more &lt;b&gt;practice&lt;/b&gt; exercises.")))
                .andExpect(content().string(not(containsString("id=\"course-resource-requests\""))));

        verify(resourceRequestService, never()).getRequestsForCourse(any(), any());
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldKeepTheDraft_afterAFailedRequest() throws Exception {
        Principal principal = enrolledStudent();

        mockMvc.perform(get("/courses/10").principal(principal).flashAttr("requestDraft", "My unfinished request"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My unfinished request</textarea>")));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldNotOfferRequestForm_toStudentWhoIsNotEnrolled() throws Exception {
        Principal principal = loggedIn(STUDENT, UserRole.STUDENT);
        given(courseAccessService.canAccessCourseContent(course, STUDENT)).willReturn(false);

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canRequestResources", false))
                .andExpect(content().string(not(containsString("resource-request-section"))));

        verify(resourceRequestService, never()).getOwnRequests(any(), any());
        verify(resourceRequestService, never()).getRequestsForCourse(any(), any());
    }

    @Test
    void courseDetail_shouldShowNoRequests_toAnonymousVisitor() throws Exception {
        mockMvc.perform(get("/courses/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("resource-request"))));

        verify(resourceRequestService, never()).getOwnRequests(any(), any());
        verify(resourceRequestService, never()).getRequestsForCourse(any(), any());
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldShowAllRequests_toTeacher_withoutTheRequestForm() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);
        given(resourceRequestService.getRequestsForCourse(10L, TEACHER)).willReturn(List.of(
                request("Anna", "Slides for week two, please."),
                request("Boris", "A reading list on recursion.")));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"course-resource-requests\"")))
                .andExpect(content().string(containsString("Anna Student")))
                .andExpect(content().string(containsString("Slides for week two, please.")))
                .andExpect(content().string(containsString("A reading list on recursion.")))
                .andExpect(content().string(containsString("2026-11-17 09:30")))
                .andExpect(content().string(not(containsString("resource-request-section"))));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldSayNoRequestsYet_toTeacher() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);
        given(resourceRequestService.getRequestsForCourse(10L, TEACHER)).willReturn(List.of());

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No requests yet.")));
    }

    // --- SCRUM-36: assigning requests and answering them ---------------------

    private User student(Long id, String firstName) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(firstName);
        user.setLastName("Student");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private ResourceRequest assignedRequest(String status, String response) {
        ResourceRequest request = request("Anna", "Extra C++ material, please.");
        request.getStudent().setUserId(1L);
        request.setAssignee(student(2L, "Boris"));
        request.setStatus(status);
        request.setResponse(response);
        if (response != null) {
            request.setRespondedAt(LocalDateTime.of(2026, 11, 18, 14, 0));
        }
        return request;
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldShowAssignedRequestWithAnswerForm_toAssignedStudent() throws Exception {
        Principal principal = enrolledStudent();
        given(resourceRequestService.getAssignedRequests(10L, STUDENT))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_IN_PROGRESS, null)));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"assigned-resource-requests\"")))
                .andExpect(content().string(containsString("Anna Student")))
                .andExpect(content().string(containsString("Extra C++ material, please.")))
                .andExpect(content().string(containsString("IN PROGRESS")))
                .andExpect(content().string(containsString("/courses/10/resource-requests/3/response")))
                .andExpect(content().string(containsString("maxlength=\"2000\"")))
                .andExpect(content().string(containsString("Send answer")))
                // no management controls for a student
                .andExpect(content().string(not(containsString("/resource-requests/3/assign"))))
                .andExpect(content().string(not(containsString("/resource-requests/3/status"))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldPrefillTheAnswer_andOfferUpdate_whenAlreadyAnswered() throws Exception {
        Principal principal = enrolledStudent();
        given(resourceRequestService.getAssignedRequests(10L, STUDENT))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_IN_PROGRESS, "See cppreference.com")));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("See cppreference.com</textarea>")))
                .andExpect(content().string(containsString("Update answer")));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldKeepTheAnswerDraft_afterAFailedAnswer() throws Exception {
        Principal principal = enrolledStudent();
        given(resourceRequestService.getAssignedRequests(10L, STUDENT))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_IN_PROGRESS, "Saved answer")));

        mockMvc.perform(get("/courses/10").principal(principal)
                        .flashAttr("responseDraftId", 3L)
                        .flashAttr("responseDraft", "My unfinished answer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My unfinished answer</textarea>")));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldShowAnswerWithoutForm_whenAssignedRequestIsClosed() throws Exception {
        Principal principal = enrolledStudent();
        given(resourceRequestService.getAssignedRequests(10L, STUDENT))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_RESOLVED, "See cppreference.com")));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your answer")))
                .andExpect(content().string(containsString("See cppreference.com")))
                .andExpect(content().string(not(containsString("/resource-requests/3/response"))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldNotShowAssignedSection_toStudentWithoutAssignments() throws Exception {
        Principal principal = enrolledStudent();

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("assigned-resource-requests"))));
    }

    @Test
    @WithMockUser(username = STUDENT, roles = "STUDENT")
    void courseDetail_shouldShowAssigneeAndAnswer_toRequestAuthor() throws Exception {
        Principal principal = enrolledStudent();
        given(resourceRequestService.getOwnRequests(10L, STUDENT))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_IN_PROGRESS, "See <i>cppreference</i>")));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Assigned to")))
                .andExpect(content().string(containsString("Boris Student")))
                .andExpect(content().string(containsString("own-request-response")))
                // plain text: markup in an answer is escaped
                .andExpect(content().string(containsString("See &lt;i&gt;cppreference&lt;/i&gt;")));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldShowStatusAndAssignControlsAndAnswer_toTeacher() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);
        given(resourceRequestService.getRequestsForCourse(10L, TEACHER))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_IN_PROGRESS, "See cppreference.com")));
        User enrolledTeacher = user("other-teacher@test.com", UserRole.TEACHER);
        enrolledTeacher.setUserId(4L);
        enrolledTeacher.setFirstName("Enrolled");
        enrolledTeacher.setLastName("Teacher");
        given(enrollmentService.getStudentsInCourse(10L)).willReturn(List.of(
                student(1L, "Anna"), student(2L, "Boris"), student(5L, "Carla"), enrolledTeacher));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/courses/10/resource-requests/3/status")))
                .andExpect(content().string(containsString("/courses/10/resource-requests/3/assign")))
                .andExpect(content().string(containsString("<option value=\"RESOLVED\">RESOLVED</option>")))
                .andExpect(content().string(containsString("<option value=\"IN_PROGRESS\" selected=\"selected\">IN PROGRESS</option>")))
                // the current assignee is selected; the author and enrolled teachers are not offered
                .andExpect(content().string(containsString("<option value=\"2\" selected=\"selected\">Boris Student</option>")))
                .andExpect(content().string(containsString("<option value=\"5\">Carla Student</option>")))
                .andExpect(content().string(not(containsString("<option value=\"1\">"))))
                .andExpect(content().string(not(containsString("Enrolled Teacher"))))
                .andExpect(content().string(containsString("See cppreference.com")))
                .andExpect(content().string(containsString("2026-11-18 14:00")));
    }

    @Test
    @WithMockUser(username = TEACHER, roles = "TEACHER")
    void courseDetail_shouldNotOfferAssigning_forClosedRequest() throws Exception {
        Principal principal = loggedIn(TEACHER, UserRole.TEACHER);
        given(courseAccessService.canAccessCourseContent(course, TEACHER)).willReturn(true);
        given(courseAccessService.canManageCourses(TEACHER)).willReturn(true);
        given(resourceRequestService.getRequestsForCourse(10L, TEACHER))
                .willReturn(List.of(assignedRequest(ResourceRequest.STATUS_IRRELEVANT, null)));

        mockMvc.perform(get("/courses/10").principal(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/courses/10/resource-requests/3/status")))
                .andExpect(content().string(not(containsString("/resource-requests/3/assign"))));
    }
}
