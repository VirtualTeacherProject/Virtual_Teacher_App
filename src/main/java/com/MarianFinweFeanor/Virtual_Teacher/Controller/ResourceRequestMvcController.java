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
 * SCRUM-36: teachers set a request's status and assign it to another enrolled
 * student, who answers it.
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

    /** POST /courses/{courseId}/resource-requests/{requestId}/status (TEACHER/ADMIN) */
    @PostMapping("/{requestId}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String updateStatus(@PathVariable Long courseId,
                               @PathVariable Long requestId,
                               @RequestParam(value = "status", required = false) String status,
                               Principal principal,
                               RedirectAttributes ra) {
        try {
            resourceRequestService.updateStatus(courseId, requestId, status, principal.getName());
            ra.addFlashAttribute("msg", "Request status updated.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    /**
     * POST /courses/{courseId}/resource-requests/{requestId}/assign (TEACHER/ADMIN)
     * An empty assigneeId removes the assignment.
     */
    @PostMapping("/{requestId}/assign")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String assign(@PathVariable Long courseId,
                         @PathVariable Long requestId,
                         @RequestParam(value = "assigneeId", required = false) Long assigneeId,
                         Principal principal,
                         RedirectAttributes ra) {
        try {
            resourceRequestService.assign(courseId, requestId, assigneeId, principal.getName());
            ra.addFlashAttribute("msg", assigneeId == null
                    ? "Assignment removed." : "Request assigned.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    /** POST /courses/{courseId}/resource-requests/{requestId}/response (STUDENT, the assignee) */
    @PostMapping("/{requestId}/response")
    @PreAuthorize("hasRole('STUDENT')")
    public String respond(@PathVariable Long courseId,
                          @PathVariable Long requestId,
                          @RequestParam(value = "response", required = false) String response,
                          Principal principal,
                          RedirectAttributes ra) {
        try {
            resourceRequestService.respond(courseId, requestId, principal.getName(), response);
            ra.addFlashAttribute("msg", "Your answer was saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("responseDraftId", requestId);
            ra.addFlashAttribute("responseDraft", response);
        }
        return "redirect:/courses/" + courseId;
    }
}
