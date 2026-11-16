package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/courses/{courseId}/rating")
public class CourseRatingMvcController {

    private final EnrollmentService enrollmentService;

    public CourseRatingMvcController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * POST /courses/{courseId}/rating
     * SCRUM-31: rating 1-5 with a mandatory review of at least 75 characters.
     * The rules live in EnrollmentService.rateCourse.
     */
    @PostMapping
    public String rate(@PathVariable Long courseId,
                       @RequestParam(required = false) Integer rating,
                       @RequestParam(required = false) String reviewText,
                       Principal principal,
                       RedirectAttributes ra) {

        if (principal == null) return "redirect:/login";

        try {
            enrollmentService.rateCourse(principal.getName(), courseId, rating, reviewText);
            ra.addFlashAttribute("msg", "Thanks! Your rating was saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            // keep what the student typed so they can fix it
            ra.addFlashAttribute("ratingDraft", rating);
            ra.addFlashAttribute("reviewDraft", reviewText);
        }

        return "redirect:/courses/" + courseId;
    }
}
