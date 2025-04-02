package se.rmdesign.crm.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;
import se.rmdesign.crm.Repos.BudgetEntryValueRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class BudgetEntryValueService {
    private final BudgetEntryValueRepository budgetEntryValueRepository;

    public BudgetEntryValueService(BudgetEntryValueRepository budgetEntryValueRepository) {
        this.budgetEntryValueRepository = budgetEntryValueRepository;
    }
    public List<BudgetEntryValue> findByBudgetEntry(BudgetEntry budgetEntry) {
        return budgetEntryValueRepository.findByBudgetEntry(budgetEntry);
    }

    @Transactional
    public void save(BudgetEntryValue budgetEntryValue) {
        budgetEntryValueRepository.save(budgetEntryValue);
    }

    @Transactional
    public void saveAll(Collection<BudgetEntryValue> budgetEntryValues) {
        budgetEntryValueRepository.saveAll(new ArrayList<>(budgetEntryValues));
    }


    @Transactional
    public void deleteByBudgetEntry(BudgetEntry budgetEntry) {
        budgetEntryValueRepository.deleteByBudgetEntry(budgetEntry);
    }
}

