package com.example.TravelApp.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_budget")
    private Double totalBudget;

    @Column(name = "spent_amount")
    private Double spentAmount;

    @Column(name = "remaining_amount")
    private Double remainingAmount;

    @OneToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    public Budget() {
    }

    public Budget(Double totalBudget, Double spentAmount, Double remainingAmount) {
        this.totalBudget = totalBudget;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
    }

    public Long getId() {
        return id;
    }

    public Double getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(Double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public Double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(Double spentAmount) {
        this.spentAmount = spentAmount;
    }

    public Double getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(Double remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        expense.setBudget(this);
    }

    public void removeExpense(Expense expense) {
        expenses.remove(expense);
        expense.setBudget(null);
    }
}
