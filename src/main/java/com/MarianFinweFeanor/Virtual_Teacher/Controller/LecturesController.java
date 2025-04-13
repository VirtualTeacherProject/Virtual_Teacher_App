package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LecturesController {
    private final LectureService lectureService;
    @Autowired
    public LecturesController(LectureService lectureService) {
        this.lectureService = lectureService;
    }
    @GetMapping
    public List<Lecture> getAllLectures() {
        return lectureService.getAllLectures();
    }
    @GetMapping("/{id}")
    public Lecture getLectureById(@PathVariable Long id) {
        return lectureService.getLecturesById(id).orElseThrow(()->new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, String.format("Lecture with id %d not found", id)));
    }
    @PostMapping
    public Lecture createLecture(@RequestBody Lecture lecture) {
        return lectureService.saveLectures(lecture);
    }

    // 2. Get a Lecture by ID
    // 3. Get All Lectures
    // 4. Update a Lecture
    // 5. Delete a Lecture
    // 6. Get Lectures by Course ID
    // 7. Get Lectures by Status (e.g., Published, Draft)
}
