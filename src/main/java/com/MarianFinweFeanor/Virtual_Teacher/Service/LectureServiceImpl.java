package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ContentCleanupService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.LectureService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Service
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final ContentCleanupService contentCleanupService;
    private final EnrollmentService enrollmentService;

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository,
                              CourseRepository courseRepository,
                              ContentCleanupService contentCleanupService,
                              EnrollmentService enrollmentService) {
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
        this.contentCleanupService = contentCleanupService;
        this.enrollmentService = enrollmentService;
    }

    public Lecture saveLecture(Lecture lecture) {
        return lectureRepository.save(lecture);
    }

    @Override
    public Lecture createLectures(Lecture lecture) {
        Long courseId = lecture.getCourse().getCourseId();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        lecture.setCourse(course); // Attach managed entity
        return lectureRepository.save(lecture);
        //TODO FILTERING EXCEPTION HANDLING CHECK IF LECTURE WITH THE SAME NAME EXISTS
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lecture> getByCourseId(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        return lectureRepository.findAllByCourse(course);
        // or, if you prefer: lectureRepository.findAllByCourse_CourseId(courseId)
    }


    // Get all lectures
    @Override
    public List<Lecture> getAllLectures() {
        if(lectureRepository.count() == 0) {
            throw new EntityNotFoundException("Lectures", "database");
        }
        return lectureRepository.findAll();
    }

    // Get a lecture by ID
    @Override
    public Optional<Lecture> getLecturesById(Long lectureId) {
        if(!lectureRepository.existsById(lectureId)) {
            throw new EntityNotFoundException("Lecture", lectureId);
        }
        return lectureRepository.findById(lectureId);
    }


    @Override
    @Transactional
    public void delete(long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecture", id));

        // SCRUM-15: submissions and comments belong to the lecture and go with it.
        contentCleanupService.deleteLectureContent(id);
        lectureRepository.delete(lecture);

        // Grades and completion depend on the course's lectures, so refresh them.
        if (lecture.getCourse() != null) {
            enrollmentService.recalculateProgressForCourse(lecture.getCourse().getCourseId());
        }
    }
    @Override
    public Lecture updateLecture(Long id, Lecture updatedLecture) {
        return lectureRepository.findById(id)
                .map(lecture -> {
                    lecture.setTitle(updatedLecture.getTitle());
                    lecture.setDescription(updatedLecture.getDescription());
                    lecture.setVideoUrl(updatedLecture.getVideoUrl());
                    lecture.setAssignmentFilePath(updatedLecture.getAssignmentFilePath());
                    return lectureRepository.save(lecture);
                })
                .orElseThrow(() -> new EntityNotFoundException("Lecture", id));
    }
}