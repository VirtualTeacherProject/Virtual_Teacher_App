package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.ResourceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * SCRUM-35: enrolled students ask for additional resources.
 * Requests are listed on the course page (EnrollmentMvcController.courseDetail).
 */
@Controller
@RequestMapping("/courses/{courseId}/resource-requests")
public class ResourceRequestMvcController {

    private final ResourceRequestService resourceRequestService;

    @Autowired
    public ResourceRequestMvcController(ResourceRequestService resourceRequestService) {
        this.resourceRequestService = resourceRequestService;
    }

    /** POST /courses/{courseId}/resource-requests (STUDENT) */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public String createRequest(@PathVariable Long courseId,
                                @RequestParam(value = "message", required = false) String message,
                                Principal principal,
                                RedirectAttributes ra) {
        try {
            resourceRequestService.createRequest(courseId, principal.getName(), message);
            ra.addFlashAttribute("msg", "Your request was sent to the teacher.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            // keep what the student typed so they can fix it
            ra.addFlashAttribute("requestDraft", message);
        }
        return "redirect:/courses/" + courseId;
    }
}
