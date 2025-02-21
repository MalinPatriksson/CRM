package se.rmdesign.crm.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectStatus;
import se.rmdesign.crm.Repos.ProjectStatusRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectStatusService {

    private final ProjectStatusRepository projectStatusRepository;

    public ProjectStatusService(ProjectStatusRepository projectStatusRepository) {
        this.projectStatusRepository = projectStatusRepository;
    }

    /**
     * 🔹 Spara en ny status för ett projekt.
     */
    @Transactional
    public void saveProjectStatus(ProjectStatus projectStatus) {
        projectStatusRepository.save(projectStatus);
    }

    // 🔹 Hämta senaste status för ett projekt
    public ProjectStatus getLatestStatus(Long projectId) {
        return projectStatusRepository.findTopByProjectIdOrderByStatusDateDesc(projectId);
    }

    // 🔹 Hämta status-historik
    public List<ProjectStatus> getStatusHistory(Long projectId) {
        return projectStatusRepository.findByProjectIdOrderByStatusDateDesc(projectId);
    }
}
