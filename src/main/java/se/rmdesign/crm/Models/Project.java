package se.rmdesign.crm.Models;

import jakarta.persistence.*;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    @ElementCollection
    @CollectionTable(
            name = "project_academies",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "academy")
    private List<String> academies = new ArrayList<>();


    @Column(nullable = false)
    private String currentStatus = "Idé";

    private LocalDate statusDate = LocalDate.now();
    private LocalDate expectedResponseDate;

    @Transient
    private double totalBudget;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectStatus> statusHistory;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetEntry> budgetEntries = new ArrayList<>();

    public ProjectStatus getLatestStatus() {
        return statusHistory != null && !statusHistory.isEmpty() ?
                statusHistory.get(statusHistory.size() - 1) : null;
    }

    public String getFormattedTotalBudget() {
        NumberFormat format = NumberFormat.getInstance(new Locale("sv", "SE"));
        return format.format(getTotalBudget()) + " SEK";
    }

    public String getCurrentStatus() {
        if (statusHistory != null && !statusHistory.isEmpty()) {
            return statusHistory.get(statusHistory.size() - 1).getStatus();
        }
        return "Ej påbörjat";
    }

    public double getTotalBudget() {
        return budgetEntries.stream()
                .filter(entry -> "Totala intäkter".equalsIgnoreCase(entry.getTitle()))
                .mapToDouble(BudgetEntry::getTotal)
                .sum();
    }

    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public List<String> getAcademies() {
        return academies;
    }

    public void setAcademies(List<String> academies) {
        this.academies = academies;
    }

    public LocalDate getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(LocalDate statusDate) {
        this.statusDate = statusDate;
    }

    public LocalDate getExpectedResponseDate() {
        return expectedResponseDate;
    }

    public void setExpectedResponseDate(LocalDate expectedResponseDate) {
        this.expectedResponseDate = expectedResponseDate;
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

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public List<BudgetEntry> getBudgetEntries() {
        return budgetEntries;
    }

}
