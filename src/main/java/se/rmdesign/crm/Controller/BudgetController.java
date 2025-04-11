package se.rmdesign.crm.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectStatus;
import se.rmdesign.crm.Services.BudgetEntryService;
import se.rmdesign.crm.Services.ProjectService;
import se.rmdesign.crm.Services.ProjectStatusService;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/budget")
public class BudgetController {

    private final ProjectService projectService;
    private final BudgetEntryService budgetEntryService;
    private final ProjectStatusService projectStatusService;

    public BudgetController(ProjectService projectService, BudgetEntryService budgetEntryService, ProjectStatusService projectStatusService) {
        this.projectService = projectService;
        this.budgetEntryService = budgetEntryService;
        this.projectStatusService = projectStatusService;
    }

    @GetMapping
    public String showBudget(@RequestParam(required = false) List<Integer> years,
                             @RequestParam(required = false) List<String> funders,
                             @RequestParam(required = false) List<String> persons,
                             @RequestParam(required = false) List<String> statuses,
                             @RequestParam(required = false) List<String> academies,
                             @RequestParam(required = false) List<String> programs,
                             Model model) {

        List<Project> projects = projectService.getAllProjects();
        List<BudgetEntry> filteredEntries = new ArrayList<>();

        for (Project project : projects) {
            Optional<BudgetEntry> totalEntryOpt = budgetEntryService.findTotalIncomeEntry(project.getId());

            totalEntryOpt.ifPresent(entry -> {
                List<BudgetEntryValue> values = new ArrayList<>(entry.getBudgetValues());

                boolean matchesYear = (years == null || years.isEmpty()) || values.stream().anyMatch(v -> years.contains(v.getYear()));
                boolean matchesFunder = (funders == null || funders.isEmpty()) || funders.contains(project.getFundingSource());
                boolean matchesPerson = (persons == null || persons.isEmpty()) || persons.contains(project.getManager());
                boolean matchesStatus = (statuses == null || statuses.isEmpty()) || statuses.contains(project.getCurrentStatus());
                boolean matchesAcademy = (academies == null || academies.isEmpty()) ||
                        project.getAcademies().stream().anyMatch(academies::contains);
                boolean matchesProgram = (programs == null || programs.isEmpty()) || programs.contains(project.getResearchProgram());

                if (matchesYear && matchesFunder && matchesPerson && matchesStatus && matchesAcademy && matchesProgram) {
                    filteredEntries.add(entry);
                }
            });
        }

        double totalBudget = filteredEntries.stream().mapToDouble(BudgetEntry::getTotal).sum();
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedTotalBudget = formatter.format(totalBudget) + " SEK";

        Map<Integer, Double> chartData = new HashMap<>();
        for (BudgetEntry entry : filteredEntries) {
            for (BudgetEntryValue value : entry.getBudgetValues()) {
                chartData.merge(value.getYear(), value.getValue(), Double::sum);
            }
        }

        model.addAttribute("totalBudget", formattedTotalBudget);
        model.addAttribute("chartLabels", new ArrayList<>(chartData.keySet()));
        model.addAttribute("chartData", new ArrayList<>(chartData.values()));
        model.addAttribute("availableYears", filteredEntries.stream()
                .flatMap(e -> e.getBudgetValues().stream().map(BudgetEntryValue::getYear))
                .collect(Collectors.toSet()));
        model.addAttribute("fundingSources", projects.stream().map(Project::getFundingSource).filter(Objects::nonNull).collect(Collectors.toSet()));
        model.addAttribute("projectManagers", projects.stream().map(Project::getManager).filter(Objects::nonNull).collect(Collectors.toSet()));
        model.addAttribute("availableStatuses", projects.stream()
                .map(Project::getCurrentStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        String selectedStatusLabel = (statuses == null || statuses.isEmpty()) ? "" : "Status: " + String.join(", ", statuses);
        model.addAttribute("selectedStatusLabel", selectedStatusLabel);
        model.addAttribute("academies", projects.stream()
                .map(Project::getAcademies)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        model.addAttribute("researchPrograms", projects.stream()
                .map(Project::getResearchProgram)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));


        model.addAttribute("selectedYears", years != null ? years : Collections.emptyList());
        model.addAttribute("selectedFunders", funders != null ? funders : Collections.emptyList());
        model.addAttribute("selectedPersons", persons != null ? persons : Collections.emptyList());
        model.addAttribute("selectedStatuses", statuses != null ? statuses : Collections.emptyList());
        model.addAttribute("selectedAcademies", academies != null ? academies : Collections.emptyList());
        model.addAttribute("selectedPrograms", programs != null ? programs : Collections.emptyList());

        return "budget";
    }

    @GetMapping("/total")
    @ResponseBody
    public Map<String, String> getTotalBudget(@RequestParam(required = false) List<Integer> years,
                                              @RequestParam(required = false) List<String> funders,
                                              @RequestParam(required = false) List<String> persons,
                                              @RequestParam(required = false) List<String> statuses,
                                              @RequestParam(required = false) List<String> academies,
                                              @RequestParam(required = false) List<String> programs,
                                              @RequestParam(name = "weighted", defaultValue = "false") boolean weighted) {

        List<Project> projects = projectService.getAllProjects();
        double totalBudget = 0.0;

        for (Project project : projects) {
            Optional<BudgetEntry> totalEntryOpt = budgetEntryService.findTotalIncomeEntry(project.getId());

            if (totalEntryOpt.isPresent()) {
                BudgetEntry entry = totalEntryOpt.get();

                boolean matchesFunder = (funders == null || funders.isEmpty()) || funders.contains(project.getFundingSource());
                boolean matchesPerson = (persons == null || persons.isEmpty()) || persons.contains(project.getManager());
                boolean matchesStatus = (statuses == null || statuses.isEmpty()) || statuses.contains(project.getCurrentStatus());
                boolean matchesAcademy = (academies == null || academies.isEmpty()) ||
                        project.getAcademies().stream().anyMatch(academies::contains);
                boolean matchesProgram = (programs == null || programs.isEmpty()) || programs.contains(project.getResearchProgram());

                if (matchesFunder && matchesPerson && matchesStatus && matchesAcademy && matchesProgram) {
                    double sum = 0.0;
                    if (years == null || years.isEmpty()) {
                        sum = entry.getTotal();
                    } else {
                        for (BudgetEntryValue value : entry.getBudgetValues()) {
                            if (years.contains(value.getYear())) {
                                sum += value.getValue();
                            }
                        }
                    }

                    if (weighted) {
                        ProjectStatus latestStatus = projectStatusService.getLatestStatus(project.getId());
                        int weight = latestStatus != null ? latestStatus.getWeighting() : 0;
                        sum *= (weight / 100.0);
                    }

                    totalBudget += sum;
                }
            }
        }

        DecimalFormat formatter = new DecimalFormat("#,###");
        return Collections.singletonMap("totalBudget", formatter.format(totalBudget) + " SEK");
    }


    @GetMapping("/chart")
    @ResponseBody
    public Map<String, List<Object>> getChartData(
            @RequestParam(required = false) List<Integer> years,
            @RequestParam(required = false) List<String> funders,
            @RequestParam(required = false) List<String> persons,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> academies,
            @RequestParam(required = false) List<String> programs,
            @RequestParam(name = "weighted", defaultValue = "false") boolean weighted) {

        List<Project> projects = projectService.getAllProjects();
        Map<Integer, Double> chartData = new HashMap<>();

        for (Project project : projects) {
            Optional<BudgetEntry> totalEntryOpt = budgetEntryService.findTotalIncomeEntry(project.getId());

            totalEntryOpt.ifPresent(entry -> {
                boolean matchesFunder = (funders == null || funders.isEmpty()) || funders.contains(project.getFundingSource());
                boolean matchesPerson = (persons == null || persons.isEmpty()) || persons.contains(project.getManager());
                boolean matchesStatus = (statuses == null || statuses.isEmpty()) || statuses.contains(project.getCurrentStatus());
                boolean matchesAcademy = (academies == null || academies.isEmpty()) ||
                        project.getAcademies().stream().anyMatch(academies::contains);
                boolean matchesProgram = (programs == null || programs.isEmpty()) || programs.contains(project.getResearchProgram());

                if (matchesFunder && matchesPerson && matchesStatus && matchesAcademy && matchesProgram) {
                    for (BudgetEntryValue value : entry.getBudgetValues()) {
                        if (years == null || years.isEmpty() || years.contains(value.getYear())) {
                            double finalValue = value.getValue();
                            if (weighted) {
                                ProjectStatus latestStatus = projectStatusService.getLatestStatus(project.getId());
                                int weight = latestStatus != null ? latestStatus.getWeighting() : 0;
                                finalValue *= (weight / 100.0);
                            }
                            chartData.merge(value.getYear(), finalValue, Double::sum);
                        }
                    }
                }
            });
        }

        Map<String, List<Object>> response = new HashMap<>();
        response.put("labels", chartData.keySet().stream().sorted().map(Object.class::cast).toList());
        response.put("values", chartData.keySet().stream().sorted().map(chartData::get).map(Object.class::cast).toList());

        return response;
    }
}
