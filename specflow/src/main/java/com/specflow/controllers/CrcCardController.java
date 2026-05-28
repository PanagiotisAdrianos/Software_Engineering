package com.specflow.controllers;

import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.domain.User;
import com.specflow.dto.CrcCardDto;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.services.CommentService;
import com.specflow.services.CrcCardService;
import com.specflow.services.ProjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}/crccards")
public class CrcCardController extends BaseController {

    private final CrcCardService crcCardService;
    private final ProjectService projectService;
    private final CommentService commentService;

    public CrcCardController(CrcCardService crcCardService,
                             ProjectService projectService,
                             CommentService commentService) {
        this.crcCardService = crcCardService;
        this.projectService = projectService;
        this.commentService = commentService;
    }

    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProjectById(projectId);
        if (!model.containsAttribute("crcCardDto")) {
            model.addAttribute("crcCardDto", new CrcCardDto());
        }
        model.addAttribute("project", project);
        return "crc-card-form";
    }

    @PostMapping
    public String submitCreateForm(@PathVariable Long projectId,
                                   @ModelAttribute("crcCardDto") CrcCardDto dto,
                                   RedirectAttributes ra) {
        try {
            Long id = crcCardService.createCrcCard(projectId, dto);
            ra.addFlashAttribute("successMessage", "CRC Card saved successfully!");
            return "redirect:/projects/" + projectId + "/crccards/" + id;
        } catch (ValidationException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("crcCardDto", dto);
            return "redirect:/projects/" + projectId + "/crccards/new";
        }
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long projectId,
                             @PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             Model model) {
        Project project = projectService.findProjectById(projectId);
        CrcCard crcCard = crcCardService.findCrcCardById(id);
        model.addAttribute("project", project);
        model.addAttribute("crcCard", crcCard);
        model.addAttribute("comments",
                commentService.findCommentsForTarget(CommentService.TARGET_CRC_CARD, id));
        model.addAttribute("targetType", CommentService.TARGET_CRC_CARD);
        model.addAttribute("targetId", id);
        model.addAttribute("currentUserId", currentUser != null ? currentUser.getId() : null);
        return "crc-card-detail-screen";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long projectId,
                               @PathVariable Long id,
                               Model model) {
        Project project = projectService.findProjectById(projectId);
        CrcCard crcCard = crcCardService.findCrcCardById(id);
        if (!model.containsAttribute("crcCardDto")) {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName(crcCard.getClassName());
            dto.setResponsibilities(crcCard.getResponsibilities());
            dto.setCollaborations(crcCard.getCollaborations());
            model.addAttribute("crcCardDto", dto);
        }
        model.addAttribute("project", project);
        model.addAttribute("crcCard", crcCard);
        return "edit-crc-card-form";
    }

    @PostMapping("/{id}")
    public String submitEditForm(@PathVariable Long projectId,
                                 @PathVariable Long id,
                                 @ModelAttribute("crcCardDto") CrcCardDto dto,
                                 RedirectAttributes ra) {
        try {
            crcCardService.updateCrcCard(id, dto);
            ra.addFlashAttribute("successMessage", "Changes saved successfully");
            return "redirect:/projects/" + projectId + "/crccards/" + id;
        } catch (ValidationException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            ra.addFlashAttribute("crcCardDto", dto);
            return "redirect:/projects/" + projectId + "/crccards/" + id + "/edit";
        }
    }

    @GetMapping("/{id}/delete-confirm")
    public String showDeleteConfirmation(@PathVariable Long projectId,
                                         @PathVariable Long id,
                                         Model model) {
        Project project = projectService.findProjectById(projectId);
        CrcCard crcCard = crcCardService.findCrcCardById(id);
        model.addAttribute("project", project);
        model.addAttribute("crcCard", crcCard);
        return "confirm-delete-crccard";
    }

    @PostMapping("/{id}/delete")
    public String confirmDelete(@PathVariable Long projectId,
                                @PathVariable Long id,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes ra) {
        try {
            crcCardService.deleteCrcCard(id, currentUser);
            ra.addFlashAttribute("successMessage", "CRC Card deleted successfully");
            return "redirect:/projects/" + projectId;
        } catch (UnauthorizedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/projects/" + projectId + "/crccards/" + id;
        }
    }
}
