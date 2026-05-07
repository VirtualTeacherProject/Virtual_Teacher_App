package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.Enrollment;
import com.MarianFinweFeanor.Virtual_Teacher.Repositories.EnrollmentRepository;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.EnrollmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
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
    private final UserService userService;
    private final EnrollmentService enrollmentService;

    public CourseRatingMvcController(EnrollmentRepository enrollmentRepository,
                                     EnrollmentService enrollmentService,
                                     UserService userService) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
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

//        if (e.getCompletionStatus() != Enrollment.CompletionStatus.COMPLETED) {
//            ra.addFlashAttribute("error", "You can rate only after completing the course.");
//            return "redirect:/courses/" + courseId;
//        }

        if (rating == null || rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "Rating must be between 1 and 5.");
            return "redirect:/courses/" + courseId;
        }

        try {
            enrollmentService.ensureCanRateCourse(e.getStudent().getUserId(), courseId);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/courses/" + courseId;
        }

//        if(e.getAverageGrade() == null) {
//            ra.addFlashAttribute("error", "You can rate only after your course grade is calculated");
//            return "redirect:/courses/" + courseId;
//        }

//        Double passingGrade = e.getCourse().getPassingGrade();
//
//        if (passingGrade != null && e.getAverageGrade() < passingGrade) {
//            ra.addFlashAttribute("error", "You can rate only after passing the course.");
//            return "redirect:/courses/" + courseId;
//        }


        e.setRating(rating);
        e.setReviewText(reviewText);
        e.setRatedAt(LocalDateTime.now());
        enrollmentRepository.save(e);

        ra.addFlashAttribute("msg", "Thanks! Your rating was saved.");
        return "redirect:/courses/" + courseId;
    }
}

