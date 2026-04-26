package com.specflow.controllers;

import org.springframework.ui.Model;

import java.security.Principal;

/**
 * Base abstract controller providing common methods and attributes
 * for all web controllers in the application.
 */
public abstract class BaseController {

    /**
     * Adds common attributes to the model that are needed across all pages.
     */
    protected void addCommonAttributes(Model model, Principal principal, String pageTitle) {
        model.addAttribute("pageTitle", pageTitle);
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
    }
}
