package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.CoursePrerequisite;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CoursePrerequisiteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SCRUM-48: required (same topic, must be completed) and recommended prerequisites. */
@ExtendWith(MockitoExtension.class)
class CoursePrerequisiteServiceImplTest {

    private static final String TEACHER = "teacher@test.com";
    private static final String STUDENT = "student@test.com";

    @Mock private CoursePrerequisiteRepository prerequisiteRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseAccessService courseAccessService;

    @InjectMocks
    private CoursePrerequisiteServiceImpl service;

    private Course advanced;    // id 1, Java
    private Course basics;      // id 2, Java
    private Course math;        // id 3, Math

    @BeforeEach
    void setUp() {
        advanced = course(1L, "Advanced Java", "Java", "PUBLISHED");
        basics = course(2L, "Java basics", " java ", "PUBLISHED");
        math = course(3L, "Discrete math", "Math", "PUBLISHED");
    }

    private static Course course(Long id, String title, String topic, String status) {
        Course course = new Course();
        course.setCourseId(id);
        course.setTitle(title);
        course.setTopic(topic);
        course.setStatus(status);
        return course;
    }

    private static CoursePrerequisite link(Course course, Course prerequisite, boolean required) {
        CoursePrerequisite link = new CoursePrerequisite();
        link.setCourse(course);
        link.setPrerequisiteCourse(prerequisite);
        link.setRequired(required);
        return link;
    }

    private static Enrollment enrollment(Enrollment.CompletionStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCompletionStatus(status);
        return enrollment;
    }

    private void managing(Course course) {
        when(courseRepository.findById(course.getCourseId())).thenReturn(Optional.of(course));
    }

    private CoursePrerequisite saved() {
        ArgumentCaptor<CoursePrerequisite> captor = ArgumentCaptor.forClass(CoursePrerequisite.class);
        verify(prerequisiteRepository).save(captor.capture());
        return captor.getValue();
    }

    // --- reading ---------------------------------------------------------------

    @Test
    void getPrerequisites_shouldListRequiredFirst_thenByTitle() {
        Course zeta = course(4L, "Zeta", "Java", "PUBLISHED");
        when(prerequisiteRepository.findByCourse_CourseId(1L)).thenReturn(List.of(
                link(advanced, math, false), link(advanced, zeta, true), link(advanced, basics, true)));

        List<CoursePrerequisite> result = service.getPrerequisites(1L);

        assertEquals(List.of(basics, zeta, math), result.stream().map(CoursePrerequisite::getPrerequisiteCourse).toList());
    }

    @Test
    void getCandidates_shouldOfferPublishedCourses_exceptItselfAndLinkedOnes() {
        Course other = course(4L, "Algorithms", "CS", "PUBLISHED");
        when(prerequisiteRepository.findByCourse_CourseId(1L)).thenReturn(List.of(link(advanced, basics, true)));
        when(courseRepository.findByStatus("PUBLISHED")).thenReturn(List.of(advanced, basics, math, other));

        assertEquals(List.of(other, math), service.getCandidates(1L));
    }

    // --- adding ----------------------------------------------------------------

