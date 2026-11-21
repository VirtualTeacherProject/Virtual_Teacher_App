package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceEntry;
import com.MarianFinweFeanor.Virtual_Teacher.Model.AttendanceStatus;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureAttendance;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureAttendanceRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** SCRUM-28: the attendance tracker; SCRUM-51: only for live sessions that have started. */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    private static final String TEACHER = "teacher@test.com";
    private static final LocalDateTime START = LocalDateTime.now().minusHours(1);

    @Mock private LectureAttendanceRepository attendanceRepository;
    @Mock private LectureService lectureService;
    @Mock private EnrollmentService enrollmentService;
    @Mock private UserService userService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Course course;
    private Lecture lecture;
    private User anna;
    private User boris;
    private User carl;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(1L);
        course.setAttendanceRequired(true);

        lecture = new Lecture();
        lecture.setLectureId(5L);
        lecture.setCourse(course);
        lecture.setLive(true);
        lecture.setLiveStart(START);

        // deliberately not in name order
        carl = student(12L, "Carl", "Young");
        anna = student(10L, "Anna", "Adams");
        boris = student(11L, "Boris", "Brown");
    }

    private static User student(Long id, String first, String last) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(first.toLowerCase() + "@test.com");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private static LectureAttendance record(Lecture lecture, User student, AttendanceStatus status) {
        LectureAttendance record = new LectureAttendance();
        record.setLecture(lecture);
        record.setStudent(student);
        record.setStatus(status);
        record.setRecordedAt(LocalDateTime.now().minusDays(1));
        return record;
    }

    private void lectureAndRoster() {
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));
        when(enrollmentService.getStudentsInCourseEnrolledBy(1L, START)).thenReturn(List.of(carl, anna, boris));
    }

    @SuppressWarnings("unchecked")
    private List<LectureAttendance> savedRecords() {
        ArgumentCaptor<List<LectureAttendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // --- reading -------------------------------------------------------------

    @Test
    void getLectureAttendance_shouldListEnrolledStudentsByName_withRecordedStatusOrNull() {
        lectureAndRoster();
        when(attendanceRepository.findByLecture_LectureId(5L)).thenReturn(List.of(
                record(lecture, boris, AttendanceStatus.ABSENT),
                record(lecture, carl, AttendanceStatus.PRESENT)));

        List<AttendanceEntry> entries = attendanceService.getLectureAttendance(1L, 5L, TEACHER);

        verify(userService).ensureApprovedTeacher(TEACHER);
        assertEquals(List.of(anna, boris, carl), entries.stream().map(AttendanceEntry::student).toList());
        assertNull(entries.get(0).status());
        assertFalse(entries.get(0).isRecorded());
        assertEquals(AttendanceStatus.ABSENT, entries.get(1).status());
        assertTrue(entries.get(1).isAbsent());
        assertEquals(AttendanceStatus.PRESENT, entries.get(2).status());
        assertFalse(entries.get(2).isAbsent());
    }

    @Test
    void getLectureAttendance_shouldStillWork_whenTheCourseNoLongerRequiresAttendance() {
        course.setAttendanceRequired(false);
        lectureAndRoster();
        when(attendanceRepository.findByLecture_LectureId(5L))
                .thenReturn(List.of(record(lecture, anna, AttendanceStatus.ABSENT)));

        List<AttendanceEntry> entries = attendanceService.getLectureAttendance(1L, 5L, TEACHER);

        assertEquals(3, entries.size());
        assertTrue(entries.get(0).isAbsent());
    }

    @Test
    void getLectureAttendance_shouldBeEmpty_whenNobodyIsEnrolled() {
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));
        when(enrollmentService.getStudentsInCourseEnrolledBy(1L, START)).thenReturn(List.of());
        when(attendanceRepository.findByLecture_LectureId(5L)).thenReturn(List.of());

        assertTrue(attendanceService.getLectureAttendance(1L, 5L, TEACHER).isEmpty());
    }

    @Test
    void getLectureForAttendance_shouldReturnTheLecture_ofTheCourse() {
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        assertSame(lecture, attendanceService.getLectureForAttendance(1L, 5L, TEACHER));
        verify(userService).ensureApprovedTeacher(TEACHER);
    }

    @Test
    void getLectureForAttendance_shouldThrowNotFound_forMissingLecture() {
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> attendanceService.getLectureForAttendance(1L, 5L, TEACHER));
    }

    @Test
    void getLectureForAttendance_shouldThrowNotFound_whenLectureBelongsToAnotherCourse() {
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        assertThrows(EntityNotFoundException.class,
                () -> attendanceService.getLectureForAttendance(2L, 5L, TEACHER));
    }

    @Test
    void getLectureForAttendance_shouldThrowNotFound_whenLectureHasNoCourse() {
        lecture.setCourse(null);
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        assertThrows(EntityNotFoundException.class,
                () -> attendanceService.getLectureForAttendance(1L, 5L, TEACHER));
    }

    @Test
    void reading_shouldBeRefused_forUnapprovedTeacherOrStudent() {
        doThrow(new AccessDeniedException("pending")).when(userService).ensureApprovedTeacher(TEACHER);

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.getLectureAttendance(1L, 5L, TEACHER));
        assertThrows(AccessDeniedException.class,
                () -> attendanceService.getLectureForAttendance(1L, 5L, TEACHER));
        verifyNoInteractions(attendanceRepository, lectureService);
    }

    // --- recording -----------------------------------------------------------

    @Test
    void recordAttendance_shouldStoreAbsentForTickedStudents_andPresentForEveryoneElse() {
        lectureAndRoster();
        when(attendanceRepository.findByLecture_LectureId(5L)).thenReturn(List.of());

        List<AttendanceEntry> result =
                attendanceService.recordAttendance(1L, 5L, TEACHER, List.of(11L));

        List<LectureAttendance> saved = savedRecords();
        assertEquals(3, saved.size());
        for (LectureAttendance record : saved) {
            assertSame(lecture, record.getLecture());
            assertNotNull(record.getRecordedAt());
            assertEquals(record.getStudent() == boris ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT,
                    record.getStatus());
        }

        assertEquals(List.of(anna, boris, carl), result.stream().map(AttendanceEntry::student).toList());
        assertEquals(List.of(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.PRESENT),
                result.stream().map(AttendanceEntry::status).toList());
    }

    @Test
    void recordAttendance_shouldMarkEveryonePresent_whenNoStudentIsTicked() {
        lectureAndRoster();
        when(attendanceRepository.findByLecture_LectureId(5L)).thenReturn(List.of());

        List<AttendanceEntry> result = attendanceService.recordAttendance(1L, 5L, TEACHER, null);

        assertTrue(savedRecords().stream().allMatch(r -> r.getStatus() == AttendanceStatus.PRESENT));
        assertTrue(result.stream().noneMatch(AttendanceEntry::isAbsent));
    }

    @Test
    void recordAttendance_shouldUpdateExistingRecords_insteadOfAddingNewOnes() {
        lectureAndRoster();
        LectureAttendance annasOld = record(lecture, anna, AttendanceStatus.ABSENT);
        annasOld.setAttendanceId(100L);
        LocalDateTime before = annasOld.getRecordedAt();
        when(attendanceRepository.findByLecture_LectureId(5L)).thenReturn(new ArrayList<>(List.of(annasOld)));

        attendanceService.recordAttendance(1L, 5L, TEACHER, List.of());

        List<LectureAttendance> saved = savedRecords();
        assertEquals(3, saved.size());
        LectureAttendance annas = saved.stream().filter(r -> r.getStudent() == anna).findFirst().orElseThrow();
        assertSame(annasOld, annas);
        assertEquals(100L, annas.getAttendanceId());
        assertEquals(AttendanceStatus.PRESENT, annas.getStatus());
        assertTrue(annas.getRecordedAt().isAfter(before));
    }

    @Test
    void recordAttendance_shouldBeRefused_whenTheCourseDoesNotRequireAttendance() {
        course.setAttendanceRequired(false);
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> attendanceService.recordAttendance(1L, 5L, TEACHER, List.of(10L)));

        assertTrue(ex.getMessage().contains("does not require attendance"));
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void recordAttendance_shouldBeRefused_forSelfPacedLecture() {
        lecture.setLive(false);
        lecture.setLiveStart(null);
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> attendanceService.recordAttendance(1L, 5L, TEACHER, List.of()));

        assertEquals(AttendanceServiceImpl.NOT_LIVE, ex.getMessage());
        verifyNoInteractions(enrollmentService);
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void recordAttendance_shouldBeRefused_beforeTheLiveSessionStarts() {
        lecture.setLiveStart(LocalDateTime.now().plusDays(1));
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> attendanceService.recordAttendance(1L, 5L, TEACHER, List.of()));

        assertEquals(AttendanceServiceImpl.NOT_STARTED, ex.getMessage());
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void recordAttendance_shouldOnlyRecordStudentsEnrolledWhenTheSessionStarted() {
        // dave enrolled after the start, so the roster query leaves him out
        lectureAndRoster();
        when(attendanceRepository.findByLecture_LectureId(5L)).thenReturn(List.of());

        attendanceService.recordAttendance(1L, 5L, TEACHER, List.of());

        assertEquals(3, savedRecords().size());
        verify(enrollmentService, never()).getStudentsInCourse(any());
    }

    @Test
    void recordAttendance_shouldRefuseToMarkAStudentWhoEnrolledAfterTheStart() {
        lectureAndRoster();   // dave (13) is enrolled now but not on the roll call

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> attendanceService.recordAttendance(1L, 5L, TEACHER, List.of(13L)));

        assertEquals(AttendanceServiceImpl.NOT_ON_ROLL_CALL, ex.getMessage());
        verify(attendanceRepository, never()).saveAll(any());
    }

    // --- SCRUM-51: when attendance is open --------------------------------------

    @Test
    void attendanceClosedReason_shouldBeNull_forStartedLiveLectureOfAttendanceCourse() {
        assertNull(attendanceService.attendanceClosedReason(lecture));
    }

    @Test
    void attendanceClosedReason_shouldExplainWhyAttendanceIsClosed() {
        lecture.setLiveStart(LocalDateTime.now().plusMinutes(5));
        assertEquals(AttendanceServiceImpl.NOT_STARTED, attendanceService.attendanceClosedReason(lecture));

        lecture.setLiveStart(null);
        assertEquals(AttendanceServiceImpl.NOT_STARTED, attendanceService.attendanceClosedReason(lecture));

        lecture.setLive(false);
        assertEquals(AttendanceServiceImpl.NOT_LIVE, attendanceService.attendanceClosedReason(lecture));

        course.setAttendanceRequired(false);
        assertEquals(AttendanceServiceImpl.NOT_REQUIRED, attendanceService.attendanceClosedReason(lecture));
    }

    @Test
    void getLectureAttendance_ofSelfPacedLecture_shouldListEveryoneEnrolled_soOldRecordsStayReadable() {
        lecture.setLive(false);
        lecture.setLiveStart(null);
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));
        when(enrollmentService.getStudentsInCourse(1L)).thenReturn(List.of(boris, anna));
        when(attendanceRepository.findByLecture_LectureId(5L))
                .thenReturn(List.of(record(lecture, boris, AttendanceStatus.ABSENT)));

        List<AttendanceEntry> entries = attendanceService.getLectureAttendance(1L, 5L, TEACHER);

        assertEquals(List.of(anna, boris), entries.stream().map(AttendanceEntry::student).toList());
        assertTrue(entries.get(1).isAbsent());
        verify(enrollmentService, never()).getStudentsInCourseEnrolledBy(any(), any());
    }

    @Test
    void recordAttendance_shouldBeRefused_forStudentWhoIsNotEnrolled() {
        lectureAndRoster();

        assertThrows(IllegalArgumentException.class,
                () -> attendanceService.recordAttendance(1L, 5L, TEACHER, List.of(10L, 99L)));

        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void recordAttendance_shouldThrowNotFound_whenLectureBelongsToAnotherCourse() {
        when(lectureService.getLecturesById(5L)).thenReturn(Optional.of(lecture));

        assertThrows(EntityNotFoundException.class,
                () -> attendanceService.recordAttendance(2L, 5L, TEACHER, List.of()));
        verify(attendanceRepository, never()).saveAll(any());
    }

    @Test
    void recordAttendance_shouldBeRefused_forUnapprovedTeacher() {
        doThrow(new AccessDeniedException("pending")).when(userService).ensureApprovedTeacher(TEACHER);

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.recordAttendance(1L, 5L, TEACHER, List.of()));
        verifyNoInteractions(attendanceRepository, lectureService, enrollmentService);
    }
}
