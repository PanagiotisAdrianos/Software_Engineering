package com.specflow.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/projects")
public class ProjectController extends BaseController {

    public String createProject() {
        // TODO: Map to UC03
        return "redirect:/projects";
    }

    public String deleteProject() {
        // TODO: Map to UC04
        return "redirect:/projects";
    }

    public String viewProjects() {
        // TODO: Map to UC05
        return "projects/list";
    }

    public String viewProjectStats() {
        // TODO: Map to UC06
        return "projects/stats";
    }

    public String shareProject() {
        // TODO: Map to UC12
        return "redirect:/projects";
    }
}
