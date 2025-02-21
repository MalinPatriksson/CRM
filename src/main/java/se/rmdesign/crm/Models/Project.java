package se.rmdesign.crm.Models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String manager;
    private LocalDate startDate;
    private LocalDate deadline;
    private String fundingSource;
    private String researchProgram;
    private String diaryNumber;
    @Transient
    private double totalBudget;


    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectStatus> statusHistory;

    public ProjectStatus getLatestStatus() {
        return statusHistory != null && !statusHistory.isEmpty() ?
                statusHistory.get(statusHistory.size() - 1) : null;
    }


    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetEntry> budgetEntries = new ArrayList<>();

    // Constructors
    public Project() {}

    public Project(String name, String manager, LocalDate startDate, LocalDate deadline, String fundingSource, String researchProgram, String diaryNumber) {
        this.name = name;
        this.manager = manager;
        this.startDate = startDate;
        this.deadline = deadline;
        this.fundingSource = fundingSource;
        this.researchProgram = researchProgram;
        this.diaryNumber = diaryNumber;
    }

    public double getTotalBudget() {
        return budgetEntries.stream()
                .filter(entry -> "Totala intäkter".equalsIgnoreCase(entry.getTitle())) // 🔹 Filtrera på rätt typ
                .mapToDouble(BudgetEntry::getTotal) // 🔹 Använder `getTotal()` istället för att summera `values`
                .sum(); // 🔹 Summera alla totala intäkter för projektet
    }

    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }

    // Add BudgetEntry to project
    public void addBudgetEntry(BudgetEntry budgetEntry) {
        budgetEntries.add(budgetEntry);
        budgetEntry.setProject(this);
    }

    public List<BudgetEntry> getBudgetEntries() {
        return budgetEntries;
    }

    public void setBudgetEntries(List<BudgetEntry> newEntries) {
        this.budgetEntries.clear();
        if (newEntries != null) {
            this.budgetEntries.addAll(newEntries);
        }
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResearchProgram() {
        return researchProgram;
    }

    public void setResearchProgram(String researchProgram) {
        this.researchProgram = researchProgram;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getDiaryNumber() {
        return diaryNumber;
    }

    public void setDiaryNumber(String diaryNumber) {
        this.diaryNumber = diaryNumber;
    }
}
