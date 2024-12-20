package se.rmdesign.crm.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectBudget;
import se.rmdesign.crm.Repos.BudgetRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    public void saveBudget(ProjectBudget budget) {
        budgetRepository.save(budget);
    }

    public List<ProjectBudget> getBudgetForProject(Integer projectId) {
        return budgetRepository.findByProjectIdOrderByYear(projectId);
    }

    public List<Integer> generateYearsBetweenDates(LocalDate startDate, LocalDate deadline) {
        List<Integer> years = new ArrayList<>();
        int startYear = startDate.getYear();
        int endYear = deadline.getYear();
        for (int year = startYear; year <= endYear; year++) {
            years.add(year);
        }
        System.out.println("Years Generated in Service: " + years); // Debug
        return years;
    }


    public void saveAllBudgets(List<ProjectBudget> budgets) {
        budgetRepository.saveAll(budgets);
    }
    public String formatCurrency(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("sv-SE"));
        return formatter.format(value) + " SEK";
    }
    public List<ProjectBudget> getBudgetForYear(Integer projectId, Integer year) {
        return budgetRepository.findByProjectIdAndYear(projectId, year);
    }

}
