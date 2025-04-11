package se.rmdesign.crm.Models;

import jakarta.persistence.*;

@Entity
public class BudgetEntryValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "budget_entry_id", nullable = false)
    private BudgetEntry budgetEntry;

    private int year;

    @Column(nullable = false)
    private double value;

    public BudgetEntryValue() {}

    public BudgetEntryValue(BudgetEntry budgetEntry, int year, double value) {
        this.budgetEntry = budgetEntry;
        this.year = year;
        this.value = value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public void setBudgetEntry(BudgetEntry budgetEntry) { this.budgetEntry = budgetEntry; }

    public int getYear() { return year; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

}
