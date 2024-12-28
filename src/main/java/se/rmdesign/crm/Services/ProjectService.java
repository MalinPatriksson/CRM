package se.rmdesign.crm.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Repos.ProjectRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // Hämta alla projekt
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Lägg till ett projekt
    public void addProject(Project project) {
        projectRepository.save(project);
    }
    public Page<Project> getProjectsPaginated(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    public Page<Project> searchProjectsPaginated(String keyword, Pageable pageable) {
        return projectRepository.findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(keyword, keyword, pageable);
    }

    public List<Project> searchProjects(String keyword) {
        return projectRepository.findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(keyword, keyword);
    }
    public void updateProject(int id, String name, String manager, String deadline) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projekt ej hittat"));
        project.setName(name);
        project.setManager(manager);
        project.setDeadline(LocalDate.parse(deadline));
        projectRepository.save(project);
    }

    public Project getProjectById(int id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent()) {
            return project.get();
        } else {
            throw new RuntimeException("Projektet med ID " + id + " hittades inte.");
        }
    }
    public void save(Project project) {
        projectRepository.save(project); // Sparar projektet i databasen
    }

    public void deleteProjectById(int id) {
        if (projectRepository.existsById(id)) {
            projectRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Projekt med ID " + id + " existerar inte.");
        }
    }
}
