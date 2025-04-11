package se.rmdesign.crm.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.ProjectStatus;
import se.rmdesign.crm.Repos.ProjectStatusRepository;
import java.util.List;

@Service
public class ProjectStatusService {

    private final ProjectStatusRepository projectStatusRepository;

    public ProjectStatusService(ProjectStatusRepository projectStatusRepository) {
        this.projectStatusRepository = projectStatusRepository;
    }

    @Transactional
    public void saveProjectStatus(ProjectStatus projectStatus) {
        projectStatusRepository.save(projectStatus);
    }

    public ProjectStatus getLatestStatus(Long projectId) {
        return projectStatusRepository.findTopByProjectIdOrderByStatusDateDesc(projectId);
    }

    public List<ProjectStatus> getStatusHistory(Long projectId) {
        return projectStatusRepository.findByProjectIdOrderByStatusDateDesc(projectId);
    }

    public double calculateWeightedBudget(double totalBudget, ProjectStatus status, boolean useWeighted) {
        if (!useWeighted || status == null) return totalBudget;

        int weighting = status.getWeighting();
        return totalBudget * (weighting / 100.0);
    }

    public double getProjectBudgetWithWeighting(Long projectId, double totalBudget, boolean useWeighted, String filterStatus) {
        ProjectStatus latest = getLatestStatus(projectId);
        if (latest == null) return useWeighted ? 0.0 : totalBudget;

        if (useWeighted && (filterStatus == null || filterStatus.isEmpty() || latest.getStatus().equalsIgnoreCase(filterStatus))) {
            return totalBudget * (latest.getWeighting() / 100.0);
        }

        return totalBudget;
    }

}
