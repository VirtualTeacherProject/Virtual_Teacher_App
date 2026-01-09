package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/courses/{courseId}/rating")
public class CourseRatingMvcController {

    private final EnrollmentRepository enrollmentRepository;

    public CourseRatingMvcController(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @PostMapping
    public String rate(@PathVariable Long courseId,
                       @RequestParam Integer rating,
                       @RequestParam(required = false) String reviewText,
                       Principal principal,
                       RedirectAttributes ra) {

        if (principal == null) return "redirect:/login";

        Enrollment e = enrollmentRepository
                .findByStudent_EmailAndCourse_CourseId(principal.getName(), courseId)
                .orElseThrow(() -> new RuntimeException("Not enrolled"));

        if (e.getCompletionStatus() != Enrollment.CompletionStatus.COMPLETED) {
            ra.addFlashAttribute("error", "You can rate only after completing the course.");
            return "redirect:/courses/" + courseId;
        }

        if (rating == null || rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "Rating must be between 1 and 5.");
            return "redirect:/courses/" + courseId;
        }

        if(e.getAverageGrade() == null) {
            ra.addFlashAttribute("error", "You can rate only after your course grade is calculated");
            return "redirect:/courses/" + courseId;
        }

        e.setRating(rating);
        e.setReviewText(reviewText);
        e.setRatedAt(LocalDateTime.now());
        enrollmentRepository.save(e);

        ra.addFlashAttribute("msg", "Thanks! Your rating was saved.");
        return "redirect:/courses/" + courseId;
    }
}