    @Test
    void addPrerequisite_shouldAddRequiredCourse_withTheSameTopic_ignoringCaseAndSpaces() {
        managing(advanced);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(basics));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.empty());
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(2L)).thenReturn(List.of());

        service.addPrerequisite(1L, 2L, true, TEACHER);

        CoursePrerequisite link = saved();
        assertSame(advanced, link.getCourse());
        assertSame(basics, link.getPrerequisiteCourse());
        assertTrue(link.isRequired());
        verify(courseAccessService).ensureCanManageCourse(advanced, TEACHER);
    }

    @Test
    void addPrerequisite_shouldAddRecommendedCourse_fromAnotherTopic() {
        managing(advanced);
        when(courseRepository.findById(3L)).thenReturn(Optional.of(math));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 3L))
                .thenReturn(Optional.empty());

        service.addPrerequisite(1L, 3L, false, TEACHER);

        assertFalse(saved().isRequired());
        verify(prerequisiteRepository, never()).findByCourse_CourseIdAndRequiredTrue(any());
    }

    @Test
    void addPrerequisite_shouldRefuseRequiredCourse_fromAnotherTopic() {
        managing(advanced);
        when(courseRepository.findById(3L)).thenReturn(Optional.of(math));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 3L))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addPrerequisite(1L, 3L, true, TEACHER));
        assertEquals(CoursePrerequisiteServiceImpl.OTHER_TOPIC, ex.getMessage());
        verify(prerequisiteRepository, never()).save(any());
    }

    @Test
    void addPrerequisite_shouldRefuseTheCourseItself() {
        managing(advanced);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addPrerequisite(1L, 1L, false, TEACHER));
        assertEquals(CoursePrerequisiteServiceImpl.SELF, ex.getMessage());
    }

    @Test
    void addPrerequisite_shouldRefuseADraft() {
        managing(advanced);
        basics.setStatus("DRAFT");
        when(courseRepository.findById(2L)).thenReturn(Optional.of(basics));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addPrerequisite(1L, 2L, true, TEACHER));
        assertEquals(CoursePrerequisiteServiceImpl.NOT_PUBLISHED, ex.getMessage());
    }

    @Test
    void addPrerequisite_shouldRefuseACourseAlreadyLinked() {
        managing(advanced);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(basics));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.of(link(advanced, basics, false)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addPrerequisite(1L, 2L, true, TEACHER));
        assertEquals(CoursePrerequisiteServiceImpl.ALREADY_LINKED, ex.getMessage());
    }

    @Test
    void addPrerequisite_shouldRefuseARequiredCycle_throughAnotherCourse() {
        // basics already requires middle, which requires advanced: advanced -> basics would close the loop
        Course middle = course(5L, "Java middle", "Java", "PUBLISHED");
        managing(advanced);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(basics));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.empty());
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(2L)).thenReturn(List.of(link(basics, middle, true)));
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(5L)).thenReturn(List.of(link(middle, advanced, true)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addPrerequisite(1L, 2L, true, TEACHER));
        assertEquals(CoursePrerequisiteServiceImpl.CYCLE, ex.getMessage());
        verify(prerequisiteRepository, never()).save(any());
    }

    @Test
    void addPrerequisite_shouldAllowSharedPrerequisites_thatAreNotACycle() {
        // basics -> middle and middle -> middle's other prerequisite, visited once each
        Course middle = course(5L, "Java middle", "Java", "PUBLISHED");
        managing(advanced);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(basics));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.empty());
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(2L))
                .thenReturn(List.of(link(basics, middle, true), link(basics, middle, true)));
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(5L)).thenReturn(List.of());

        service.addPrerequisite(1L, 2L, true, TEACHER);

        assertTrue(saved().isRequired());
        verify(prerequisiteRepository, times(1)).findByCourse_CourseIdAndRequiredTrue(5L);
    }

    @Test
    void addPrerequisite_shouldBeRefused_forATeacherOfAnotherCourse() {
        managing(advanced);
        doThrow(new AccessDeniedException("not yours")).when(courseAccessService).ensureCanManageCourse(advanced, TEACHER);

        assertThrows(AccessDeniedException.class, () -> service.addPrerequisite(1L, 2L, true, TEACHER));
        verify(prerequisiteRepository, never()).save(any());
    }

    @Test
    void addPrerequisite_shouldThrow_whenAPrerequisiteCourseDoesNotExist() {
        managing(advanced);
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.addPrerequisite(1L, 9L, false, TEACHER));
    }

    @Test
    void addPrerequisite_shouldThrow_whenTheCourseDoesNotExist() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.addPrerequisite(9L, 2L, false, TEACHER));
        verifyNoInteractions(courseAccessService);
    }

    @Test
    void addPrerequisite_shouldRefuseRequired_whenATopicIsMissing() {
        managing(advanced);
        basics.setTopic(null);
        when(courseRepository.findById(2L)).thenReturn(Optional.of(basics));
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.addPrerequisite(1L, 2L, true, TEACHER));
    }

    // --- removing --------------------------------------------------------------

    @Test
    void removePrerequisite_shouldDeleteTheLink() {
        managing(advanced);
        CoursePrerequisite link = link(advanced, basics, true);
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.of(link));

        service.removePrerequisite(1L, 2L, TEACHER);

        verify(courseAccessService).ensureCanManageCourse(advanced, TEACHER);
        verify(prerequisiteRepository).delete(link);
    }

    @Test
    void removePrerequisite_shouldThrow_whenNotLinked() {
        managing(advanced);
        when(prerequisiteRepository.findByCourse_CourseIdAndPrerequisiteCourse_CourseId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.removePrerequisite(1L, 2L, TEACHER));
        verify(prerequisiteRepository, never()).delete(any());
    }

    // --- the enrolment check ---------------------------------------------------

    @Test
    void getMissingRequired_shouldListRequiredCourses_notCompletedByTheStudent() {
        Course patterns = course(4L, "Java patterns", "Java", "PUBLISHED");
        Course streams = course(5L, "Java streams", "Java", "PUBLISHED");
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(1L)).thenReturn(List.of(
                link(advanced, basics, true), link(advanced, patterns, true), link(advanced, streams, true)));
        when(enrollmentRepository.findByStudent_EmailAndCourse_CourseId(STUDENT, 2L))
                .thenReturn(Optional.of(enrollment(Enrollment.CompletionStatus.COMPLETED)));
        when(enrollmentRepository.findByStudent_EmailAndCourse_CourseId(STUDENT, 4L))
                .thenReturn(Optional.of(enrollment(Enrollment.CompletionStatus.IN_PROGRESS)));
        when(enrollmentRepository.findByStudent_EmailAndCourse_CourseId(STUDENT, 5L))
                .thenReturn(Optional.empty());

        assertEquals(List.of(patterns, streams), service.getMissingRequired(1L, STUDENT));
    }

    @Test
    void getMissingRequired_shouldListEveryRequiredCourse_forAnonymousVisitor() {
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(1L))
                .thenReturn(List.of(link(advanced, basics, true)));

        assertEquals(List.of(basics), service.getMissingRequired(1L, null));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void ensurePrerequisitesMet_shouldNameTheMissingCourses() {
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(1L))
                .thenReturn(List.of(link(advanced, basics, true)));
        when(enrollmentRepository.findByStudent_EmailAndCourse_CourseId(STUDENT, 2L)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.ensurePrerequisitesMet(STUDENT, 1L));
        assertEquals("Complete these courses before enrolling: Java basics.", ex.getMessage());
    }

    @Test
    void ensurePrerequisitesMet_shouldPass_whenEverythingRequiredIsCompleted() {
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(1L))
                .thenReturn(List.of(link(advanced, basics, true)));
        when(enrollmentRepository.findByStudent_EmailAndCourse_CourseId(STUDENT, 2L))
                .thenReturn(Optional.of(enrollment(Enrollment.CompletionStatus.COMPLETED)));

        assertDoesNotThrow(() -> service.ensurePrerequisitesMet(STUDENT, 1L));
    }

    @Test
    void ensurePrerequisitesMet_shouldPass_withoutRequiredPrerequisites() {
        when(prerequisiteRepository.findByCourse_CourseIdAndRequiredTrue(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.ensurePrerequisitesMet(STUDENT, 1L));
    }
}
