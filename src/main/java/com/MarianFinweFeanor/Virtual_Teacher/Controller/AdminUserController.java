package com.MarianFinweFeanor.Virtual_Teacher.Controller;

import com.MarianFinweFeanor.Virtual_Teacher.Model.User;
import com.MarianFinweFeanor.Virtual_Teacher.Model.UserRole;
import com.MarianFinweFeanor.Virtual_Teacher.Service.Interfaces.UserService;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    // list all users
    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    // make user a teacher
    @PostMapping("/{id}/make-teacher")
    public String makeTeacher(@PathVariable Long id, RedirectAttributes ra) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(UserRole.TEACHER);
        userService.updateUser( user);

        ra.addFlashAttribute("msg", "User promoted to TEACHER.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/make-admin")
    public String makeAdmin(@PathVariable Long id, RedirectAttributes ra) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(UserRole.ADMIN);
        userService.updateUser(user);

        ra.addFlashAttribute("msg", "User promoted to ADMIN.");
        return "redirect:/admin/users";
    }

    // make user student again
    @PostMapping("/{id}/make-student")
    public String makeStudent(@PathVariable Long id, RedirectAttributes ra) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(UserRole.STUDENT);
        userService.updateUser(user);

        ra.addFlashAttribute("msg", "User changed to STUDENT.");
        return "redirect:/admin/users";
    }



    // GET /admin/users/{id}/edit
    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "admin/user-edit";
    }

    // POST /admin/users/{id}/edit
    @PostMapping("/{id}/edit")
    public String submitEditUser(@PathVariable Long id,
                                 @Valid @ModelAttribute("user") User formUser,
                                 BindingResult br,
                                 RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "admin/user-edit";
        }

        User existing = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // copy editable fields from form into existing entity
        existing.setFirstName(formUser.getFirstName());
        existing.setLastName(formUser.getLastName());
        existing.setEmail(formUser.getEmail());
        existing.setRole(formUser.getRole());
        existing.setStatus(formUser.getStatus());

        // reuse your saveUser (with create/update logic fixed as we discussed)
        userService.updateUser(existing);

        ra.addFlashAttribute("msg", "User updated.");
        return "redirect:/admin/users";
    }

    // delete user (optional: protect against deleting last admin)
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra,
                             @AuthenticationPrincipal UserDetails currentUser) {
        // optional: don't let admin delete themselves
        // optional: don't delete if this is last ADMIN
        User existing = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        //userService.deleteUserById(id);

        User current = userService.findByEmail(currentUser.getUsername());
        if (current.getUserId().equals(existing.getUserId())) {
            ra.addFlashAttribute("error", "You can't delete your own account.");
            return "redirect:/admin/users";
        }

        // Prevent admins deleting admins
        if (existing.getRole() == UserRole.ADMIN) {
            ra.addFlashAttribute("error", "Admins can't delete other admins.");
            return "redirect:/admin/users";
        }

        userService.deleteUserById(id);

        ra.addFlashAttribute("msg", "User deleted.");
        return "redirect:/admin/users";
    }

    // GET /admin/users/pending-teachers
    @GetMapping("/pending-teachers")
    public String listPendingTeachers(Model model) {
        model.addAttribute("pendingTeachers", userService.findPendingTeachers());
        return "admin/pending-teachers";
    }

    // POST /admin/users/{id}/approve-teacher
    @PostMapping("/{id}/approve-teacher")
    public String approveTeacher(@PathVariable Long id, RedirectAttributes ra) {
        userService.approveTeacher(id);
        ra.addFlashAttribute("msg", "Teacher approved successfully.");
        return "redirect:/admin/users/pending-teachers";
    }




}

