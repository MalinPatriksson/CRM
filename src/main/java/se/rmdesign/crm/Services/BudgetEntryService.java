package se.rmdesign.crm.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Repos.BudgetEntryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BudgetEntryService {
    private final BudgetEntryRepository budgetEntryRepository;

    public BudgetEntryService(BudgetEntryRepository budgetEntryRepository) {
        this.budgetEntryRepository = budgetEntryRepository;
    }

    @Transactional
    public void deleteBudgetEntriesForProject(Long projectId) {
        System.out.println("🗑️ Raderar budgetrader för projekt ID: " + projectId);
        budgetEntryRepository.deleteByProjectId(projectId);
    }

    public List<BudgetEntry> processBudgetEntries(Map<String, String> budgetRows, Project project) {
        List<BudgetEntry> budgetEntries = new ArrayList<>();
        Map<String, Map<Integer, Double>> groupedBudgetEntries = new HashMap<>();

        System.out.println("🔍 DEBUG - Inkommande budgetRows:");
        budgetRows.forEach((key, value) -> System.out.println("   ➝ " + key + " = " + value));

        Pattern pattern = Pattern.compile("budgetRows\\[(\\d+|__[^]]+)]\\[([^]]+)]");

        budgetRows.forEach((key, value) -> {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                String rowIndex = matcher.group(1);
                String columnName = matcher.group(2);

                String titleKey = "budgetRows[" + rowIndex + "][Rubrik]";
                String title = budgetRows.get(titleKey);

                if (title != null && !columnName.equals("Rubrik") && !value.isEmpty()) {
                    try {
                        double amount = Double.parseDouble(value.replaceAll("[^\\d.]", ""));
                        int year = columnName.equals("Total") ? -1 : Integer.parseInt(columnName);

                        groupedBudgetEntries.computeIfAbsent(title, k -> new HashMap<>()).put(year, amount);
                        System.out.println("✅ Upptäckt data: " + title + " [" + columnName + "] = " + amount);

                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ Ogiltigt värde för " + title + " [" + columnName + "] = " + value);
                    }
                }
            }
        });

        System.out.println("📌 Samlade budgetrader:");
        groupedBudgetEntries.forEach((title, values) -> {
            if (values.isEmpty()) {
                System.out.println("⚠️ Ignorerar tom budgetrad: " + title);
                return;
            }

            System.out.println("   ➝ Titel: " + title);
            values.forEach((year, amount) -> System.out.println("      År: " + year + " -> " + amount));

            double total = values.getOrDefault(-1, 0.0);
            values.remove(-1);

            BudgetEntry entry = new BudgetEntry(project, title, values);
            budgetEntries.add(entry);
        });

        System.out.println("✅ Budgetrader att spara: " + budgetEntries.size());
        return budgetEntryRepository.saveAll(budgetEntries);
    }



    @Transactional
    public void saveAll(List<BudgetEntry> budgetEntries) {
        if (budgetEntries.isEmpty()) {
            System.out.println("⚠️ Inga budgetrader att spara.");
            return;
        }

        System.out.println("📌 Sparar budgetrader i databasen...");
        budgetEntries.forEach(entry -> {
            System.out.println("   ➝ " + entry.getTitle() + " (Total: " + entry.getTotal() + ")");
            entry.getValues().forEach((year, value) ->
                    System.out.println("      År: " + year + ", Belopp: " + value)
            );
        });

        budgetEntryRepository.saveAll(budgetEntries);
        System.out.println("✅ Alla budgetrader har sparats i databasen.");
    }

    public List<BudgetEntry> findAll() {
        return budgetEntryRepository.findAll();
    }

    public List<BudgetEntry> findByProject(Project project) {
        return budgetEntryRepository.findByProject(project);
    }
}
