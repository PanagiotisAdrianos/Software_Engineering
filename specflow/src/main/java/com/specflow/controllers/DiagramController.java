package com.specflow.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/diagrams")
public class DiagramController extends BaseController {

    public String generateUseCaseDiagram() {
        // TODO: Map to UC13
        return "diagrams/usecase";
    }

    public String generateClassDiagram() {
        // TODO: Map to UC13
        return "diagrams/class";
    }
}
