package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectStatusRepository extends JpaRepository<ProjectStatus, Long> {
    List<ProjectStatus> findByProjectIdOrderByStatusDateDesc(Long projectId);
    ProjectStatus findTopByProjectIdOrderByStatusDateDesc(Long projectId);
    Optional<ProjectStatus> findByProjectAndStatusDate(Project project, LocalDate statusDate);

}
