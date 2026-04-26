package com.specflow.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/usecases")
public class UseCaseController extends BaseController {

    public String createUseCase() {
        // TODO: Map to UC07
        return "redirect:/usecases";
    }

    public String updateUseCase() {
        // TODO: Map to UC08
        return "redirect:/usecases";
    }

    public String deleteUseCase() {
        // TODO: Map to UC09
        return "redirect:/usecases";
    }

    public String approveRejectUseCase() {
        // TODO: Map to UC16
        return "redirect:/usecases";
    }

    public String viewUseCases() {
        // TODO: View use cases
        return "usecases/list";
    }
}
