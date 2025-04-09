package se.rmdesign.crm.Services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

@Service
public class ExcelService {

    public Map<String, Object> processExcelFile(MultipartFile file) throws Exception {
        Map<String, Object> extractedData = new HashMap<>();

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xlsm"))) {
            throw new IllegalArgumentException("Endast .xlsx och .xlsm-filer stöds.");
        }

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // 🔹 Försök med Sheet 0 först
            boolean isNewFormat = isNewExcelFormat(sheet);

            if (!isNewFormat) {
                // 🔄 Om det inte är en ny mall, prova istället med gamla mallen på sheet 1
                sheet = workbook.getSheetAt(1);
                System.out.println("📄 Provade Sheet 1 istället (gammal mall)");
                isNewFormat = isNewExcelFormat(sheet); // kontrollera igen
            }

            System.out.println("📄 Detekterat format: " + (isNewFormat ? "NY MALL" : "GAMMAL MALL"));

            if (isNewFormat) {
                extractedData.put("Projektnamn", getMergedRowText(sheet, 6, "D", "I"));
                extractedData.put("Diarienummer", getCellValue(sheet, 1, "I"));
                extractedData.put("Projektledares för- och efternamn", getCellValue(sheet, 7, "D"));
                extractedData.put("Forskningsprogram", getCellValue(sheet, 8, "D"));
                extractedData.put("Finansiär", getCellValue(sheet, 9, "D"));
                extractedData.put("Startdatum", getCellValue(sheet, 14, "E"));
                extractedData.put("Deadline", getCellValue(sheet, 14, "I"));
            } else {
                extractedData.put("Projektnamn", getCellValue(sheet, 5, "D"));
                extractedData.put("Diarienummer", getCellValue(sheet, 1, "H"));
                extractedData.put("Projektledares för- och efternamn", getCellValue(sheet, 6, "D"));
                extractedData.put("Startdatum", getCellValue(sheet, 9, "D"));
                extractedData.put("Deadline", getCellValue(sheet, 9, "E"));
                extractedData.put("Forskningsprogram", extractResearchProgram(sheet));
                extractedData.put("Finansiär", extractFinancier(sheet));
            }

            Map<String, Object> budgetData = extractBudgetData(sheet, isNewFormat);
            extractedData.put("BudgetRows", budgetData.get("BudgetRows"));
            extractedData.put("Years", budgetData.get("Years"));

