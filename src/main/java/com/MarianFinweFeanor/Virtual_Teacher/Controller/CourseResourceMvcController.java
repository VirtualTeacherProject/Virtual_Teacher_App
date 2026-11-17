package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.CourseResourceService;
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
 * SCRUM-34: teachers and admins manage a course's additional resources.
 * The resources are shown on the course page (EnrollmentMvcController.courseDetail).
 */
@Controller
@RequestMapping("/courses/{courseId}/resources")
public class CourseResourceMvcController {

    private final CourseResourceService courseResourceService;

    @Autowired
    public CourseResourceMvcController(CourseResourceService courseResourceService) {
        this.courseResourceService = courseResourceService;
    }

    /** POST /courses/{courseId}/resources (TEACHER/ADMIN) */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String addResource(@PathVariable Long courseId,
                              @RequestParam("title") String title,
                              @RequestParam("url") String url,
                              @RequestParam(value = "description", required = false) String description,
                              Principal principal,
                              RedirectAttributes ra) {
        try {
            courseResourceService.addResource(courseId, principal.getName(), title, url, description);
            ra.addFlashAttribute("msg", "Resource added.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    /** POST /courses/{courseId}/resources/{resourceId}/delete (TEACHER/ADMIN) */
    @PostMapping("/{resourceId}/delete")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String deleteResource(@PathVariable Long courseId,
                                 @PathVariable Long resourceId,
                                 Principal principal,
                                 RedirectAttributes ra) {
        courseResourceService.deleteResource(courseId, resourceId, principal.getName());
        ra.addFlashAttribute("msg", "Resource removed.");
        return "redirect:/courses/" + courseId;
    }
}
