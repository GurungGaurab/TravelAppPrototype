package com.example.TravelApp.controller;

import com.example.TravelApp.service.BudgetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;

@Controller
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/expenses")
    public String createExpense(@RequestParam Long tripId,
                                @RequestParam String category,
                                @RequestParam Double amount,
                                @RequestParam LocalDate date,
                                @RequestParam String description,
                                Principal principal) {
        budgetService.addExpenseToTrip(tripId, principal.getName(), category, amount, date, description);
        return "redirect:/trips/" + tripId;
    }
}
