package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;

import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LectureRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

public class LectureService {

    @Autowired
    private LectureRepository lectureRepository;

    // Create or Update a user
    public Lecture saveLectures(Lecture lectures) {
        return lectureRepository.save(lectures);
    }


    // Get all lectures
    public List<Lecture> getAllLectures() {
        return lectureRepository.findAll();
    }

    // Get a lecture by ID
    public Optional<Lecture> getLecturesById(Long lectureId) {
        return lectureRepository.findById(lectureId);
    }

    // Delete a user by ID
    public void deleteUserById(Long lectureId) {
        lectureRepository.deleteById(lectureId);
    }
}