package se.rmdesign.crm.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Repos.ProjectRepository;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final BudgetEntryService budgetEntryService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, BudgetEntryService budgetEntryService) {
        this.projectRepository = projectRepository;
        this.budgetEntryService = budgetEntryService;
    }

    public Double calculateTotalBudget(Long projectId) {
        return budgetEntryService.getTotalIncomeForProject(projectId);
    }


    public List<Project> getAllProjects(Sort by) {
        return projectRepository.findAll();
    }
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public void deleteProjectById(Long id) {
        projectRepository.deleteById(id);
    }

    public List<Project> searchProjects(String keyword) {
        return projectRepository.findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(keyword, keyword);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projekt hittades inte"));
    }

    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }
}

