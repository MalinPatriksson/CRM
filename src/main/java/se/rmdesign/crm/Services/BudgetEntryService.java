package se.rmdesign.crm.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Repos.BudgetEntryRepository;
import se.rmdesign.crm.Repos.BudgetEntryValueRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BudgetEntryService {
    private final BudgetEntryRepository budgetEntryRepository;
    private final BudgetEntryValueService budgetEntryValueService;
    private final BudgetEntryValueRepository budgetEntryValueRepository;

    public BudgetEntryService(BudgetEntryRepository budgetEntryRepository, BudgetEntryValueService budgetEntryValueService, BudgetEntryValueRepository budgetEntryValueRepository) {
        this.budgetEntryRepository = budgetEntryRepository;
        this.budgetEntryValueService = budgetEntryValueService;
        this.budgetEntryValueRepository = budgetEntryValueRepository;
    }

    @Transactional
    public void updateBudgetEntries(List<BudgetEntry> updatedEntries, Project project, List<Long> deletedBudgetRows) {
        Map<Long, BudgetEntry> existingEntriesMap = findByProjectAsMap(project);
        Set<Long> updatedIds = updatedEntries.stream()
                .map(BudgetEntry::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<BudgetEntry> entriesToSave = new ArrayList<>();

        for (BudgetEntry updatedEntry : updatedEntries) {
            if (existingEntriesMap.containsKey(updatedEntry.getId())) {
                BudgetEntry existingEntry = existingEntriesMap.get(updatedEntry.getId());

                existingEntry.setTitle(updatedEntry.getTitle());
                existingEntry.setTotal(updatedEntry.getTotal());

                updateBudgetValues(existingEntry, updatedEntry.getBudgetValues());

                entriesToSave.add(existingEntry);
            } else {
                entriesToSave.add(updatedEntry);
            }
        }

        for (Long existingId : existingEntriesMap.keySet()) {
            if (!updatedIds.contains(existingId) && (deletedBudgetRows == null || !deletedBudgetRows.contains(existingId))) {
                System.out.println("🗑️ Markerar budgetrad för borttagning: " + existingId);
                BudgetEntry toDelete = existingEntriesMap.get(existingId);
                budgetEntryRepository.delete(toDelete);
            }
        }

        if (!entriesToSave.isEmpty()) {
            saveAll(entriesToSave, project);
            System.out.println("✅ Budgetrader uppdaterade/sparade: " + entriesToSave.size());
        }
    }

    @Transactional
    public void updateBudgetValues(BudgetEntry budgetEntry, List<BudgetEntryValue> newValues) {
        List<BudgetEntryValue> existingValues = budgetEntryValueService.findByBudgetEntry(budgetEntry);
        Map<Integer, BudgetEntryValue> existingValueMap = existingValues.stream()
                .collect(Collectors.toMap(BudgetEntryValue::getYear, v -> v, (existing, replacement) -> {
                    existing.setValue(replacement.getValue());
                    return existing;
                }));

        for (BudgetEntryValue newValue : newValues) {
            newValue.setBudgetEntry(budgetEntry);

            if (existingValueMap.containsKey(newValue.getYear())) {
                existingValueMap.get(newValue.getYear()).setValue(newValue.getValue());
                budgetEntryValueRepository.save(existingValueMap.get(newValue.getYear()));
            } else {
                budgetEntryValueRepository.save(newValue);
            }
        }
    }

    @Transactional
    public void saveAll(List<BudgetEntry> entries, Project project) {
        for (BudgetEntry entry : entries) {
            entry.setProject(project);

            // 🔹 Spara först själva entry så den får ett ID
            BudgetEntry savedEntry = budgetEntryRepository.save(entry);

            // 🔹 Spara alla värden (BudgetEntryValue) kopplade till entry
            for (BudgetEntryValue value : entry.getBudgetValues()) {
                value.setBudgetEntry(savedEntry);
                budgetEntryValueRepository.save(value);
            }
        }
    }


    public List<BudgetEntry> findByProject(Project project) {
        List<BudgetEntry> budgetEntries = budgetEntryRepository.findByProject(project);

        // 🔹 Hämta och sätt budgetvärden för varje BudgetEntry
        for (BudgetEntry entry : budgetEntries) {
            List<BudgetEntryValue> budgetValues = budgetEntryValueService.findByBudgetEntry(entry);
            entry.setBudgetValues(budgetValues); // ✅ Korrekt - behåller det som en List
        }

        return budgetEntries;
    }

    public Map<Long, BudgetEntry> findByProjectAsMap(Project project) {
        return budgetEntryRepository.findByProject(project)
                .stream()
                .collect(Collectors.toMap(BudgetEntry::getId, entry -> entry));
    }


    public List<BudgetEntry> processBudgetEntries(Map<String, String> budgetRows, Project project) {
        Map<Long, BudgetEntry> entryMap = new HashMap<>();
        Map<Long, List<BudgetEntryValue>> valueMap = new HashMap<>();

        Pattern pattern = Pattern.compile("budgetRows\\[(\\d+)]\\[(.+?)]");

        for (Map.Entry<String, String> entry : budgetRows.entrySet()) {
            Matcher matcher = pattern.matcher(entry.getKey());
            if (!matcher.matches()) continue;

            Long index = Long.parseLong(matcher.group(1));
            String key = matcher.group(2);
            String value = entry.getValue();

            entryMap.putIfAbsent(index, new BudgetEntry(project, "", 0.0));
            BudgetEntry currentEntry = entryMap.get(index);

            if (key.equalsIgnoreCase("Rubrik")) {
                currentEntry.setTitle(value.trim());
            } else if (key.equalsIgnoreCase("Total")) {
                currentEntry.setTotal(parseBudgetValue(value));
            } else if (key.matches("\\d{4}")) { // År
                int year = Integer.parseInt(key);
                double val = parseBudgetValue(value);
                valueMap.computeIfAbsent(index, k -> new ArrayList<>()).add(new BudgetEntryValue(currentEntry, year, val));
            }
        }

        // Koppla alla values till respektive BudgetEntry
        for (Map.Entry<Long, BudgetEntry> entry : entryMap.entrySet()) {
            List<BudgetEntryValue> values = valueMap.getOrDefault(entry.getKey(), new ArrayList<>());
            entry.getValue().setBudgetValues(values);
        }

        List<BudgetEntry> entriesToSave = new ArrayList<>(entryMap.values());
        budgetEntryRepository.saveAll(entriesToSave); // ⬅️ Detta sparar till databasen

        // Spara även varje BudgetEntryValue separat (eller via Cascade, beroende på din setup)
        for (BudgetEntry entry : entriesToSave) {
            for (BudgetEntryValue val : entry.getBudgetValues()) {
                val.setBudgetEntry(entry); // Se till att kopplingen sätts
            }
            budgetEntryValueRepository.saveAll(entry.getBudgetValues());
        }

        return entriesToSave;
    }


    public Optional<BudgetEntry> findTotalIncomeEntry(Long projectId) {
        return budgetEntryRepository.findByProjectId(projectId)
                .stream()
                .filter(entry -> "Totala intäkter".equalsIgnoreCase(entry.getTitle()))
                .findFirst();
    }

    private double parseBudgetValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0; // Returnera 0.0 istället för att kasta NumberFormatException
        }
        return Double.parseDouble(value.replaceAll("[^\\d.]", ""));
    }

    public Double getTotalIncomeForProject(Long projectId) {
        BudgetEntry totalEntry = budgetEntryRepository.findTotalIncomeForProject(projectId);
        return totalEntry != null ? totalEntry.getTotal() : 0.0;
    }

}
