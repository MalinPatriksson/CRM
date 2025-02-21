package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.rmdesign.crm.Models.ProjectStatus;

import java.util.List;

@Repository
public interface ProjectStatusRepository extends JpaRepository<ProjectStatus, Long> {
    List<ProjectStatus> findByProjectIdOrderByStatusDateDesc(Long projectId); // 🔹 Ändrat till `statusDate`
    ProjectStatus findTopByProjectIdOrderByStatusDateDesc(Long projectId);   // 🔹 Ändrat till `statusDate`
}
