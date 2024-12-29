package com.MarianFinweFeanor.Virtual_Teacher.Service;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lectures;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.LecturesRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

public class LecturesService {

    @Autowired
    private LecturesRepository lecturesRepository;

    // Create or Update a user
    public Lectures saveLectures(Lectures lectures) {
        return lecturesRepository.save(lectures);
    }


    // Get all lectures
    public List<Lectures> getAllLectures() {
        return lecturesRepository.findAll();
    }

    // Get a lecture by ID
    public Optional<Lectures> getLecturesById(Long lectureId) {
        return lecturesRepository.findById(lectureId);
    }

    // Delete a user by ID
    public void deleteUserById(Long lectureId) {
        lecturesRepository.deleteById(lectureId);
    }
}
