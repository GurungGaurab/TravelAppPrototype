package com.example.TravelApp.service;

import com.example.TravelApp.model.Budget;
import com.example.TravelApp.model.Expense;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.BudgetRepository;
import com.example.TravelApp.repository.ExpenseRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final TripRepository tripRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    public BudgetService(TripRepository tripRepository,
                         BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository) {
        this.tripRepository = tripRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
    }

    public void addExpenseToTrip(Long tripId,
                                 String ownerEmail,
                                 String category,
                                 Double amount,
                                 LocalDate date,
                                 String description) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));
        addExpenseToTrip(trip, category, amount, date, description, true);
    }

    public boolean addAutomaticExpenseToTrip(Long tripId,
                                             String ownerEmail,
                                             String category,
                                             Double amount,
                                             LocalDate date,
                                             String description) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));
        return addExpenseToTrip(trip, category, amount, date, description, false);
    }

    public boolean removeAutomaticExpenseFromTrip(Long tripId,
                                                  String ownerEmail,
                                                  String category,
                                                  String description) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Budget budget = trip.getBudget();
        if (budget == null || budget.getExpenses() == null || budget.getExpenses().isEmpty()) {
            return false;
        }

        Expense expense = budget.getExpenses().stream()
                .filter(candidate -> sameText(candidate.getCategory(), category) && sameText(candidate.getDescription(), description))
                .findFirst()
                .orElse(null);
        if (expense == null) {
            return false;
        }

        budget.removeExpense(expense);
        expenseRepository.delete(expense);
        recalculateBudget(budget);
        tripRepository.save(trip);
        budgetRepository.save(budget);
        return true;
    }

    public void cleanupDuplicateExpensesForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Budget budget = trip.getBudget();
        if (budget == null || budget.getExpenses() == null || budget.getExpenses().isEmpty()) {
            return;
        }

        List<Expense> snapshot = new ArrayList<>(budget.getExpenses());
        Set<String> seen = new HashSet<>();

        for (Expense expense : snapshot) {
            String key = expenseKey(expense);
            if (!seen.add(key)) {
                budget.removeExpense(expense);
                expenseRepository.delete(expense);
            }
        }

        recalculateBudget(budget);
        tripRepository.save(trip);
        budgetRepository.save(budget);
    }

    private boolean addExpenseToTrip(Trip trip,
                                     String category,
                                     Double amount,
                                     LocalDate date,
                                     String description,
                                     boolean notify) {
        Budget budget = trip.getBudget();
        if (budget == null) {
            Double totalBudget = trip.getTotalBudget() != null ? trip.getTotalBudget() : 0.0;
            budget = new Budget(totalBudget, 0.0, totalBudget);
            trip.setBudget(budget);
        }

        double safeAmount = amount != null ? amount : 0.0;
        if (safeAmount <= 0) {
            return false;
        }

        boolean alreadyExists = budget.getExpenses().stream()
                .anyMatch(expense -> sameText(expense.getCategory(), category) && sameText(expense.getDescription(), description));
        if (alreadyExists) {
            return false;
        }

        Expense expense = new Expense(category, safeAmount, date != null ? date : LocalDate.now(), description);
        budget.addExpense(expense);
        recalculateBudget(budget);

        tripRepository.save(trip);
        budgetRepository.save(budget);
        return true;
    }

    private void recalculateBudget(Budget budget) {
        double totalBudget = budget.getTotalBudget() != null ? budget.getTotalBudget() : 0.0;
        double spent = budget.getExpenses().stream()
                .mapToDouble(expense -> expense.getAmount() != null ? expense.getAmount() : 0.0)
                .sum();
        budget.setSpentAmount(spent);
        budget.setRemainingAmount(totalBudget - spent);
    }

    private boolean sameText(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String expenseKey(Expense expense) {
        String category = expense.getCategory() == null ? "" : expense.getCategory().trim().toLowerCase();
        String description = expense.getDescription() == null ? "" : expense.getDescription().trim().toLowerCase();
        String date = expense.getDate() == null ? "" : expense.getDate().toString();
        String amount = expense.getAmount() == null ? "" : String.format("%.2f", expense.getAmount());
        return category + "|" + amount + "|" + date + "|" + description;
    }

    public double getSpentPercentage(Budget budget) {
        if (budget == null || budget.getTotalBudget() == null || budget.getTotalBudget() <= 0) {
            return 0.0;
        }
        double spent = getDisplaySpentAmount(budget);
        return Math.min(100.0, (spent / budget.getTotalBudget()) * 100.0);
    }

    public boolean isOverBudget(Budget budget) {
        if (budget == null) {
            return false;
        }
        double remaining = getDisplayRemainingAmount(budget);
        return remaining < 0;
    }

    public List<CategorySpend> getCategorySpend(Budget budget) {
        List<Expense> expenses = getUniqueExpenses(budget);
        if (expenses.isEmpty()) {
            return List.of();
        }

        Map<String, Double> totals = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory() == null || expense.getCategory().isBlank() ? "Other" : expense.getCategory(),
                        Collectors.summingDouble(expense -> expense.getAmount() != null ? expense.getAmount() : 0.0)
                ));

        return totals.entrySet().stream()
                .map(entry -> new CategorySpend(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategorySpend::amount).reversed())
                .toList();
    }

    public List<Expense> getUniqueExpenses(Budget budget) {
        if (budget == null || budget.getExpenses() == null || budget.getExpenses().isEmpty()) {
            return List.of();
        }

        List<Expense> uniqueExpenses = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Expense expense : budget.getExpenses()) {
            String key = expenseKey(expense);
            if (seen.add(key)) {
                uniqueExpenses.add(expense);
            }
        }
        return uniqueExpenses;
    }

    public double getDisplaySpentAmount(Budget budget) {
        return getUniqueExpenses(budget).stream()
                .mapToDouble(expense -> expense.getAmount() != null ? expense.getAmount() : 0.0)
                .sum();
    }

    public double getDisplayRemainingAmount(Budget budget) {
        if (budget == null) {
            return 0.0;
        }
        double totalBudget = budget.getTotalBudget() != null ? budget.getTotalBudget() : 0.0;
        return totalBudget - getDisplaySpentAmount(budget);
    }

    public record CategorySpend(String category, Double amount) {
    }
}
