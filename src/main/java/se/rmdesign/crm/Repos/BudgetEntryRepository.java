package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.Project;

import java.util.List;

@Repository
public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM BudgetEntry b WHERE b.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    List<BudgetEntry> findByProject(Project project);

}

