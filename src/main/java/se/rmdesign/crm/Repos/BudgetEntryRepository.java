package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.Project;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {

    @Query("SELECT b FROM BudgetEntry b WHERE b.project.id = :projectId AND b.title = 'Totala intäkter' ORDER BY b.id ASC LIMIT 1")
    BudgetEntry findTotalIncomeForProject(@Param("projectId") Long projectId);

    List<BudgetEntry> findByProject(Project project);

    @Query("SELECT b FROM BudgetEntry b WHERE b.project = :project")
    List<BudgetEntry> findByProjectAsList(@Param("project") Project project);

    default Map<Long, BudgetEntry> findByProjectAsMap(Project project) {
        return findByProjectAsList(project).stream()
                .collect(Collectors.toMap(BudgetEntry::getId, entry -> entry));
    }

    @Query("SELECT b FROM BudgetEntry b WHERE b.project.id = :projectId")
    List<BudgetEntry> findByProjectId(@Param("projectId") Long projectId);


    @Transactional
    void deleteById(Long id);
}
