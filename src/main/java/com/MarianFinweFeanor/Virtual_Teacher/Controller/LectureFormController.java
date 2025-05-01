package com.MarianFinweFeanor.Virtual_Teacher.Controller;


import com.MarianFinweFeanor.Virtual_Teacher.Model.Lecture;
import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureServiceImpl;
import org.springframework.ui.Model;

import com.MarianFinweFeanor.Virtual_Teacher.Service.LectureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LectureFormController {

    @Autowired
    private LectureServiceImpl lectureService;

    @GetMapping("/add-lecture")
    public String showForm(Model model) {
        model.addAttribute("lecture", new Lecture());
        return "add-lecture";
    }

    @PostMapping("/add-lecture")
    public String submitForm(@ModelAttribute Lecture lecture) {
        lectureService.saveLecture(lecture);
        return "redirect:/home";
    }
}