            return extractedData;

        } catch (Exception e) {
            System.err.println("❌ Fel vid läsning av Excel-fil: " + e.getMessage());
            throw new RuntimeException("Kunde inte läsa Excel-filen: " + e.getMessage(), e);
        }
    }

    private boolean isNewExcelFormat(Sheet sheet) {
        // Gå igenom de 10 första raderna och kolumn A–E
        for (int rowIndex = 0; rowIndex <= 10; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            for (int colIndex = 0; colIndex <= 4; colIndex++) {
                Cell cell = row.getCell(colIndex);
                String value = parseCellValue(cell).trim().toLowerCase();
                System.out.println("🔍 Letar i rad " + (rowIndex + 1) + ", kolumn " + (colIndex + 1) + ": '" + value + "'");
                if (value.contains("projektnamn/akronym")) {
                    System.out.println("✅ Ny mall identifierad på rad " + (rowIndex + 1));
                    return true;
                }
            }
        }
        System.out.println("❌ Kunde inte identifiera ny mall. Antas vara gammal.");
        return false;
    }

    private String getMergedRowText(Sheet sheet, int rowNumber, String startCol, String endCol) {
        int startIndex = columnLetterToIndex(startCol);
        int endIndex = columnLetterToIndex(endCol);
        Row row = sheet.getRow(rowNumber - 1);

        if (row == null) return "";

        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i <= endIndex; i++) {
            Cell cell = row.getCell(i);
            String value = parseCellValue(cell).trim();

            if (!value.isEmpty()) {
                // Rätta till årtal som t.ex. "2025.0" → "2025"
                if (value.matches("\\d{4}\\.0")) {
                    value = value.replace(".0", "");
                }

                if (result.length() > 0) result.append(" ");
                result.append(value);
            }
        }

        return result.toString();
    }



    /*
    public Map<String, Object> processExcelFile(MultipartFile file) throws Exception {
        Map<String, Object> extractedData = new HashMap<>();

        // Kontrollera filtyp
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xlsm"))) {
            throw new IllegalArgumentException("Endast .xlsx och .xlsm-filer stöds.");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(1); // 🔹 Alltid första bladet
            System.out.println("📄 Läser Excel-fil: " + filename);

            // 🔹 Fasta celler
            extractedData.put("Projektnamn", getCellValue(sheet, 5, "D"));
            extractedData.put("Diarienummer", getCellValue(sheet, 1, "H"));
            extractedData.put("Projektledares för- och efternamn", getCellValue(sheet, 6, "D"));
            extractedData.put("Startdatum", getCellValue(sheet, 9, "D"));
            extractedData.put("Deadline", getCellValue(sheet, 9, "E"));

            // 🔹 Forskningsprogram & finansiär
            extractedData.put("Forskningsprogram", extractResearchProgram(sheet));
            extractedData.put("Finansiär", extractFinancier(sheet));

            // 🔹 Budgetrader och år
            Map<String, Object> budgetData = extractBudgetData(sheet);
            extractedData.put("BudgetRows", budgetData.get("BudgetRows"));
            extractedData.put("Years", budgetData.get("Years"));

            System.out.println("✅ Extraherad data: " + extractedData);

        } catch (Exception e) {
            System.err.println("❌ Fel vid läsning av Excel-fil: " + e.getMessage());
            throw new RuntimeException("Kunde inte läsa Excel-filen: " + e.getMessage(), e);
        }

        return extractedData;
    }
*/

    private String extractResearchProgram(Sheet sheet) {
        int programRow = 10; // 🔹 Rad 11 i Excel (0-index i POI)
        int answerCol1 = columnLetterToIndex("H"); // 🔹 Primär svarskolumn (H)
        int answerCol2 = columnLetterToIndex("I"); // 🔹 Sekundär svarskolumn (I)

        Row row = sheet.getRow(programRow);
        if (row == null) {
            System.out.println("❌ Rad 11 saknas i arket!");
            return "";
        }

        String answer = parseCellValue(row.getCell(answerCol1)); // 🔹 Försök hämta från kolumn H
        if (!answer.isEmpty()) {
            System.out.println("✅ Forskningsprogram hittat i kolumn H: " + answer);
            return answer;
        }

        answer = parseCellValue(row.getCell(answerCol2)); // 🔹 Om H är tom, hämta från kolumn I
        if (!answer.isEmpty()) {
            System.out.println("✅ Forskningsprogram hittat i kolumn I: " + answer);
            return answer;
        }

        System.out.println("⚠️ Forskningsprogram hittades men saknar värde!");
        return ""; // 🔹 Lämna tomt istället för "Ej angivet"
    }

    public Map<String, Object> extractBudgetData(Sheet sheet, boolean isNewFormat) {
        Map<String, Object> budgetData = new HashMap<>();
        List<String> years = extractYears(sheet, isNewFormat);
        int yearStartCol = columnLetterToIndex("D");
        int yearRowIndex = isNewFormat ? 19 : 12;
        int totalCol = findTotalColumn(sheet, yearRowIndex);

        int budgetStartRow = isNewFormat ? 19 : 12;
        int totalBudgetRow = findRowIndex(sheet, "Totala intäkter");

        if (budgetStartRow == -1 || totalBudgetRow == -1) {
            System.err.println("❌ Fel: 'Intäkter' eller 'Totala intäkter' hittades inte i Excel-filen.");
            return budgetData;
        }

        System.out.println("📌 Börjar läsa budget från rad " + budgetStartRow + " till " + totalBudgetRow);

        List<Map<String, String>> budgetRows = new ArrayList<>();
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE);

        for (int rowIndex = budgetStartRow + 1; rowIndex <= totalBudgetRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String rowTitle = parseCellValue(row.getCell(columnLetterToIndex("A")));
            if (rowTitle.isEmpty() || rowTitle.equals(".")) continue;

            Map<String, String> budgetRow = new LinkedHashMap<>();
            budgetRow.put("Rubrik", rowTitle);

            for (int i = 0; i < years.size(); i++) {
                String year = years.get(i);
                double rawValue = getCellValueAsDouble(row.getCell(yearStartCol + i));
                int roundedValue = (int) Math.round(rawValue);
                String formattedValue = (roundedValue == 0) ? "0" : numberFormat.format(roundedValue);
                budgetRow.put(year, formattedValue);
            }

            if (totalCol != -1) {
                double totalValue = getCellValueAsDouble(row.getCell(totalCol));
                int roundedTotal = (int) Math.round(totalValue);
                String formattedTotal = (roundedTotal == 0) ? "0" : numberFormat.format(roundedTotal);
                budgetRow.put("Total", formattedTotal);
            }

            budgetRows.add(budgetRow);
            System.out.println("✅ Sparade budgetrad: " + budgetRow);
        }

        System.out.println("📌 Totalt extraherade budgetrader: " + budgetRows.size());
        budgetData.put("Years", years);
        budgetData.put("BudgetRows", budgetRows);

        return budgetData;
    }

    private List<String> extractYears(Sheet sheet, boolean isNewFormat) {
        List<String> years = new ArrayList<>();
        int yearRowIndex = isNewFormat ? 19 : 12;
        Row yearRow = sheet.getRow(yearRowIndex);

        if (yearRow != null) {
            for (int col = columnLetterToIndex("D"); col < columnLetterToIndex("Z"); col++) {
                Cell cell = yearRow.getCell(col);
                if (cell == null) continue;

                String yearValue = parseCellValue(cell);

                if (yearValue.equalsIgnoreCase("Totalt")) {
                    break;
                }

                if (yearValue.matches("\\d{4}")) {
                    years.add(yearValue);
                } else {
                    try {
                        int numericYear = (int) cell.getNumericCellValue();
                        years.add(String.valueOf(numericYear));
                    } catch (Exception e) {
                        System.err.println("⚠️ Problem med årtal i kolumn " + col + ": " + yearValue);
                    }
                }
            }
        }

        if (years.isEmpty()) {
            System.err.println("❌ Fortfarande inga år funna! Kolla om cellerna är tomma eller har konstig formatering.");
        } else {
            System.out.println("📌 Hittade årtal: " + years);
        }

        return years;
    }


    private int findTotalColumn(Sheet sheet, int yearRowIndex) {
        // Försök hitta med text först
        int totalCol = findColumnIndex(sheet, "Totalt", yearRowIndex);

        // Om inte hittad, ta sista icke-tomma kolumnen istället
        if (totalCol == -1) {
            Row yearRow = sheet.getRow(yearRowIndex);
            if (yearRow != null) {
                for (int col = yearRow.getLastCellNum() - 1; col >= columnLetterToIndex("D"); col--) {
                    Cell cell = yearRow.getCell(col);
                    if (cell != null && !parseCellValue(cell).isEmpty()) {
                        totalCol = col;
                        System.out.println("📍 'Totalt' hittades inte via text, använder sista icke-tomma kolumnen: " + col);
                        break;
                    }
                }
            }
        } else {
            System.out.println("📍 'Totalt'-kolumn hittades via textmatchning på kolumn: " + totalCol);
        }

        return totalCol;
    }


    // 🔹 Hämtar cellvärde baserat på radnummer och kolumnbokstav (A, B, C...)
    private String getCellValue(Sheet sheet, int rowNumber, String columnLetter) {
        int columnIndex = columnLetterToIndex(columnLetter);
        Row row = sheet.getRow(rowNumber - 1); // Excel-rader börjar från 1, men POI använder 0-index

        if (row != null) {
            Cell cell = row.getCell(columnIndex);
            return parseCellValue(cell);
        }
        return "⚠️ TOM CELL!";
    }

    // 🔹 Konverterar kolumnbokstav (A, B, C...) till index (0, 1, 2...)
    private int columnLetterToIndex(String columnLetter) {
        int columnIndex = 0;
        for (char ch : columnLetter.toCharArray()) {
            columnIndex = columnIndex * 26 + (ch - 'A' + 1);
        }
        return columnIndex - 1;
    }

    private int findRowIndex(Sheet sheet, String searchText) {
        for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String cellValue = parseCellValue(row.getCell(columnLetterToIndex("A"))); // Läs första kolumnen (A)
            if (cellValue.equalsIgnoreCase(searchText)) {
                return i;
            }
        }
        return -1; // Returnera -1 om vi inte hittar raden
    }

    private String extractFinancier(Sheet sheet) {
        int financierRowIndex = findRowIndex(sheet, "Finansiär");

        if (financierRowIndex == -1) {
            System.err.println("❌ Hittade inte 'Finansiär' i Excel-filen.");
            return "";
        }

        Row row = sheet.getRow(financierRowIndex);

        // 🔹 Loopar igenom kolumner från B till E
        for (int col = columnLetterToIndex("B"); col <= columnLetterToIndex("E"); col++) {
            Cell cell = row.getCell(col);
            String cellValue = parseCellValue(cell);

            if (!cellValue.isEmpty() && !cellValue.matches(".*\\d+.*")) { // Hittar första textvärdet
                System.out.println("✅ Hittade 'Finansiär': " + cellValue);
                return cellValue;
            }
        }

        System.err.println("❌ Ingen giltig finansiär hittades i raden.");
        return "";
    }

        private int findColumnIndex(Sheet sheet, String columnName, int rowIndex) {
            Row headerRow = sheet.getRow(rowIndex);
            if (headerRow != null) {
                for (int col = columnLetterToIndex("D"); col < columnLetterToIndex("Z"); col++) {
                    Cell cell = headerRow.getCell(col);
                    if (cell != null && parseCellValue(cell).equalsIgnoreCase(columnName)) {
                        return col;
                    }
                }
            }
            return -1;
        }



    // 🔹 Förbättrad metod för att hämta numeriska värden
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return 0.0; // Returnera 0 om cellen är null
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return 0.0; // Hantera tomma celler
                    return Double.parseDouble(value.replace(",", ".")); // Hantera eventuella kommatecken
                case FORMULA:
                    return cell.getNumericCellValue();
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Fel vid cellkonvertering: " + e.getMessage());
            return 0.0;
        }
    }

    // 🔹 Hanterar olika celltyper och returnerar deras värde
    private String parseCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}