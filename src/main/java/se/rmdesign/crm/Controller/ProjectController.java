package se.rmdesign.crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectStatus;
import se.rmdesign.crm.Services.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final ExcelService excelService;
    private final BudgetEntryService budgetEntryService;
    private final ProjectStatusService projectStatusService;
    private final BudgetEntryValueService budgetEntryValueService;

    @Autowired
    public ProjectController(ProjectService projectService, ExcelService excelService,
                             BudgetEntryService budgetEntryService, ProjectStatusService projectStatusService, BudgetEntryValueService budgetEntryValueService) {
        this.projectService = projectService;
        this.excelService = excelService;
        this.budgetEntryService = budgetEntryService;
        this.projectStatusService = projectStatusService;
        this.budgetEntryValueService = budgetEntryValueService;
    }

    @GetMapping("/")
    public String startPage() {
        return "start";
    }

    @GetMapping("/projects")
    public String getProjects(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "name") String sortBy,
                              @RequestParam(defaultValue = "asc") String order,
                              @RequestParam(required = false) Double minBudget,
                              @RequestParam(required = false) Double maxBudget,
                              Model model) {
        Sort.Direction sortDirection = order.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        List<Project> projects = (keyword != null && !keyword.trim().isEmpty())
                ? projectService.searchProjects(keyword)
                : projectService.getAllProjects(Sort.by(sortDirection, sortBy));

        DecimalFormat formatter = new DecimalFormat("#,###");

        // 🔹 Räkna ut och sätt budget för varje projekt
        for (Project project : projects) {
            Double totalBudget = budgetEntryService.getTotalIncomeForProject(project.getId());
            project.setTotalBudget(totalBudget != null ? totalBudget : 0.0);
        }

        // 🔹 Budgetfilter gränser
        final double DEFAULT_MAX_BUDGET = 10_000_000;
        if (minBudget == null) minBudget = 0.0;
        if (maxBudget == null || maxBudget > DEFAULT_MAX_BUDGET) maxBudget = DEFAULT_MAX_BUDGET;

        // 🔹 Filtrera projekt på budget
        double finalMinBudget = minBudget;
        double finalMaxBudget = maxBudget;
        projects = projects.stream()
                .filter(p -> p.getTotalBudget() >= finalMinBudget && p.getTotalBudget() <= finalMaxBudget)
                .collect(Collectors.toList());

        // 🔹 Alla år mellan start och deadline (inklusive mellanliggande år)
        Set<Integer> allYears = projects.stream()
                .flatMap(project -> {
                    LocalDate start = project.getStartDate();
                    LocalDate end = project.getDeadline();
                    if (start != null && end != null && !end.isBefore(start)) {
                        return start.datesUntil(end.plusDays(1))
                                .map(LocalDate::getYear)
                                .distinct();
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toCollection(TreeSet::new)); // sorterad lista

        // 🔹 Övriga filterdata
        Set<String> projectManagers = projects.stream()
                .map(Project::getManager)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> fundingSources = projects.stream()
                .map(Project::getFundingSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> academies = projects.stream()
                .map(Project::getAcademy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> researchPrograms = projects.stream()
                .map(Project::getResearchProgram)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> statuses = projects.stream()
                .map(Project::getCurrentStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 🔹 Formaterade budgetgränser
        String formattedMinBudget = formatter.format(minBudget);
        String formattedMaxBudget = formatter.format(maxBudget);

        // 🔹 Lägg till i model
        model.addAttribute("projects", projects);
        model.addAttribute("projectManagers", projectManagers);
        model.addAttribute("fundingSources", fundingSources);
        model.addAttribute("academies", academies);
        model.addAttribute("researchPrograms", researchPrograms);
        model.addAttribute("statuses", statuses);
        model.addAttribute("years", allYears);
        model.addAttribute("minBudget", minBudget);
        model.addAttribute("maxBudget", maxBudget);
        model.addAttribute("formattedMinBudget", formattedMinBudget);
        model.addAttribute("formattedMaxBudget", formattedMaxBudget);

        return "projects";
    }


    @PostMapping("/projects/save")
    public String saveOrUpdateProject(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam String manager,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate deadline,
            @RequestParam String fundingSource,
            @RequestParam String researchProgram,
            @RequestParam String diaryNumber,
            @RequestParam(required = false) String academy,
            @RequestParam Map<String, String> budgetRows,
            @RequestParam String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statusDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedResponseDate,
            @RequestParam(required = false) Integer weighting,
            RedirectAttributes redirectAttributes) {

        System.out.println("📌 Hanterar projekt: " + (id != null ? "Uppdatering" : "Ny skapelse"));

        Project project = (id != null) ? projectService.getProjectById(id) : new Project();

        project.setName(name);
        project.setManager(manager);
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setFundingSource(fundingSource);
        project.setResearchProgram(researchProgram);
        project.setDiaryNumber(diaryNumber);
        project.setAcademy(academy); // ✅ Akademi
        project.setExpectedResponseDate(expectedResponseDate); // ✅ Förväntat svar
        project.setStatusDate(statusDate); // ✅ Statusdatum

        if (project.getCurrentStatus() == null || project.getCurrentStatus().isEmpty()) {
            project.setCurrentStatus(status);
        }

        Project savedProject = projectService.saveProject(project);

        List<BudgetEntry> budgetEntries = budgetEntryService.processBudgetEntries(budgetRows, savedProject);
        if (!budgetEntries.isEmpty()) {
            budgetEntryService.updateBudgetEntries(budgetEntries, savedProject, null);
        }

        ProjectStatus latestStatus = projectStatusService.getLatestStatus(savedProject.getId());

        if (latestStatus == null || !latestStatus.getStatus().equals(status) || !latestStatus.getStatusDate().equals(statusDate)) {
            ProjectStatus newStatus = new ProjectStatus(status, statusDate, savedProject);
            newStatus.setWeighting(weighting != null ? weighting : 0); // ✅ Viktning
            projectStatusService.saveProjectStatus(newStatus);
        }

        // ✅ Uppdatera currentStatus i Project-tabellen
        ProjectStatus latestStatusAfterSave = projectStatusService.getLatestStatus(savedProject.getId());
        if (latestStatusAfterSave != null) {
            savedProject.setCurrentStatus(latestStatusAfterSave.getStatus());
            projectService.saveProject(savedProject);
        }

        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har sparats!");
        return "redirect:/projects";
    }



    @GetMapping("/projects/{id}/budget")
    public String showBudget(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        model.addAttribute("project", project);

        List<BudgetEntry> budgetEntries = budgetEntryService.findByProject(project);
        model.addAttribute("budgetEntries", budgetEntries);

        //Hämta budgetposten "Totala intäkter" från databasen
        Optional<BudgetEntry> totalEntry = budgetEntries.stream()
                .filter(entry -> entry.getTitle().equalsIgnoreCase("Totala intäkter"))
                .findFirst();

        double totalBudget = totalEntry.map(BudgetEntry::getTotal).orElse(0.0);
        project.setTotalBudget(totalBudget);
        model.addAttribute("totalBudget", totalBudget);

        List<Integer> years = IntStream.rangeClosed(project.getStartDate().getYear(), project.getDeadline().getYear())
                .boxed().collect(Collectors.toList());
        model.addAttribute("years", years);

        return "budget-table";
    }


    @GetMapping("/add-project")
    public String showAddProjectForm(Model model) {
        model.addAttribute("extractedData", new HashMap<>()); //Skicka en tom HashMap vid första laddning
        return "add-project";
    }

    @PostMapping("/upload-excel")
    public String uploadExcel(@RequestParam("file") MultipartFile file, Model model) {
        try {
            Map<String, Object> extractedData = excelService.processExcelFile(file);
            model.addAttribute("extractedData", extractedData);
            return "add-project"; // Samma template som PDF-formuläret
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fel vid bearbetning av Excel-filen: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/add")
    public String addProject(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam String manager,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate deadline,
            @RequestParam String fundingSource,
            @RequestParam String researchProgram,
            @RequestParam String diaryNumber,
            @RequestParam String academy,
            @RequestParam Map<String, String> budgetRows,
            @RequestParam String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statusDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedResponseDate,
            @RequestParam(required = false) Integer weighting,
            RedirectAttributes redirectAttributes) {

        System.out.println("📌 INKOMMANDE `budgetRows`: " + budgetRows.size());

        // 🔹 Skapa eller hämta projekt
        Project project = (id != null) ? projectService.getProjectById(id) : new Project();
        project.setName(name);
        project.setManager(manager);
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setFundingSource(fundingSource);
        project.setResearchProgram(researchProgram);
        project.setDiaryNumber(diaryNumber);
        project.setAcademy(academy);
        project.setExpectedResponseDate(expectedResponseDate);
        project.setStatusDate(statusDate);

        if (project.getCurrentStatus() == null || project.getCurrentStatus().isEmpty()) {
            project.setCurrentStatus(status);
        }

        // 🔹 Spara projektet först
        Project savedProject = projectService.saveProject(project);

        // 🔹 Spara budget
        List<BudgetEntry> budgetEntries = budgetEntryService.processBudgetEntries(budgetRows, savedProject);
        if (!budgetEntries.isEmpty()) {
            budgetEntryService.saveAll(budgetEntries, savedProject);
        }

        // 🔹 Hantera status och viktning
        ProjectStatus latestStatus = projectStatusService.getLatestStatus(savedProject.getId());

        if (latestStatus == null) {
            ProjectStatus newStatus = new ProjectStatus(status, statusDate, savedProject);
            newStatus.setWeighting(weighting != null ? weighting : 0);
            projectStatusService.saveProjectStatus(newStatus);
        } else if (!latestStatus.getStatus().equals(status) || !latestStatus.getStatusDate().equals(statusDate)) {
            latestStatus.setStatus(status);
            latestStatus.setStatusDate(statusDate);
            latestStatus.setWeighting(weighting != null ? weighting : 0);
            projectStatusService.saveProjectStatus(latestStatus);
        }

        // 🔹 Uppdatera currentStatus i Project
        ProjectStatus latestStatusAfterSave = projectStatusService.getLatestStatus(savedProject.getId());
        if (latestStatusAfterSave != null) {
            savedProject.setCurrentStatus(latestStatusAfterSave.getStatus());
            projectService.saveProject(savedProject);
        }

        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har lagts till!");
        return "redirect:/projects";
    }


    @GetMapping("/edit/{id}")
    public String showEditProjectForm(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);

        if (project == null) {
            return "redirect:/projects";
        }

        // Hämta budgetposter och dess värden
        List<BudgetEntry> budgetEntries = budgetEntryService.findByProject(project);
        for (BudgetEntry entry : budgetEntries) {
            List<BudgetEntryValue> budgetValues = budgetEntryValueService.findByBudgetEntry(entry);
            entry.setBudgetValues(budgetValues);
        }

        // Formatterare med mellanslag och utan decimaler
        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(symbols);

        // Skapa map med rader för HTML-formuläret
        List<Map<String, String>> budgetRows = new ArrayList<>();

        for (BudgetEntry entry : budgetEntries) {
            Map<String, String> row = new HashMap<>();
            row.put("Rubrik", entry.getTitle());

            double total = 0.0;
            for (BudgetEntryValue value : entry.getBudgetValues()) {
                String year = String.valueOf(value.getYear());
                total += value.getValue();
                row.put(year, formatter.format(value.getValue()));
            }

            row.put("Total", formatter.format(total));
            budgetRows.add(row);
        }

        // Hämta unika år från alla värden
        Set<Integer> yearsSet = budgetEntries.stream()
                .flatMap(entry -> entry.getBudgetValues().stream())
                .map(BudgetEntryValue::getYear)
                .collect(Collectors.toCollection(TreeSet::new));

        List<Integer> years = new ArrayList<>(yearsSet);

        // Hämta aktuell status och historik
        ProjectStatus currentStatus = projectStatusService.getLatestStatus(id);
        LocalDate statusDate = (currentStatus != null) ? currentStatus.getStatusDate() : LocalDate.now();
        List<ProjectStatus> statusHistory = projectStatusService.getStatusHistory(id);

        model.addAttribute("project", project);
        model.addAttribute("budgetEntries", budgetEntries);
        model.addAttribute("budgetRows", budgetRows);
        model.addAttribute("years", years);
        model.addAttribute("currentStatus", (currentStatus != null) ? currentStatus.getStatus() : "Idé");
        model.addAttribute("statusDate", statusDate);
        model.addAttribute("statusHistory", statusHistory);

        return "edit-project";
    }


    @PostMapping("/{id}/delete-project")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectService.deleteProjectById(id);
        redirectAttributes.addFlashAttribute("message", "Projektet har tagits bort!");
        return "redirect:/projects";
    }
}
