package com.MarianFinweFeanor.Virtual_Teacher.Controller;


import com.MarianFinweFeanor.Virtual_Teacher.Model.Assignment;
import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.AssignmentService;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.MarianFinweFeanor.Virtual_Teacher.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/lectures/{lectureId}/assignments")
public class AssignmentMvcController {

    private final AssignmentService assignmentService;
    private final UserService userService;

    @Autowired
    public AssignmentMvcController (AssignmentService assignmentService, UserService userService)
    {
        this.assignmentService = assignmentService;
        this.userService = userService;
    }

    // List submissions: student sees their own; teacher/admin see all
    @GetMapping({"", "/"})
    public String listSubmissions(@PathVariable Long courseId,
                                  @PathVariable Long lectureId,
                                  Model model,
                                  Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        String email = principal.getName();

        boolean canManageAssignments =
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_TEACHER")
                                        || authority.getAuthority().equals("ROLE_ADMIN")
                        );

        List<Assignment> submissions;

        if (canManageAssignments) {
            submissions = assignmentService.getSubmissionsByLecture(lectureId);
        } else {
            submissions =
                    assignmentService.getSubmissionsByLectureAndUser(
                            lectureId,
                            email
                    );
        }

        model.addAttribute("submissions", submissions);
        model.addAttribute("courseId", courseId);
        model.addAttribute("lectureId", lectureId);

        return "assignments";
    }

    // 2) Download a submission file
    @GetMapping("/{assignmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long assignmentId) throws IOException {
        Assignment assignment = assignmentService.findById(assignmentId);
        Path path = Paths.get(assignment.getSubmissionFilePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            throw new EntityNotFoundException("Assignment file", assignmentId);
        }
        String filename = path.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // (Optional) 3) Teacher grades an assignment
    @PostMapping("/{assignmentId}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public String grade(@PathVariable Long courseId,
                        @PathVariable Long lectureId,
                        @PathVariable Long assignmentId,
                        @RequestParam Double grade,
                        @RequestParam(required = false) String teacherComment,
                        RedirectAttributes ra) {

        assignmentService.gradeAssignment(assignmentId, grade, teacherComment);
        ra.addFlashAttribute("msg", "Assignment graded.");
        //Assignment assignment = assignmentService.findById(assignmentId);
        //assignment.setGrade(grade);
        // assuming you have save in repository via service (you might need a method)
        // e.g., assignmentService.save(assignment);
        ra.addFlashAttribute("msg", "Assignment graded.");
        return "redirect:/courses/" + courseId + "/lectures/" + lectureId + "/assignments";
    }

}
