package com.specflow.controllers;

import com.specflow.domain.Actor;
import com.specflow.domain.Project;
import com.specflow.domain.UseCase;
import com.specflow.domain.User;
import com.specflow.dto.UseCaseDto;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ActorRepository;
import com.specflow.services.CommentService;
import com.specflow.services.ProjectService;
import com.specflow.services.UseCaseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects/{projectId}/usecases")
public class UseCaseController extends BaseController {

    private final UseCaseService useCaseService;
    private final ProjectService projectService;
    private final ActorRepository actorRepository;
    private final CommentService commentService;

    public UseCaseController(UseCaseService useCaseService,
                             ProjectService projectService,
                             ActorRepository actorRepository,
                             CommentService commentService) {
        this.useCaseService = useCaseService;
        this.projectService = projectService;
        this.actorRepository = actorRepository;
        this.commentService = commentService;
    }

    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProjectById(projectId);
        List<Actor> actors = actorRepository.findAllByProject(project);
        if (!model.containsAttribute("useCaseDto")) {
            model.addAttribute("useCaseDto", new UseCaseDto());
        }
        model.addAttribute("project", project);
        model.addAttribute("actors", actors);
        return "use-case-form";
    }

    @PostMapping
    public String submitCreateForm(@PathVariable Long projectId,
                                   @ModelAttribute("useCaseDto") UseCaseDto dto,
                                   @AuthenticationPrincipal User currentUser,
                                   RedirectAttributes ra) {
        try {
            Long id = useCaseService.createUseCase(projectId, dto, currentUser);
            ra.addFlashAttribute("successMessage", "Use Case saved successfully!");
            return "redirect:/projects/" + projectId + "/usecases/" + id;
        } catch (ValidationException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("useCaseDto", dto);
            return "redirect:/projects/" + projectId + "/usecases/new";
        }
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long projectId,
                             @PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        Project project = projectService.findProjectById(projectId);
        UseCase useCase = useCaseService.findUseCaseById(id);
        model.addAttribute("project", project);
        model.addAttribute("useCase", useCase);
        model.addAttribute("comments",
                commentService.findCommentsForTarget(CommentService.TARGET_USE_CASE, id));
        model.addAttribute("targetType", CommentService.TARGET_USE_CASE);
        model.addAttribute("targetId", id);
        model.addAttribute("currentUserId", currentUser != null ? currentUser.getId() : null);
        return "use-case-detail-screen";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long projectId,
                               @PathVariable Long id,
                               Model model) {
        Project project = projectService.findProjectById(projectId);
        UseCase useCase = useCaseService.findUseCaseById(id);
        List<Actor> actors = actorRepository.findAllByProject(project);
        if (!model.containsAttribute("useCaseDto")) {
            UseCaseDto dto = new UseCaseDto();
            dto.setName(useCase.getName());
            dto.setPrecondition(useCase.getPrecondition());
            dto.setMainFlow(useCase.getMainFlow());
            dto.setAlternativeFlow(useCase.getAlternativeFlow());
            dto.setPostcondition(useCase.getPostcondition());
            dto.setActorIds(useCase.getActors().stream().map(a -> a.getId()).toList());
            model.addAttribute("useCaseDto", dto);
        }
        model.addAttribute("project", project);
        model.addAttribute("useCase", useCase);
        model.addAttribute("actors", actors);
        return "edit-use-case-form";
    }

    @PostMapping("/{id}")
    public String submitEditForm(@PathVariable Long projectId,
                                 @PathVariable Long id,
                                 @ModelAttribute("useCaseDto") UseCaseDto dto,
                                 RedirectAttributes ra) {
        try {
            useCaseService.updateUseCase(id, dto);
            ra.addFlashAttribute("successMessage", "Changes saved successfully!");
            return "redirect:/projects/" + projectId + "/usecases/" + id;
        } catch (ValidationException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("useCaseDto", dto);
            return "redirect:/projects/" + projectId + "/usecases/" + id + "/edit";
        }
    }

    @GetMapping("/{id}/delete-confirm")
    public String showDeleteConfirmation(@PathVariable Long projectId,
                                         @PathVariable Long id,
                                         Model model) {
        Project project = projectService.findProjectById(projectId);
        UseCase useCase = useCaseService.findUseCaseById(id);
        model.addAttribute("project", project);
        model.addAttribute("useCase", useCase);
        return "confirm-delete-usecase";
    }

    @PostMapping("/{id}/delete")
    public String confirmDelete(@PathVariable Long projectId,
                                @PathVariable Long id,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes ra) {
        useCaseService.deleteUseCase(id, currentUser);
        ra.addFlashAttribute("successMessage", "Use Case deleted successfully.");
        return "redirect:/projects/" + projectId;
    }

    // ===== UC16: Approve / Reject =====

    @GetMapping("/{id}/reply-approval")
    public String showReplyApproval(@PathVariable Long projectId,
                                    @PathVariable Long id,
                                    Model model) {
        Project project = projectService.findProjectById(projectId);
        UseCase useCase = useCaseService.findUseCaseById(id);
        model.addAttribute("project", project);
        model.addAttribute("useCase", useCase);
        return "reply-approval-dialog";
    }

    @PostMapping("/{id}/approve")
    public String approveUseCase(@PathVariable Long projectId,
                                 @PathVariable Long id,
                                 RedirectAttributes ra) {
        useCaseService.approve(id);
        ra.addFlashAttribute("successMessage", "Use Case approved");
        return "redirect:/projects/" + projectId + "/usecases/" + id;
    }

    @GetMapping("/{id}/reject-dialog")
    public String openRejectionDialog(@PathVariable Long projectId,
                                      @PathVariable Long id,
                                      Model model) {
        Project project = projectService.findProjectById(projectId);
        UseCase useCase = useCaseService.findUseCaseById(id);
        model.addAttribute("project", project);
        model.addAttribute("useCase", useCase);
        return "rejection-reason-dialog";
    }

    @PostMapping("/{id}/reject")
    public String rejectUseCase(@PathVariable Long projectId,
                                @PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes ra) {
        useCaseService.reject(id, reason);
        ra.addFlashAttribute("successMessage", "Use Case rejected");
        return "redirect:/projects/" + projectId + "/usecases/" + id;
    }
}
