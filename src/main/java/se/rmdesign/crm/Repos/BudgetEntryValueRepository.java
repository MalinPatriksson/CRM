package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;

import java.util.List;


@Repository
public interface BudgetEntryValueRepository extends JpaRepository<BudgetEntryValue, Long> {

    List<BudgetEntryValue> findByBudgetEntry(BudgetEntry budgetEntry);

    @Transactional
    void deleteByBudgetEntry(BudgetEntry budgetEntry);
}
