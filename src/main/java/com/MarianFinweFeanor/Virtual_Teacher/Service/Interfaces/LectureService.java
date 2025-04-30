package com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;

import java.util.List;
import java.util.Optional;

public interface LectureService {
    Lecture createLectures(Lecture lectures);

    // Get all lectures
    List<Lecture> getAllLectures();

    // Get a lecture by ID
    Optional<Lecture> getLecturesById(Long lectureId);


    void delete(long id);

    Lecture updateLecture(Long id, Lecture updatedLecture);
}
