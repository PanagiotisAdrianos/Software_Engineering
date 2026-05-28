package com.specflow.controllers;

import com.specflow.domain.Project;
import com.specflow.exceptions.InsufficientDataException;
import com.specflow.services.DiagramService;
import com.specflow.services.ProjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/projects/{projectId}/diagrams")
public class DiagramController extends BaseController {

    private final DiagramService diagramService;
    private final ProjectService projectService;

    public DiagramController(DiagramService diagramService,
                             ProjectService projectService) {
        this.diagramService = diagramService;
        this.projectService = projectService;
    }

    @GetMapping
    public String showDiagramsTab(@PathVariable Long projectId, Model model) {
        Project project = projectService.findProjectById(projectId);
        model.addAttribute("project", project);
        if (!model.containsAttribute("tool")) {
            model.addAttribute("tool", "plantuml");
        }
        return "generate-diagrams-screen";
    }

    @PostMapping("/use-case")
    public String generateUcDiagram(@PathVariable Long projectId,
                                    @RequestParam String tool,
                                    Model model) {
        Project project = projectService.findProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("tool", tool);
        try {
            String script = diagramService.generateUcScript(projectId, tool);
            model.addAttribute("script", script);
            model.addAttribute("scriptType", "Use Case Diagram");
        } catch (InsufficientDataException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "generate-diagrams-screen";
    }

    @PostMapping("/class")
    public String generateClassDiagram(@PathVariable Long projectId,
                                       @RequestParam String tool,
                                       Model model) {
        Project project = projectService.findProjectById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("tool", tool);
        try {
            String script = diagramService.generateClassScript(projectId, tool);
            model.addAttribute("script", script);
            model.addAttribute("scriptType", "Class Diagram");
        } catch (InsufficientDataException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "generate-diagrams-screen";
    }
}
