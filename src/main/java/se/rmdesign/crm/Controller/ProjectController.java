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
    public String addProjectPage() {
        return "add-project";
    }

    @PostMapping("/add")
    public String addProject(
            @RequestParam String name,
            @RequestParam String manager,
            @RequestParam LocalDate deadline,
            @RequestParam int budgetYear1,
            @RequestParam int budgetYear2,
            @RequestParam int budgetYear3,
            @RequestParam int spent,
            @RequestParam LocalDate startDate,
            RedirectAttributes redirectAttributes) {

        Project newProject = new Project(0, name, manager, deadline, budgetYear1, budgetYear2, budgetYear3, spent, startDate);
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
    public String confirmUpdates(@PathVariable Integer id,
                                 @RequestParam Map<String, String> formData,
                                 RedirectAttributes redirectAttributes) {
        try {
            Project project = projectService.getProjectById(id);

            if (formData.containsKey("name")) project.setName(formData.get("name"));
            if (formData.containsKey("manager")) project.setManager(formData.get("manager"));
            if (formData.containsKey("deadline")) project.setDeadline(LocalDate.parse(formData.get("deadline")));

            projectService.save(project);

            redirectAttributes.addFlashAttribute("message", "Projektet har 1111 uppdaterats framgångsrikt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fel vid uppdatering av projektet: " + e.getMessage());
        }
        return "redirect:/projects";
    }
}
