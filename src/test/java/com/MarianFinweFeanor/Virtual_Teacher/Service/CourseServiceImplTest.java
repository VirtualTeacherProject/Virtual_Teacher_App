package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private User teacher;
    private Course course;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setUserId(1L);
        teacher.setEmail("teacher@test.com");

        course = new Course();
        course.setTitle("Java Course");
        course.setTopic("Java");
        course.setDescription("Java backend course");
        course.setTeacher(teacher);
    }

    @Test
    void createCourse_shouldSaveCourseAsDraft() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(teacher));

        when(courseRepository.save(course))
                .thenReturn(course);

        Course result = courseService.createCourse(course);

        assertEquals("DRAFT", result.getStatus());
        assertEquals(teacher, result.getTeacher());

        verify(userRepository).findById(1L);
        verify(courseRepository).save(course);
    }

    @Test
    void createCourse_shouldThrowEntityNotFoundException_whenTeacherDoesNotExist() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.createCourse(course)
        );

        assertEquals("User with id 1 not found", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void getCourseById_shouldReturnCourse_whenCourseExists() {
        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        Optional<Course> result = courseService.getCourseById(1L);

        assertTrue(result.isPresent());
        assertEquals(course, result.get());

        verify(courseRepository).findById(1L);
    }

    @Test
    void getCourseById_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseRepository.findById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.getCourseById(99L)
        );

        assertEquals("Course with id 99 not found", exception.getMessage());

        verify(courseRepository).findById(99L);
    }

    @Test
    void publishCourse_shouldSetStatusToPublished_whenCourseIsValid() {
        course.setStatus("DRAFT");
        course.setTitle("Java Course");
        course.setTopic("Java");
        course.setDescription("Java backend course");

        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        when(courseRepository.save(course))
                .thenReturn(course);

        Course result = courseService.publishCourse(1L);

        assertEquals("PUBLISHED", result.getStatus());

        verify(courseRepository).findById(1L);
        verify(courseRepository).save(course);
    }

    @Test
    void publishCourse_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseRepository.findById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.publishCourse(99L)
        );

        assertEquals("Course with id 99 not found", exception.getMessage());

        verify(courseRepository).findById(99L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void publishCourse_shouldThrowIllegalStateException_whenTitleIsBlank() {
        course.setTitle("   ");
        course.setTopic("Java");
        course.setDescription("Java backend course");

        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.publishCourse(1L)
        );

        assertEquals(
                "Course title is required before publishing.",
                exception.getMessage()
        );

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void publishCourse_shouldThrowIllegalStateException_whenTopicIsBlank() {
        course.setTitle("Java Course");
        course.setTopic("   ");
        course.setDescription("Java backend course");

        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.publishCourse(1L)
        );

        assertEquals(
                "Course topic is required before publishing.",
                exception.getMessage()
        );

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void publishCourse_shouldThrowIllegalStateException_whenDescriptionIsBlank() {
        course.setTitle("Java Course");
        course.setTopic("Java");
        course.setDescription("   ");

        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courseService.publishCourse(1L)
        );

        assertEquals(
                "Course description is required before publishing.",
                exception.getMessage()
        );

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void getCourse_shouldReturnCourse_whenCourseExists() {
        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        Course result = courseService.getCourse(1L);

        assertEquals(course, result);

        verify(courseRepository).findById(1L);
    }

    @Test
    void getCourse_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseRepository.findById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.getCourse(99L)
        );

        assertEquals("Course with id 99 not found", exception.getMessage());

        verify(courseRepository).findById(99L);
    }

    @Test
    void getVisibleCourses_shouldReturnPublishedAndOwnCourses_whenUserCanManageCourses() {
        String email = "teacher@test.com";
        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatusOrTeacher_Email("PUBLISHED", email))
                .thenReturn(expectedCourses);

        List<Course> result = courseService.getVisibleCourses(email, true);

        assertEquals(expectedCourses, result);

        verify(courseRepository)
                .findByStatusOrTeacher_Email("PUBLISHED", email);

        verify(courseRepository, never())
                .findByStatus("PUBLISHED");
    }

    @Test
    void getVisibleCourses_shouldReturnOnlyPublishedCourses_whenUserCannotManageCourses() {
        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatus("PUBLISHED"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.getVisibleCourses("student@test.com", false);

        assertEquals(expectedCourses, result);

        verify(courseRepository).findByStatus("PUBLISHED");

        verify(courseRepository, never())
                .findByStatusOrTeacher_Email(anyString(), anyString());
    }

    @Test
    void getVisibleCourses_shouldReturnOnlyPublishedCourses_whenUserIsAnonymous() {
        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatus("PUBLISHED"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.getVisibleCourses(null, false);

        assertEquals(expectedCourses, result);

        verify(courseRepository).findByStatus("PUBLISHED");
    }

    @Test
    void getVisibleCourses_shouldReturnOnlyPublishedCourses_whenEmailIsNull() {
        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatus("PUBLISHED"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.getVisibleCourses(null, true);

        assertEquals(expectedCourses, result);

        verify(courseRepository).findByStatus("PUBLISHED");

        verify(courseRepository, never())
                .findByStatusOrTeacher_Email(anyString(), anyString());
    }

    @Test
    void searchVisibleCoursesByTitle_shouldSearchPublishedCourses_whenUserCannotManage() {
        List<Course> expectedCourses = List.of(course);

        when(courseRepository
                .findByStatusAndTitleContainingIgnoreCase("PUBLISHED", "Java"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.searchVisibleCoursesByTitle(
                        "Java",
                        "student@test.com",
                        false
                );

        assertEquals(expectedCourses, result);

        verify(courseRepository)
                .findByStatusAndTitleContainingIgnoreCase(
                        "PUBLISHED",
                        "Java"
                );
    }

    @Test
    void searchVisibleCoursesByTitle_shouldSearchPublishedAndOwnCourses_whenUserCanManage() {
        String email = "teacher@test.com";
        List<Course> expectedCourses = List.of(course);

        when(courseRepository
                .findByStatusAndTitleContainingIgnoreCaseOrTeacher_EmailAndTitleContainingIgnoreCase(
                        "PUBLISHED",
                        "Java",
                        email,
                        "Java"
                ))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.searchVisibleCoursesByTitle(
                        "Java",
                        email,
                        true
                );

        assertEquals(expectedCourses, result);

        verify(courseRepository)
                .findByStatusAndTitleContainingIgnoreCaseOrTeacher_EmailAndTitleContainingIgnoreCase(
                        "PUBLISHED",
                        "Java",
                        email,
                        "Java"
                );
    }

    @Test
    void searchVisibleCoursesByTitle_shouldTrimSearchTitle() {
        when(courseRepository
                .findByStatusAndTitleContainingIgnoreCase(
                        "PUBLISHED",
                        "Java"
                ))
                .thenReturn(List.of(course));

        courseService.searchVisibleCoursesByTitle(
                "   Java   ",
                null,
                false
        );

        verify(courseRepository)
                .findByStatusAndTitleContainingIgnoreCase(
                        "PUBLISHED",
                        "Java"
                );
    }

    @Test
    void searchVisibleCoursesByTitle_shouldReturnVisibleCourses_whenTitleIsBlank() {
        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatus("PUBLISHED"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.searchVisibleCoursesByTitle(
                        "   ",
                        null,
                        false
                );

        assertEquals(expectedCourses, result);

        verify(courseRepository).findByStatus("PUBLISHED");
    }

    @Test
    void searchVisibleCoursesByTitle_shouldReturnVisibleCourses_whenTitleIsNull() {
        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatus("PUBLISHED"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.searchVisibleCoursesByTitle(
                        null,
                        null,
                        false
                );

        assertEquals(expectedCourses, result);

        verify(courseRepository).findByStatus("PUBLISHED");
    }

    @Test
    void countCourses_shouldReturnRepositoryCount() {
        when(courseRepository.count())
                .thenReturn(5L);

        long result = courseService.countCourses();

        assertEquals(5L, result);

        verify(courseRepository).count();
    }

    @Test
    void getAllCourses_shouldReturnAllCourses() {
        List<Course> courses = List.of(course);

        when(courseRepository.findAll())
                .thenReturn(courses);

        List<Course> result = courseService.getAllCourses();

        assertEquals(courses, result);

        verify(courseRepository).findAll();
    }

    @Test
    void getAllCourses_shouldReturnEmptyList_whenNoCoursesExist() {
        when(courseRepository.findAll())
                .thenReturn(List.of());

        List<Course> result = courseService.getAllCourses();

        assertTrue(result.isEmpty());

        verify(courseRepository).findAll();
    }

    @Test
    void updateCourse_shouldUpdateCourseFields() {
        course.setStatus("DRAFT");

        LocalDateTime originalDate =
                LocalDateTime.of(2026, 1, 1, 12, 0);

        course.setStartDate(originalDate);
        course.setPassingGrade(50.0);

        Course updatedCourse = new Course();
        updatedCourse.setTitle("Updated Java Course");
        updatedCourse.setTopic("Spring Boot");
        updatedCourse.setDescription("Updated description");

        LocalDateTime updatedDate =
                LocalDateTime.of(2026, 2, 1, 12, 0);

        updatedCourse.setStartDate(updatedDate);
        updatedCourse.setPassingGrade(75.0);

        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        when(courseRepository.save(course))
                .thenReturn(course);

        Course result =
                courseService.updateCourse(1L, updatedCourse);

        assertEquals("Updated Java Course", result.getTitle());
        assertEquals("Spring Boot", result.getTopic());
        assertEquals("Updated description", result.getDescription());
        assertEquals(updatedDate, result.getStartDate());
        assertEquals(75.0, result.getPassingGrade());

        verify(courseRepository).save(course);
    }

    @Test
    void updateCourse_shouldNotChangeCourseStatus() {
        course.setStatus("DRAFT");

        Course updatedCourse = new Course();
        updatedCourse.setTitle("Updated Course");
        updatedCourse.setTopic("Updated Topic");
        updatedCourse.setDescription("Updated Description");

        // Imagine this came from a manipulated request.
        updatedCourse.setStatus("PUBLISHED");

        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));

        when(courseRepository.save(course))
                .thenReturn(course);

        Course result =
                courseService.updateCourse(1L, updatedCourse);

        assertEquals("DRAFT", result.getStatus());

        verify(courseRepository).save(course);
    }

    @Test
    void updateCourse_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        Course updatedCourse = new Course();

        when(courseRepository.findById(99L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.updateCourse(99L, updatedCourse)
        );

        assertEquals("Course with id 99 not found", exception.getMessage());

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void deleteCourse_shouldDeleteCourse_whenCourseExists() {
        when(courseRepository.existsById(1L))
                .thenReturn(true);

        courseService.deleteCourse(1L);

        verify(courseRepository).existsById(1L);
        verify(courseRepository).deleteById(1L);
    }


    @Test
    void deleteCourse_shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
        when(courseRepository.existsById(99L))
                .thenReturn(false);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.deleteCourse(99L)
        );

        assertEquals("Course with id 99 not found", exception.getMessage());

        verify(courseRepository).existsById(99L);
        verify(courseRepository, never()).deleteById(anyLong());
    }


    @Test
    void getCoursesByStatus_shouldReturnCoursesWithRequestedStatus() {
        course.setStatus("DRAFT");

        List<Course> expectedCourses = List.of(course);

        when(courseRepository.findByStatus("DRAFT"))
                .thenReturn(expectedCourses);

        List<Course> result =
                courseService.getCoursesByStatus("DRAFT");

        assertEquals(expectedCourses, result);

        verify(courseRepository).findByStatus("DRAFT");
    }




}
