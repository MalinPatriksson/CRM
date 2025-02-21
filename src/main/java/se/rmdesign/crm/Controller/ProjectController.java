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
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectStatus;
import se.rmdesign.crm.Services.BudgetEntryService;
import se.rmdesign.crm.Services.ExcelService;
import se.rmdesign.crm.Services.ProjectFileService;
import se.rmdesign.crm.Services.ProjectService;
import se.rmdesign.crm.Services.ProjectStatusService; // 🟢 Importera ProjectStatusService

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final ExcelService excelService;
    private final BudgetEntryService budgetEntryService;
    private final ProjectStatusService projectStatusService;

    @Autowired
    public ProjectController(ProjectService projectService, ExcelService excelService,
                             BudgetEntryService budgetEntryService, ProjectStatusService projectStatusService) {
        this.projectService = projectService;
        this.excelService = excelService;
        this.budgetEntryService = budgetEntryService;
        this.projectStatusService = projectStatusService;
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

        // Hämta totalbudget från "Totala intäkter"
        DecimalFormat formatter = new DecimalFormat("#,###"); // Tusentalsavgränsning

        for (Project project : projects) {
            Optional<BudgetEntry> totalEntry = project.getBudgetEntries().stream()
                    .filter(entry -> entry.getTitle().equalsIgnoreCase("Totala intäkter"))
                    .findFirst();

            double totalBudget = totalEntry.map(BudgetEntry::getTotal).orElse(0.0);
            project.setTotalBudget(totalBudget);
        }

        // Sätt standardvärden för budgetfiltret
        final double DEFAULT_MAX_BUDGET = 10_000_000; // Alltid max 10 miljoner

        if (minBudget == null) {
            minBudget = 0.0; // Standardvärde för min budget
        }
        if (maxBudget == null || maxBudget > DEFAULT_MAX_BUDGET) {
            maxBudget = DEFAULT_MAX_BUDGET; // Standardvärde för max budget
        }

        // Filtrera på totalbudget
        double finalMinBudget = minBudget;
        double finalMaxBudget = maxBudget;
        projects = projects.stream()
                .filter(p -> p.getTotalBudget() >= finalMinBudget && p.getTotalBudget() <= finalMaxBudget)
                .collect(Collectors.toList());

        // Formatera budgetvärden i backend
        String formattedMinBudget = formatter.format(minBudget);
        String formattedMaxBudget = formatter.format(maxBudget);

        // Lägg till i modellen
        model.addAttribute("projects", projects);
        model.addAttribute("projectManagers", projects.stream().map(Project::getManager).filter(Objects::nonNull).collect(Collectors.toSet()));
        model.addAttribute("fundingSources", projects.stream().map(Project::getFundingSource).filter(Objects::nonNull).collect(Collectors.toSet()));
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
            @RequestParam Map<String, String> budgetRows,
            @RequestParam String status,  // 🔹 Tar emot status
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statusDate, // ✅ Fixad
            RedirectAttributes redirectAttributes) {

        System.out.println("📌 Hanterar projekt: " + (id != null ? "Uppdatering" : "Ny skapelse"));

        // Hämta projektet om det finns, annars skapa ett nytt
        Project project = (id != null) ? projectService.getProjectById(id) : new Project();

        project.setName(name);
        project.setManager(manager);
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setFundingSource(fundingSource);
        project.setResearchProgram(researchProgram);
        project.setDiaryNumber(diaryNumber);

        // 🔹 Spara projektet först
        Project savedProject = projectService.saveProject(project);

        // 🔹 Hantera budgetuppgifter
        List<BudgetEntry> budgetEntries = budgetEntryService.processBudgetEntries(budgetRows, savedProject);
        budgetEntryService.saveAll(budgetEntries);

        System.out.println("✅ Budgeten uppdaterad för projekt ID: " + savedProject.getId());

        // 🔹 Spara ny status i historiken
        ProjectStatus projectStatus = new ProjectStatus(status, statusDate, savedProject);
        projectStatusService.saveProjectStatus(projectStatus);

        System.out.println("✅ Status uppdaterad till: " + status + " den " + statusDate);

        // 🔹 Meddela användaren om ändringar
        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har " + (id != null ? "uppdaterats!" : "lagts till!"));

        return "redirect:/projects";
    }



    @GetMapping("/projects/{id}/budget")
    public String showBudget(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        model.addAttribute("project", project);

        List<BudgetEntry> budgetEntries = budgetEntryService.findByProject(project);
        model.addAttribute("budgetEntries", budgetEntries);

        // Hämta budgetposten "Totala intäkter" från databasen
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
        model.addAttribute("extractedData", new HashMap<>()); // Skicka en tom HashMap vid första laddning
        return "add-project";
    }

    @PostMapping("/upload-pdf")
    public String processPdf(@RequestParam("file") MultipartFile file, Model model) {
        try {
            Map<String, Object> extractedData = excelService.processPdfFile(file);
            model.addAttribute("extractedData", extractedData);
        } catch (Exception e) {
            model.addAttribute("error", "Kunde inte bearbeta PDF-filen: " + e.getMessage());
        }
        return "add-project";
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
            @RequestParam Map<String, String> budgetRows,
            @RequestParam String status,  // 🔹 Nytt: Tar emot status
            @RequestParam LocalDate statusDate, // 🔹 Nytt: Tar emot datum
            RedirectAttributes redirectAttributes) {

        System.out.println("📌 INKOMMANDE `budgetRows`:");
        budgetRows.forEach((key, value) -> System.out.println(key + " = " + value));

        // 🔹 Skapa eller hämta befintligt projekt
        Project project = (id != null) ? projectService.getProjectById(id) : new Project();
        project.setName(name);
        project.setManager(manager);
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setFundingSource(fundingSource);
        project.setResearchProgram(researchProgram);
        project.setDiaryNumber(diaryNumber);

        // 🔹 Spara projektet först
        Project savedProject = projectService.saveProject(project);

        // 🔹 Hantera budgetdata
        List<BudgetEntry> budgetEntries = budgetEntryService.processBudgetEntries(budgetRows, savedProject);
        budgetEntryService.saveAll(budgetEntries);
        System.out.println("✅ Antal sparade budgetrader: " + budgetEntries.size());

        // 🔹 Spara status i `ProjectStatus`-tabellen
        ProjectStatus projectStatus = new ProjectStatus(status, statusDate, savedProject);
        projectStatusService.saveProjectStatus(projectStatus);

        // 🔹 Meddela användaren
        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har lagts till!");
        return "redirect:/projects";
    }

    @GetMapping("/edit/{id}")
    public String showEditProjectForm(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);

        if (project == null) {
            return "redirect:/projects";
        }

        // 🔹 Hämta budgetuppgifter
        List<BudgetEntry> budgetEntries = budgetEntryService.findByProject(project);

        // 🔹 Hämta aktuell status och datum
        ProjectStatus currentStatus = projectStatusService.getLatestStatus(id);
        LocalDate statusDate = (currentStatus != null) ? currentStatus.getStatusDate() : LocalDate.now();

        // 🔹 Hämta historik av statusändringar
        List<ProjectStatus> statusHistory = projectStatusService.getStatusHistory(id);

        // 🔹 Lägg till i modellen
        model.addAttribute("project", project);
        model.addAttribute("budgetEntries", budgetEntries);
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
