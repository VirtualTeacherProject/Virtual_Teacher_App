package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureNote;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureNoteRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseAccessService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SCRUM-20: private plain-text notes per lecture. */
@ExtendWith(MockitoExtension.class)
class LectureNoteServiceImplTest {

    private static final String EMAIL = "student@test.com";

    @Mock private LectureNoteRepository lectureNoteRepository;
    @Mock private LectureService lectureService;
    @Mock private UserService userService;
    @Mock private CourseAccessService courseAccessService;

    @InjectMocks
    private LectureNoteServiceImpl lectureNoteService;

    private Course course;
    private Lecture lecture;
    private User user;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setCourseId(10L);

        lecture = new Lecture();
        lecture.setLectureId(100L);
        lecture.setCourse(course);

        user = new User();
        user.setUserId(1L);
        user.setEmail(EMAIL);
    }

    private LectureNote existingNote(String content) {
        LectureNote note = new LectureNote();
        note.setNoteId(7L);
        note.setLecture(lecture);
        note.setUser(user);
        note.setContent(content);
        return note;
    }

    private void lectureIsAccessible() {
        when(lectureService.getLecturesById(100L)).thenReturn(Optional.of(lecture));
        when(courseAccessService.canAccessCourseContent(course, EMAIL)).thenReturn(true);
    }

    // --- getNoteContent ------------------------------------------------------

    @Test
    void getNoteContent_shouldReturnTheUsersNote() {
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.of(existingNote("My note")));

        assertEquals("My note", lectureNoteService.getNoteContent(100L, EMAIL));
    }

    @Test
    void getNoteContent_shouldReturnEmptyString_whenThereIsNoNote() {
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.empty());

        assertEquals("", lectureNoteService.getNoteContent(100L, EMAIL));
    }

    @Test
    void getNoteContent_shouldReturnEmptyString_forAnonymousVisitor() {
        assertEquals("", lectureNoteService.getNoteContent(100L, null));

        verifyNoInteractions(lectureNoteRepository);
    }

    // --- saveNote ------------------------------------------------------------

    @Test
    void saveNote_shouldCreateNote_whenUserHasNone() {
        lectureIsAccessible();
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(user);

        lectureNoteService.saveNote(100L, EMAIL, "First note");

        verify(lectureNoteRepository).save(argThat(saved ->
                saved.getNoteId() == null
                        && saved.getLecture().equals(lecture)
                        && saved.getUser().equals(user)
                        && saved.getContent().equals("First note")
                        && saved.getUpdatedAt() != null));
    }

    @Test
    void saveNote_shouldUpdateExistingNote_insteadOfCreatingASecondOne() {
        lectureIsAccessible();
        LectureNote note = existingNote("Old text");
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.of(note));

        lectureNoteService.saveNote(100L, EMAIL, "New text");

        verify(lectureNoteRepository).save(note);
        assertEquals("New text", note.getContent());
        assertEquals(7L, note.getNoteId());
        assertNotNull(note.getUpdatedAt());
        verifyNoInteractions(userService);
    }

    @Test
    void saveNote_shouldKeepTextAsWritten_andNormaliseLineBreaks() {
        lectureIsAccessible();
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(user);

        lectureNoteService.saveNote(100L, EMAIL, "  indented\r\n<b>second</b> line");

        verify(lectureNoteRepository).save(argThat(saved ->
                saved.getContent().equals("  indented\n<b>second</b> line")));
    }

    @Test
    void saveNote_shouldDeleteExistingNote_whenContentIsBlank() {
        lectureIsAccessible();
        LectureNote note = existingNote("Old text");
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.of(note));

        lectureNoteService.saveNote(100L, EMAIL, "   \r\n ");

        verify(lectureNoteRepository).delete(note);
        verify(lectureNoteRepository, never()).save(any());
    }

    @Test
    void saveNote_shouldDoNothing_whenContentIsNullAndThereIsNoNote() {
        lectureIsAccessible();
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.empty());

        lectureNoteService.saveNote(100L, EMAIL, null);

        verify(lectureNoteRepository, never()).delete(any());
        verify(lectureNoteRepository, never()).save(any());
    }

    @Test
    void saveNote_shouldAcceptContentOfExactlyTheMaximumLength() {
        lectureIsAccessible();
        when(lectureNoteRepository.findByLecture_LectureIdAndUser_Email(100L, EMAIL))
                .thenReturn(Optional.empty());
        when(userService.findByEmail(EMAIL)).thenReturn(user);

        lectureNoteService.saveNote(100L, EMAIL, "a".repeat(LectureNote.MAX_LENGTH));

        verify(lectureNoteRepository).save(any(LectureNote.class));
    }

    @Test
    void saveNote_shouldRejectContentLongerThanTheMaximum() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lectureNoteService.saveNote(
                        100L, EMAIL, "a".repeat(LectureNote.MAX_LENGTH + 1)));

        assertEquals("Notes cannot be longer than 5000 characters", exception.getMessage());
        verifyNoInteractions(lectureService, lectureNoteRepository);
    }

    @Test
    void saveNote_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() {
        when(lectureService.getLecturesById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> lectureNoteService.saveNote(999L, EMAIL, "Note"));

        verifyNoInteractions(lectureNoteRepository);
    }

    @Test
    void saveNote_shouldBeRefused_whenUserCannotAccessTheCourseContent() {
        when(lectureService.getLecturesById(100L)).thenReturn(Optional.of(lecture));
        when(courseAccessService.canAccessCourseContent(course, EMAIL)).thenReturn(false);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> lectureNoteService.saveNote(100L, EMAIL, "Note"));

        assertEquals("Enroll in this course to take notes", exception.getMessage());
        verifyNoInteractions(lectureNoteRepository);
    }
}
