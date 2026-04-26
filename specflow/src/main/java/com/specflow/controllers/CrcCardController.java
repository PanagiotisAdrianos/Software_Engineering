package com.specflow.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/crccards")
public class CrcCardController extends BaseController {

    public String createCrcCard() {
        // TODO: Map to UC10
        return "redirect:/crccards";
    }

    public String updateCrcCard() {
        // TODO: Map to UC11
        return "redirect:/crccards";
    }

    public String deleteCrcCard() {
        // TODO: Delete CRC card
        return "redirect:/crccards";
    }

    public String viewCrcCards() {
        // TODO: View CRC cards
        return "crccards/list";
    }
}
