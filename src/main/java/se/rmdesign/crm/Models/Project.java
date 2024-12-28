package se.rmdesign.crm.Models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // Unikt projekt-ID
    private String name; // Projektets namn
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    private String manager; // Ansvarig person
    private LocalDate deadline; // Deadline för projektet
    private int budgetYear1; // Budget för år 1
    private int budgetYear2; // Budget för år 2
    private int budgetYear3; // Budget för år 3
    private int spent; // Totalt spenderat på projektet

    private String fundingSource; // Nytt fält: Finansiär
    private String researchProgram; // Nytt fält: Forskningsprogram

    public Project() {
    }

    public Project(
            int id,
            String name,
            String manager,
            LocalDate deadline,
            int budgetYear1,
            int budgetYear2,
            int budgetYear3,
            int spent,
            LocalDate startDate,
            String fundingSource,
            String researchProgram
    ) {
        this.id = id;
        this.name = name;
        this.manager = manager;
        this.deadline = deadline;
        this.budgetYear1 = budgetYear1;
        this.budgetYear2 = budgetYear2;
        this.budgetYear3 = budgetYear3;
        this.spent = spent;
        this.startDate = startDate;
        this.fundingSource = fundingSource;
        this.researchProgram = researchProgram;
    }

    // Getter och Setter för startDate
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    // Getters och Setters för övriga fält
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public int getBudgetYear1() {
        return budgetYear1;
    }

    public void setBudgetYear1(int budgetYear1) {
        this.budgetYear1 = budgetYear1;
    }

    public int getBudgetYear2() {
        return budgetYear2;
    }

    public void setBudgetYear2(int budgetYear2) {
        this.budgetYear2 = budgetYear2;
    }

    public int getBudgetYear3() {
        return budgetYear3;
    }

    public void setBudgetYear3(int budgetYear3) {
        this.budgetYear3 = budgetYear3;
    }

    public int getSpent() {
        return spent;
    }

    public void setSpent(int spent) {
        this.spent = spent;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public String getResearchProgram() {
        return researchProgram;
    }

    public void setResearchProgram(String researchProgram) {
        this.researchProgram = researchProgram;
    }
}
