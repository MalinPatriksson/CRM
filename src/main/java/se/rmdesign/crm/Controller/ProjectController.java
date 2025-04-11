package se.rmdesign.crm.Controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.IOException;
import java.io.InputStream;
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

        for (Project project : projects) {
            Double totalBudget = budgetEntryService.getTotalIncomeForProject(project.getId());
            project.setTotalBudget(totalBudget != null ? totalBudget : 0.0);
        }

        final double DEFAULT_MAX_BUDGET = 10_000_000;
        if (minBudget == null) minBudget = 0.0;
        if (maxBudget == null || maxBudget > DEFAULT_MAX_BUDGET) maxBudget = DEFAULT_MAX_BUDGET;

        double finalMinBudget = minBudget;
        double finalMaxBudget = maxBudget;
        projects = projects.stream()
                .filter(p -> p.getTotalBudget() >= finalMinBudget && p.getTotalBudget() <= finalMaxBudget)
                .collect(Collectors.toList());

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
                .collect(Collectors.toCollection(TreeSet::new));


        Set<String> projectManagers = projects.stream()
                .map(Project::getManager)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> fundingSources = projects.stream()
                .map(Project::getFundingSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> academies = projects.stream()
                .map(Project::getAcademies)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> researchPrograms = projects.stream()
                .map(Project::getResearchProgram)
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toSet());

        Set<String> statuses = projects.stream()
                .map(Project::getCurrentStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> activities = projects.stream()
                .map(Project::getActivity)
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toSet());


        String formattedMinBudget = formatter.format(minBudget);
        String formattedMaxBudget = formatter.format(maxBudget);

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
        model.addAttribute("activities", activities);

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
            @RequestParam("activity") String activity,
            @RequestParam(name = "academies", required = false) List<String> academies,
            @RequestParam Map<String, String> budgetRows,
            @RequestParam String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statusDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedResponseDate,
            @RequestParam(required = false) Integer weighting,
            RedirectAttributes redirectAttributes) {


        Project project = (id != null) ? projectService.getProjectById(id) : new Project();

        project.setName(name);
        project.setManager(manager);
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setFundingSource(fundingSource);
        project.setResearchProgram(researchProgram);
        project.setDiaryNumber(diaryNumber);
        project.setAcademies(academies);
        project.setExpectedResponseDate(expectedResponseDate);
        project.setStatusDate(statusDate);
        project.setActivity(activity);

        if (project.getCurrentStatus() == null || project.getCurrentStatus().isEmpty()) {
            project.setCurrentStatus(status);
        }

        Project savedProject = projectService.saveProject(project);

        List<BudgetEntry> budgetEntries = budgetEntryService.processBudgetEntries(budgetRows, savedProject);
        if (!budgetEntries.isEmpty()) {
            budgetEntryService.updateBudgetEntries(budgetEntries, savedProject, null);
        }

        ProjectStatus latestStatus = projectStatusService.getLatestStatus(savedProject.getId());

        if (latestStatus == null
                || !latestStatus.getStatus().equals(status)
                || !latestStatus.getStatusDate().equals(statusDate)
                || !Objects.equals(latestStatus.getWeighting(), weighting)) {

            ProjectStatus newStatus = new ProjectStatus();
            newStatus.setProject(savedProject);
            newStatus.setStatus(status);
            newStatus.setStatusDate(statusDate);
            newStatus.setWeighting(weighting != null ? weighting : 0);

            projectStatusService.saveProjectStatus(newStatus);
        }


        ProjectStatus latestStatusAfterSave = projectStatusService.getLatestStatus(savedProject.getId());
        if (latestStatusAfterSave != null) {
            savedProject.setCurrentStatus(latestStatusAfterSave.getStatus());
            projectService.saveProject(savedProject);
        }

        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har sparats!");
        return "redirect:/projects";
    }


    @GetMapping("/add-project")
    public String showAddProjectForm(Model model) {
        model.addAttribute("extractedData", new HashMap<>()); //Skicka en tom HashMap vid första laddning
        return "add-project";
    }

    @PostMapping("/upload-excel")
    public String uploadExcel(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // Kontroll: bara .xlsx eller .xlsm tillåts
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xlsm"))) {
                model.addAttribute("errorMessage", "Endast Excel-filer med .xlsx eller .xlsm stöds.");
                System.out.println("Filtyp: " + file.getContentType());
                System.out.println("Filnamn: " + file.getOriginalFilename());

                return "add-project";
            }

            Map<String, Object> extractedData = excelService.processExcelFile(file);
            model.addAttribute("extractedData", extractedData);
            return "add-project";
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
            @RequestParam("activity") String activity,
            @RequestParam(name = "academies", required = false) List<String> academies,
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
        project.setAcademies(academies);
        project.setExpectedResponseDate(expectedResponseDate);
        project.setStatusDate(statusDate);
        project.setActivity(activity);

        if (project.getCurrentStatus() == null || project.getCurrentStatus().isEmpty()) {
            project.setCurrentStatus(status);
        }

        Project savedProject = projectService.saveProject(project);

        List<BudgetEntry> budgetEntries = budgetEntryService.processBudgetEntries(budgetRows, savedProject);
        if (!budgetEntries.isEmpty()) {
            budgetEntryService.saveAll(budgetEntries, savedProject);
        }

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

        ProjectStatus latestStatusAfterSave = projectStatusService.getLatestStatus(savedProject.getId());
        if (latestStatusAfterSave != null) {
            savedProject.setCurrentStatus(latestStatusAfterSave.getStatus());
            projectService.saveProject(savedProject);
        }

        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har lagts till!");
        return "redirect:/projects";
    }

    @PostMapping("/upload-excel-edit")
    public String uploadExcelEdit(@RequestParam("file") MultipartFile file,
                                  @RequestParam("projectId") Long projectId,
                                  Model model) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xlsm"))) {
                model.addAttribute("errorMessage", "Endast Excel-filer med .xlsx eller .xlsm stöds.");
                return "edit-project";
            }

            Map<String, Object> extractedData = excelService.processExcelFile(file);
            Project project = projectService.getProjectById(projectId);
            if (project == null) {
                model.addAttribute("errorMessage", "Projektet kunde inte hittas.");
                return "error";
            }

            // Skriv endast över om värde finns i filen
            if (extractedData.get("Projektnamn") != null)
                project.setName((String) extractedData.get("Projektnamn"));

            if (extractedData.get("Diarienummer") != null)
                project.setDiaryNumber((String) extractedData.get("Diarienummer"));

            if (extractedData.get("Projektledares för- och efternamn") != null)
                project.setManager((String) extractedData.get("Projektledares för- och efternamn"));

            if (extractedData.get("Finansiär") != null)
                project.setFundingSource((String) extractedData.get("Finansiär"));

            LocalDate startDate = parseDate(extractedData.get("Startdatum"));
            if (startDate != null)
                project.setStartDate(startDate);

            LocalDate deadline = parseDate(extractedData.get("Deadline"));
            if (deadline != null)
                project.setDeadline(deadline);

            if (extractedData.get("Forskningsprogram") != null)
                project.setResearchProgram((String) extractedData.get("Forskningsprogram"));

            model.addAttribute("project", project);
            model.addAttribute("budgetRows", extractedData.get("BudgetRows"));
            model.addAttribute("years", extractedData.get("Years"));

            ProjectStatus currentStatus = projectStatusService.getLatestStatus(projectId);
            LocalDate statusDate = (currentStatus != null) ? currentStatus.getStatusDate() : LocalDate.now();
            model.addAttribute("currentStatus", (currentStatus != null) ? currentStatus.getStatus() : "Idé");
            model.addAttribute("statusDate", statusDate);
            model.addAttribute("statusHistory", projectStatusService.getStatusHistory(projectId));
            Integer weighting = (currentStatus != null) ? currentStatus.getWeighting() : 0;
            model.addAttribute("currentStatusWeighting", weighting);
            model.addAttribute("activity", project.getActivity());


            return "edit-project";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Fel vid bearbetning av Excel-filen: " + e.getMessage());
            return "error";
        }
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

            DecimalFormat formatter = new DecimalFormat("#,###");
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(' ');
            formatter.setDecimalFormatSymbols(symbols);

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
            model.addAttribute("currentStatusWeighting", (currentStatus != null) ? currentStatus.getWeighting() : 0);


            return "edit-project";
        }


    @PostMapping("/{id}/delete-project")
    public String deleteProject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectService.deleteProjectById(id);
        redirectAttributes.addFlashAttribute("message", "Projektet har tagits bort!");
        return "redirect:/projects";
    }

    @GetMapping("/export")
    public void exportFilteredProjects(@RequestParam Map<String, String> filters, HttpServletResponse response) throws IOException {
        // 1. Filtrera projekt (samma logik som din översiktssida)
        List<Project> filteredProjects = projectService.filterProjects(filters);

        // 2. Ladda in Excel-mall
        InputStream templateStream = getClass().getResourceAsStream("/templates/excel-template.xlsx");
        XSSFWorkbook workbook = new XSSFWorkbook(templateStream);

        // 3. För varje projekt – duplicera blad och fyll i data
        for (Project project : filteredProjects) {
            Sheet sheet = workbook.cloneSheet(0);
            workbook.setSheetName(workbook.getSheetIndex(sheet), project.getName());

            projectService.fillProjectToSheet(project, sheet); // En metod som fyller in fält i rätt celler
        }

        workbook.removeSheetAt(0);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=projekt-export.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @PostMapping("/projects/export")
    public void exportProjects(@RequestParam Map<String, String> allParams, HttpServletResponse response) throws IOException {
        List<Project> filtered = projectService.filterProjects(allParams);

        Workbook workbook = new XSSFWorkbook();
        Set<String> usedNames = new HashSet<>();

        for (Project p : filtered) {
            String baseName = p.getName().length() > 31 ? p.getName().substring(0, 31) : p.getName();
            String sheetName = baseName;
            int counter = 1;

            while (usedNames.contains(sheetName)) {
                sheetName = (baseName.length() > 28 ? baseName.substring(0, 28) : baseName) + " (" + counter++ + ")";
            }

            usedNames.add(sheetName);
            Sheet sheet = workbook.createSheet(sheetName);
            projectService.fillProjectToSheet(p, sheet);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exporterade_projekt.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }


    @GetMapping("/projects/export")
    public void exportSingleProject(@RequestParam Long id, HttpServletResponse response) throws IOException {
        Project project = projectService.getProjectById(id);
        projectService.exportSingleProject(project, response);
    }

    @GetMapping("/projects/exportAll")
    public void exportAllProjects(HttpServletResponse response) throws IOException {
        List<Project> projects = projectService.getAllProjects(); // Retrieve the list of projects

        Workbook workbook = new XSSFWorkbook();
        for (Project project : projects) {
            Sheet sheet = workbook.createSheet(
                    project.getName().substring(0, Math.min(31, project.getName().length()))
            );
            projectService.fillProjectToSheet(project, sheet);
        }

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setHeader("Content-Disposition", "attachment; filename=all_projects.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }


}
