package se.rmdesign.crm.Models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "budget_entries")
public class BudgetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "total", nullable = false)
    private double total;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "budgetEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BudgetEntryValue> budgetValues = new ArrayList<>();

    public BudgetEntry() {
    }

    public BudgetEntry(Project project, String title, double total) {
        this.project = project;
        this.title = title;
        this.total = total;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public List<BudgetEntryValue> getBudgetValues() {
        if (budgetValues == null) {
            budgetValues = new ArrayList<>();
        }
        return budgetValues;
    }
    public void setBudgetValues(List<BudgetEntryValue> budgetValues) {
        this.budgetValues = budgetValues != null ? budgetValues : new ArrayList<>();
    }
}
