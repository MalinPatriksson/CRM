package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;

import java.util.List;


@Repository
public interface BudgetEntryValueRepository extends JpaRepository<BudgetEntryValue, Long> {

    // Hämta alla budgetvärden för en viss budgetpost
    List<BudgetEntryValue> findByBudgetEntry(BudgetEntry budgetEntry);

    // Radera alla budgetvärden för en viss budgetpost
    @Transactional
    void deleteByBudgetEntry(BudgetEntry budgetEntry);
}
