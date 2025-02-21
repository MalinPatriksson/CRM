package se.rmdesign.crm.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Services.BudgetEntryService;
import se.rmdesign.crm.Services.ProjectService;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/budget")
public class BudgetController {

    private final ProjectService projectService;
    private final BudgetEntryService budgetEntryService;

    public BudgetController(ProjectService projectService, BudgetEntryService budgetEntryService) {
        this.projectService = projectService;
        this.budgetEntryService = budgetEntryService;
    }

    @GetMapping
    public String showBudget(@RequestParam(required = false) List<Integer> years,
                             @RequestParam(required = false) List<String> funders,
                             @RequestParam(required = false) List<String> persons,
                             Model model) {

        List<Project> projects = projectService.getAllProjects();
        List<BudgetEntry> filteredEntries = new ArrayList<>();

        for (Project project : projects) {
            for (BudgetEntry entry : project.getBudgetEntries()) {
                // Endast "Totala intäkter"
                if (!entry.getTitle().equalsIgnoreCase("Totala intäkter")) continue;

                boolean matchesYear = (years == null || years.isEmpty()) || entry.getValues().keySet().stream().anyMatch(years::contains);
                boolean matchesFunder = (funders == null || funders.isEmpty()) || funders.contains(project.getFundingSource());
                boolean matchesPerson = (persons == null || persons.isEmpty()) || persons.contains(project.getManager());

                if (matchesYear && matchesFunder && matchesPerson) {
                    filteredEntries.add(entry);
                }
            }
        }

        // 🔹 Summera total budget och formatera den snyggt
        double totalBudget = filteredEntries.stream().mapToDouble(BudgetEntry::getTotal).sum();
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedTotalBudget = formatter.format(totalBudget) + " SEK";

        // 🔹 Skapa data för stapeldiagram
        Map<String, Double> chartData = new HashMap<>();
        for (BudgetEntry entry : filteredEntries) {
            for (Map.Entry<Integer, Double> valueEntry : entry.getValues().entrySet()) {
                chartData.merge(String.valueOf(valueEntry.getKey()), valueEntry.getValue(), Double::sum);
            }
        }

        model.addAttribute("totalBudget", formattedTotalBudget);
        model.addAttribute("chartLabels", new ArrayList<>(chartData.keySet()));
        model.addAttribute("chartData", new ArrayList<>(chartData.values()));

        // 🔹 Skicka unika år, finansiärer och personer för filtrering
        model.addAttribute("availableYears", filteredEntries.stream()
                .flatMap(e -> e.getValues().keySet().stream())
                .collect(Collectors.toSet()));
        model.addAttribute("fundingSources", projects.stream().map(Project::getFundingSource).filter(Objects::nonNull).collect(Collectors.toSet()));
        model.addAttribute("projectManagers", projects.stream().map(Project::getManager).filter(Objects::nonNull).collect(Collectors.toSet()));

        return "budget";
    }

    /**
     * 🟢 Endpoint för att hämta total budget efter filtrering.
     */
    @GetMapping("/total")
    @ResponseBody
    public Map<String, String> getTotalBudget(@RequestParam(required = false) List<Integer> years,
                                              @RequestParam(required = false) List<String> funders,
                                              @RequestParam(required = false) List<String> persons) {
        List<Project> projects = projectService.getAllProjects();
        double totalBudget = 0.0;

        for (Project project : projects) {
            for (BudgetEntry entry : project.getBudgetEntries()) {
                // Se till att vi bara tar "Totala intäkter"
                if (!entry.getTitle().equalsIgnoreCase("Totala intäkter")) continue;

                boolean matchesFunder = (funders == null || funders.isEmpty()) || funders.contains(project.getFundingSource());
                boolean matchesPerson = (persons == null || persons.isEmpty()) || persons.contains(project.getManager());

                // 🟢 Viktigt! Filtrera på år
                if (matchesFunder && matchesPerson) {
                    for (Map.Entry<Integer, Double> valueEntry : entry.getValues().entrySet()) {
                        if (years == null || years.isEmpty() || years.contains(valueEntry.getKey())) {
                            totalBudget += valueEntry.getValue();
                        }
                    }
                }
            }
        }

        DecimalFormat formatter = new DecimalFormat("#,###");
        return Collections.singletonMap("totalBudget", formatter.format(totalBudget) + " SEK");
    }



    /**
     * 🟢 Endpoint för att hämta data till stapeldiagrammet.
     */
    @GetMapping("/chart")
    @ResponseBody
    public Map<String, List<Object>> getChartData(@RequestParam(required = false) List<Integer> years,
                                                  @RequestParam(required = false) List<String> funders,
                                                  @RequestParam(required = false) List<String> persons) {
        List<Project> projects = projectService.getAllProjects();
        Map<Integer, Double> chartData = new HashMap<>();

        for (Project project : projects) {
            for (BudgetEntry entry : project.getBudgetEntries()) {
                if (!entry.getTitle().equalsIgnoreCase("Totala intäkter")) continue; // 🟢 Endast "Totala intäkter"

                boolean matchesFunder = (funders == null || funders.isEmpty()) || funders.contains(project.getFundingSource());
                boolean matchesPerson = (persons == null || persons.isEmpty()) || persons.contains(project.getManager());

                if (matchesFunder && matchesPerson) {
                    for (Map.Entry<Integer, Double> valueEntry : entry.getValues().entrySet()) {
                        int year = valueEntry.getKey();
                        double amount = valueEntry.getValue();

                        if (years == null || years.isEmpty() || years.contains(year)) { // 🟢 Filtrera på år
                            chartData.merge(year, amount, Double::sum);
                        }
                    }
                }
            }
        }

        Map<String, List<Object>> response = new HashMap<>();
        response.put("labels", new ArrayList<>(chartData.keySet().stream().sorted().toList())); // 🟢 Sortera år
        response.put("values", new ArrayList<>(chartData.values()));

        return response;
    }

}
