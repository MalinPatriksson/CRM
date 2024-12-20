package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.rmdesign.crm.Models.ProjectBudget;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<ProjectBudget, Integer> {
    List<ProjectBudget> findByProjectId(int project_id);
    List<ProjectBudget> findByProjectIdOrderByYear(int project_id);
    List<ProjectBudget> findByProjectIdAndYear(Integer projectId, Integer year);

}

