package com.specflow.controllers;

import com.specflow.domain.Participant;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.dto.ProjectDto;
import com.specflow.exceptions.AlreadyParticipantException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.UserNotFoundException;
import com.specflow.exceptions.ValidationException;
import com.specflow.services.CrcCardService;
import com.specflow.services.ProjectService;
import com.specflow.services.UseCaseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects")
public class ProjectController extends BaseController {

    private final ProjectService projectService;
    private final UseCaseService useCaseService;
    private final CrcCardService crcCardService;

    public ProjectController(ProjectService projectService,
                             UseCaseService useCaseService,
                             CrcCardService crcCardService) {
        this.projectService = projectService;
        this.useCaseService = useCaseService;
        this.crcCardService = crcCardService;
    }

    @GetMapping
    public String listProjects(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("projects", projectService.findAllByOwner(currentUser));
        return "project-list-screen";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("projectDto")) {
            model.addAttribute("projectDto", new ProjectDto());
        }
        return "create-project-form";
    }

    @PostMapping
    public String submitCreateForm(@ModelAttribute("projectDto") ProjectDto dto,
                                   @AuthenticationPrincipal User currentUser,
                                   RedirectAttributes ra) {
        try {
            Long id = projectService.createProject(dto, currentUser);
            ra.addFlashAttribute("successMessage", "Project created successfully!");
            return "redirect:/projects/" + id;
        } catch (ValidationException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("projectDto", dto);
            return "redirect:/projects/new";
        }
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long id, Model model) {
        Project project = projectService.findProjectById(id);
        model.addAttribute("project", project);
        model.addAttribute("useCases", useCaseService.findAllByProject(project));
        model.addAttribute("crcCards", crcCardService.findAllByProject(project));
        return "project-detail-screen";
    }

    @GetMapping("/{id}/delete-confirm")
    public String showDeleteConfirmation(@PathVariable Long id, Model model) {
        Project project = projectService.findProjectById(id);
        model.addAttribute("project", project);
        return "confirm-delete-project";
    }

    @PostMapping("/{id}/delete")
    public String confirmDelete(@PathVariable Long id,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes ra) {
        try {
            projectService.deleteProject(id, currentUser);
            ra.addFlashAttribute("successMessage", "Project deleted successfully");
            return "redirect:/projects";
        } catch (UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/projects/" + id;
        }
    }

    // ===== UC12: Project Sharing =====

    @GetMapping("/{projectId}/participants")
    public String showParticipants(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProjectById(projectId);
        List<Participant> participants = projectService.findParticipantsByProject(projectId);
        model.addAttribute("project", project);
        model.addAttribute("participants", participants);
        return "participants-screen";
    }

    @GetMapping("/{projectId}/participants/new")
    public String showAddParticipantForm(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProjectById(projectId);
        model.addAttribute("project", project);
        if (!model.containsAttribute("participantUsername")) {
            model.addAttribute("participantUsername", "");
        }
        if (!model.containsAttribute("participantRole")) {
            model.addAttribute("participantRole", Role.COLLABORATOR.name());
        }
        return "participant-form";
    }

    @PostMapping("/{projectId}/participants")
    public String addParticipant(@PathVariable Long projectId,
                                 @RequestParam String username,
                                 @RequestParam Role role,
                                 RedirectAttributes ra) {
        try {
            projectService.addParticipant(projectId, username, role);
            ra.addFlashAttribute("successMessage", "Participant added successfully");
            return "redirect:/projects/" + projectId + "/participants";
        } catch (UserNotFoundException | AlreadyParticipantException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("participantUsername", username);
            ra.addFlashAttribute("participantRole", role != null ? role.name() : Role.COLLABORATOR.name());
            return "redirect:/projects/" + projectId + "/participants/new";
        }
    }

    @GetMapping("/{projectId}/participants/{participantId}/delete-confirm")
    public String confirmRemove(@PathVariable Long projectId,
                                @PathVariable Long participantId,
                                Model model) {
        Project project = projectService.findProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("participantId", participantId);
        return "confirm-remove-participant";
    }

    @PostMapping("/{projectId}/participants/{participantId}/delete")
    public String removeParticipant(@PathVariable Long projectId,
                                    @PathVariable Long participantId,
                                    RedirectAttributes ra) {
        projectService.removeParticipant(projectId, participantId);
        ra.addFlashAttribute("successMessage", "Participant removed successfully");
        return "redirect:/projects/" + projectId + "/participants";
    }
}
