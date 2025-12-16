package com.MarianFinweFeanor.Virtual_Teacher.Controller.mvc;


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

    // 1) List submissions: student sees their own, teacher sees all
    @GetMapping({"", "/"})
    public String listSubmissions(@PathVariable Long courseId,
                                  @PathVariable Long lectureId,
                                  Model model,
                                  Principal principal) {
        String email = principal != null ? principal.getName() : null;
        if (email == null) {
            return "redirect:/login";
        }

        //boolean isTeacher = /* your logic to check role, e.g., via userService or SecurityContext */;

        boolean isTeacher = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<Assignment> subs;
        if (isTeacher) {
            // teacher: show all submissions for lecture
            // you'd need a method like assignmentRepo.findByLecture_LectureId(lectureId)
            subs = assignmentService.getSubmissionsByLecture(lectureId);
            // adjust or add new service method
            model.addAttribute("submissions", subs);
        } else {
            // student: only their own
            subs = assignmentService.getSubmissionsByLectureAndUser(lectureId, email);
        }

        model.addAttribute("submissions", subs);
        model.addAttribute("courseId", courseId);
        model.addAttribute("lectureId", lectureId);

        return "assignments"; // create a Thymeleaf view for this
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
    @PreAuthorize("hasRole('TEACHER')")
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

//    //a) List all my submissions by the logged in student
//    @GetMapping("/assignments/my")
//    public String mySubmissions (Model model , Principal principal)
//    {
//        var list = assignmentService.getMySubmissions(principal.getName());
//        model.addAttribute("submission", list);
//        return "my-assignments";
//    }
}
