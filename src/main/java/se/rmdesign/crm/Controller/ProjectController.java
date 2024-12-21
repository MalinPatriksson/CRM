package se.rmdesign.crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Repos.ProjectFileRepository;
import se.rmdesign.crm.Services.ExcelService;
import se.rmdesign.crm.Services.ProjectService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectFileRepository projectFileRepository;
    private final ExcelService excelService;

    @Autowired
    public ProjectController(ProjectService projectService, ProjectFileRepository projectFileRepository, ExcelService excelService) {
        this.projectService = projectService;
        this.projectFileRepository = projectFileRepository;
        this.excelService = excelService;
    }


    @GetMapping("/")
    public String startPage() {
        return "start";
    }

    @GetMapping("/projects")
    public String getProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        int pageSize = 10;
        var pageable = PageRequest.of(page - 1, pageSize,
                order.equals("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());

        var projectPage = (keyword != null && !keyword.isEmpty())
                ? projectService.searchProjectsPaginated(keyword, pageable)
                : projectService.getProjectsPaginated(pageable);

        model.addAttribute("projects", projectPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", projectPage.getTotalPages());
        model.addAttribute("pages", IntStream.rangeClosed(1, projectPage.getTotalPages()).boxed().collect(Collectors.toList()));
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("order", order);

        return "projects";
    }

    @ResponseBody
    @GetMapping("/project/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable int id) {
        return projectService.getAllProjects().stream()
                .filter(project -> project.getId() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/add-project")
    public String showAddProjectForm(Model model) {
        model.addAttribute("extractedData", new HashMap<>()); // Skicka en tom HashMap vid första laddning
        return "add-project"; // Namnet på HTML-mallen
    }

    @PostMapping("/add")
    public String addProject(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String manager,
            @RequestParam(required = false) LocalDate deadline,
            @RequestParam(required = false) Integer budgetYear1,
            @RequestParam(required = false) Integer budgetYear2,
            @RequestParam(required = false) Integer budgetYear3,
            @RequestParam(required = false) Integer spent,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) String fundingSource,
            @RequestParam(required = false) String researchProgram,
            @RequestParam(required = false) MultipartFile file, // För filuppladdning
            RedirectAttributes redirectAttributes,
            Model model) {

        // Hantera nullvärden genom att sätta standardvärden om nödvändigt
        budgetYear1 = (budgetYear1 != null) ? budgetYear1 : 0;
        budgetYear2 = (budgetYear2 != null) ? budgetYear2 : 0;
        budgetYear3 = (budgetYear3 != null) ? budgetYear3 : 0;
        spent = (spent != null) ? spent : 0;

        Map<String, String> extractedData = new HashMap<>();

        // Bearbeta filen om den är uppladdad
        if (file != null && !file.isEmpty()) {
            try {
                extractedData = excelService.processPdfFile(file);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Kunde inte bearbeta PDF-filen: " + e.getMessage());
                return "redirect:/projects";
            }
        }

        // Om fildata hittades, fyll i de värden som inte redan har skickats
        name = name != null ? name : extractedData.get("Projektnamn");
        manager = manager != null ? manager : extractedData.get("Projektledares för- och efternamn");
        startDate = startDate != null ? startDate : LocalDate.parse(extractedData.get("Startdatum"));
        deadline = deadline != null ? deadline : LocalDate.parse(extractedData.get("Deadline"));
        fundingSource = fundingSource != null ? fundingSource : extractedData.get("Finansiär");
        researchProgram = researchProgram != null ? researchProgram : extractedData.get("Forskningsprogram");

        // Skapa ett nytt projekt
        Project newProject = new Project(0, name, manager, deadline, budgetYear1, budgetYear2, budgetYear3, spent, startDate, fundingSource, researchProgram);
        projectService.addProject(newProject);

        redirectAttributes.addFlashAttribute("message", "Projektet \"" + name + "\" har lagts till!");
        return "redirect:/projects";
    }

    @GetMapping("/edit-project/{id}")
    public String editProject(@PathVariable int id, Model model) {
        Project project = projectService.getProjectById(id);
        model.addAttribute("project", project);
        return "edit-project";
    }

    @PostMapping("/edit-project")
    public String updateProject(@RequestParam int id,
                                @RequestParam String name,
                                @RequestParam String manager,
                                @RequestParam String deadline,
                                RedirectAttributes redirectAttributes) {
        // Uppdatera projektinformationen
        projectService.updateProject(id, name, manager, deadline);

        // Lägg till ett framgångsmeddelande
        redirectAttributes.addFlashAttribute("message", "Projektet 2222 uppdaterades framgångsrikt!");

        // Omdirigera till projektlistan
        return "redirect:/projects";
    }


    @PostMapping("/{id}/upload-file")
    public String uploadFile(@PathVariable Long id,
                             @RequestParam("file") MultipartFile file,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            // Bearbeta filen med ExcelService
            Map<String, String> extractedData = excelService.processFile(file);

            // Lägg till extraherad data i modellen
            model.addAttribute("extractedData", extractedData);
            model.addAttribute("projectId", id);

            // Skicka användaren till granskningsvyn
            return "review-project";
        } catch (Exception e) {
            // Vid fel, skicka användaren tillbaka till redigeringssidan
            redirectAttributes.addFlashAttribute("errorMessage", "Fel vid bearbetning av filen: " + e.getMessage());
            return "redirect:/edit-project/" + id;
        }
    }

    @PostMapping("/{id}/confirm-updates")
    public String confirmUpdates(@PathVariable Integer id, @ModelAttribute("project") Project project, RedirectAttributes redirectAttributes) {
        // Hämta befintligt projekt från databasen
        Project existingProject = projectService.getProjectById(id);

        // Uppdatera fält med nya värden
        existingProject.setName(project.getName());
        existingProject.setManager(project.getManager());
        existingProject.setStartDate(project.getStartDate());
        existingProject.setDeadline(project.getDeadline());
        existingProject.setFundingSource(project.getFundingSource()); // Finansiär
        existingProject.setResearchProgram(project.getResearchProgram());

        // Spara ändringarna
        projectService.save(existingProject);

        // Meddela användaren
        redirectAttributes.addFlashAttribute("message", "Projektet har uppdaterats!");

        return "redirect:/projects";
    }

    @PostMapping("/upload-pdf")
    public String processPdf(@RequestParam("file") MultipartFile file, Model model) {
        Map<String, String> extractedData = new HashMap<>();

        try {
            extractedData = excelService.processPdfFile(file);
        } catch (Exception e) {
            model.addAttribute("error", "Kunde inte bearbeta PDF-filen: " + e.getMessage());
        }

        model.addAttribute("extractedData", extractedData);
        return "add-project"; // Visa samma formulär men nu med data från PDF
    }

    @PostMapping("/{id}/delete-project")
    public String deleteProject(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            projectService.deleteProjectById(id);
            redirectAttributes.addFlashAttribute("message", "Projektet togs bort!");
            return "redirect:/projects";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Kunde inte ta bort projektet. Kontrollera att det inte finns budget kopplad till projektet.");
            return "redirect:/projects";
        }
    }
}
