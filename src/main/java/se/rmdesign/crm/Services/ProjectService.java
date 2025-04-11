package se.rmdesign.crm.Services;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import se.rmdesign.crm.Models.BudgetEntry;
import se.rmdesign.crm.Models.BudgetEntryValue;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Repos.ProjectRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final BudgetEntryService budgetEntryService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, BudgetEntryService budgetEntryService) {
        this.projectRepository = projectRepository;
        this.budgetEntryService = budgetEntryService;
    }

    public List<Project> getAllProjects(Sort by) {
        return projectRepository.findAll();
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public void deleteProjectById(Long id) {
        projectRepository.deleteById(id);
    }

    public List<Project> searchProjects(String keyword) {
        return projectRepository.findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(keyword, keyword);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projekt hittades inte"));
    }

    public Project saveProject(Project project) {
        return projectRepository.save(project);
    }

    public List<Project> filterProjects(Map<String, String> filters) {
        List<Project> all = getAllProjects();

        return all.stream().filter(p -> {
            boolean match = true;
            if (filters.containsKey("years")) {
                List<Integer> years = Arrays.stream(filters.get("years").split(","))
                        .map(Integer::parseInt).toList();
                match &= p.getBudgetEntries().stream()
                        .flatMap(e -> e.getBudgetValues().stream())
                        .anyMatch(v -> years.contains(v.getYear()));
            }
            if (filters.containsKey("funders")) {
                match &= Arrays.asList(filters.get("funders").split(","))
                        .contains(p.getFundingSource());
            }
            if (filters.containsKey("persons")) {
                match &= Arrays.asList(filters.get("persons").split(","))
                        .contains(p.getManager());
            }
            if (filters.containsKey("statuses")) {
                match &= Arrays.asList(filters.get("statuses").split(","))
                        .contains(p.getCurrentStatus());
            }
            if (filters.containsKey("academies")) {
                List<String> selectedAcademies = Arrays.stream(filters.get("academies").split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank() && !s.equalsIgnoreCase("Alla"))
                        .toList();

                match &= p.getAcademies() != null &&
                        selectedAcademies.stream().anyMatch(selected -> p.getAcademies().contains(selected));
            }


            if (filters.containsKey("programs")) {
                match &= Arrays.asList(filters.get("programs").split(","))
                        .contains(p.getResearchProgram());
            }
            return match;
        }).toList();
    }

    public void fillProjectToSheet(Project project, Sheet sheet) {
        Workbook workbook = sheet.getWorkbook();
        CellStyle bold = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        bold.setFont(boldFont);

        CellStyle bordered = workbook.createCellStyle();
        bordered.setBorderBottom(BorderStyle.THIN);
        bordered.setBorderTop(BorderStyle.THIN);
        bordered.setBorderLeft(BorderStyle.THIN);
        bordered.setBorderRight(BorderStyle.THIN);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.cloneStyleFrom(bold);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        int rowIdx = 0;
        String[][] info = {
                {"Projektnamn:", project.getName()},
                {"Projektledare:", project.getManager()},
                {"Finansiär:", project.getFundingSource()},
                {"Akademi:", String.join(", ", project.getAcademies())},
                {"Forskningsprogram:", project.getResearchProgram()},
                {"Startdatum:", project.getStartDate().toString()},
                {"Deadline:", project.getDeadline().toString()}
        };

        for (String[] pair : info) {
            Row row = sheet.createRow(rowIdx++);
            Cell c1 = row.createCell(0);
            c1.setCellValue(pair[0]);
            c1.setCellStyle(bold);
            Cell c2 = row.createCell(1);
            c2.setCellValue(pair[1]);
        }

        rowIdx++; // empty row

        Set<Integer> allYears = new TreeSet<>();
        for (BudgetEntry be : project.getBudgetEntries()) {
            allYears.addAll(be.getBudgetValues().stream().map(BudgetEntryValue::getYear).toList());
        }

        Row header = sheet.createRow(rowIdx++);
        Cell h0 = header.createCell(0);
        h0.setCellValue("Intäktstyp");
        h0.setCellStyle(headerStyle);
        int colIdx = 1;
        for (Integer year : allYears) {
            Cell c = header.createCell(colIdx++);
            c.setCellValue(year);
            c.setCellStyle(headerStyle);
        }
        Cell hTotal = header.createCell(colIdx);
        hTotal.setCellValue("Total");
        hTotal.setCellStyle(headerStyle);

        for (BudgetEntry be : project.getBudgetEntries()) {
            Row row = sheet.createRow(rowIdx++);
            Cell typeCell = row.createCell(0);
            typeCell.setCellValue(be.getTitle());
            typeCell.setCellStyle(bordered);
            Map<Integer, Double> valueMap = be.getBudgetValues().stream()
                    .collect(Collectors.toMap(BudgetEntryValue::getYear, BudgetEntryValue::getValue));
            double total = 0;
            colIdx = 1;
            for (Integer year : allYears) {
                double val = valueMap.getOrDefault(year, 0.0);
                Cell c = row.createCell(colIdx++);
                c.setCellValue(val);
                c.setCellStyle(bordered);
                total += val;
            }
            Cell totalCell = row.createCell(colIdx);
            totalCell.setCellValue(total);
            totalCell.setCellStyle(bordered);
        }

        sheet.autoSizeColumn(0);
        for (int i = 1; i <= allYears.size() + 1; i++) {
            sheet.setColumnWidth(i, 15 * 256);
        }
    }

    public void exportSingleProject(Project project, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(project.getName().substring(0, Math.min(31, project.getName().length())));
        fillProjectToSheet(project, sheet);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + project.getName() + "-budget.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}