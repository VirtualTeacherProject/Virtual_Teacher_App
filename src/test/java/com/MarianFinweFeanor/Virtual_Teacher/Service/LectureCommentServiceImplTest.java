package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.LectureComment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureCommentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LectureCommentServiceImplTest {

    @Mock
    private LectureCommentRepository lectureCommentRepository;

    @Mock
    private LectureService lectureService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LectureCommentServiceImpl lectureCommentService;

    private Lecture lecture;
    private User user;

    @BeforeEach
    void setUp() {
        lecture = new Lecture();
        lecture.setLectureId(100L);

        user = new User();
        user.setUserId(1L);
        user.setEmail("student@test.com");
    }

    @Test
    void getCommentsForLecture_shouldReturnCommentsFromRepository() {
        LectureComment firstComment = mock(LectureComment.class);
        LectureComment secondComment = mock(LectureComment.class);

        List<LectureComment> expected =
                List.of(firstComment, secondComment);

        when(lectureCommentRepository
                .findByLecture_LectureIdOrderByCreatedAtAsc(100L))
                .thenReturn(expected);

        List<LectureComment> result =
                lectureCommentService.getCommentsForLecture(100L);

        assertEquals(expected, result);

        verify(lectureCommentRepository)
                .findByLecture_LectureIdOrderByCreatedAtAsc(100L);
    }

    @Test
    void addComment_shouldThrowIllegalArgumentException_whenCommentIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lectureCommentService.addComment(
                        100L,
                        "student@test.com",
                        null
                )
        );

        assertEquals(
                "Comments cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(lectureService);
        verifyNoInteractions(userService);
        verify(lectureCommentRepository, never())
                .save(any(LectureComment.class));
    }

    @Test
    void addComment_shouldThrowIllegalArgumentException_whenCommentIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lectureCommentService.addComment(
                        100L,
                        "student@test.com",
                        "     "
                )
        );

        assertEquals(
                "Comments cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(lectureService);
        verifyNoInteractions(userService);
        verify(lectureCommentRepository, never())
                .save(any(LectureComment.class));
    }

    @Test
    void addComment_shouldThrowEntityNotFoundException_whenLectureDoesNotExist() {
        when(lectureService.getLecturesById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureCommentService.addComment(
                        999L,
                        "student@test.com",
                        "Good lecture"
                )
        );

        assertEquals(
                "Lecture with id 999 not found",
                exception.getMessage()
        );

        verify(lectureService).getLecturesById(999L);

        verifyNoInteractions(userService);

        verify(lectureCommentRepository, never())
                .save(any(LectureComment.class));
    }

    @Test
    void addComment_shouldSaveCommentWithLectureAuthorTextAndTimestamp() {
        when(lectureService.getLecturesById(100L))
                .thenReturn(Optional.of(lecture));

        when(userService.findByEmail("student@test.com"))
                .thenReturn(user);

        lectureCommentService.addComment(
                100L,
                "student@test.com",
                "Good lecture"
        );

        verify(lectureCommentRepository).save(argThat(saved ->
                saved.getLecture().equals(lecture)
                        && saved.getAuthor().equals(user)
                        && saved.getComment().equals("Good lecture")
                        && saved.getCreatedAt() != null
        ));
    }

    @Test
    void addComment_shouldTrimCommentBeforeSaving() {
        when(lectureService.getLecturesById(100L))
                .thenReturn(Optional.of(lecture));

        when(userService.findByEmail("student@test.com"))
                .thenReturn(user);

        lectureCommentService.addComment(
                100L,
                "student@test.com",
                "   Good lecture   "
        );

        verify(lectureCommentRepository).save(argThat(saved ->
                saved.getComment().equals("Good lecture")
        ));
    }

    @Test
    void addComment_shouldNotSaveComment_whenUserLookupFails() {
        when(lectureService.getLecturesById(100L))
                .thenReturn(Optional.of(lecture));

        when(userService.findByEmail("missing@test.com"))
                .thenThrow(
                        new EntityNotFoundException(
                                "User",
                                "email",
                                "missing@test.com"
                        )
                );

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> lectureCommentService.addComment(
                        100L,
                        "missing@test.com",
                        "Good lecture"
                )
        );

        assertEquals(
                "User with email missing@test.com not found",
                exception.getMessage()
        );

        verify(lectureCommentRepository, never())
                .save(any(LectureComment.class));
    }







}