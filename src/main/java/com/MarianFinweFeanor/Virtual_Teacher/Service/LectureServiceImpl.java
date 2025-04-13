package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Course;
import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import com.MarianFinweFeanor.Virtual_Teacher.Repositories.CourseRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class LectureServiceImpl implements LectureService {

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public LectureServiceImpl(LectureRepository lectureRepository, CourseRepository courseRepository) {
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
    }
    @Override
    public Lecture createLectures(Lecture lecture) {
        Long courseId = lecture.getCourse().getCourseId();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
        lecture.setCourse(course); // Attach managed entity
        return lectureRepository.save(lecture);
    }


    // Get all lectures
    @Override
    public List<Lecture> getAllLectures() {
        return lectureRepository.findAll();
    }

    // Get a lecture by ID
    @Override
    public Optional<Lecture> getLecturesById(Long lectureId) {
        return lectureRepository.findById(lectureId);
    }


    @Override
    public void delete(long id) {
        if (!lectureRepository.existsById(id)) {
            throw new RuntimeException("Lecture not found with ID: " + id);
        }else {
            lectureRepository.deleteById(id);
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
                .orElseThrow(() -> new RuntimeException("Lecture not found with ID: " + id));
    }
}