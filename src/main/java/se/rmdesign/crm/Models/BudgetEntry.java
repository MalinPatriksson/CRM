package se.rmdesign.crm.Models;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Entity
public class BudgetEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(name = "total", nullable = false)
    private double total;

    @ElementCollection
    @CollectionTable(name = "budget_entry_values", joinColumns = @JoinColumn(name = "budget_entry_id"))
    @MapKeyColumn(name = "year")
    @Column(name = "value", nullable = false)
    private Map<Integer, Double> values = new HashMap<>();

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // 🔹 Konstruktorer
    public BudgetEntry() {
        this.values = new HashMap<>();
    }

    public BudgetEntry(Project project, String title, Map<Integer, Double> values) {
        this.project = project;
        this.title = title;
        this.values = (values != null) ? values : new HashMap<>();
    }

    // 🔹 Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Map<Integer, Double> getValues() { return values; }
    public void setValues(Map<Integer, Double> values) {
        this.values = (values != null) ? values : new HashMap<>();
    }

    // 🔹 Dynamisk totalberäkning
    public double getTotal() {
        return total;
    }

    // 🔹 Hämta budget för ett specifikt år
    public double getBudgetForYear(int year) {
        return values.getOrDefault(year, 0.0);
    }
    // 🔹 Hämta alla år där det finns budgetdata
    public Set<Integer> getYears() {
        return values.keySet();
    }
}
