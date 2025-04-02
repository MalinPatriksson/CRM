package se.rmdesign.crm.Services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelService {


    public Map<String, Object> processExcelFile(MultipartFile file) throws Exception {
        Map<String, Object> extractedData = new HashMap<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("📄 Läser Excel-fil: " + file.getOriginalFilename());

            // 🔹 Hämtar fasta värden från specifika rader och kolumner
            extractedData.put("Projektnamn", getCellValue(sheet, 5, "D"));
            extractedData.put("Diarienummer", getCellValue(sheet, 1, "H"));
            extractedData.put("Projektledares för- och efternamn", getCellValue(sheet, 6, "D"));
            extractedData.put("Startdatum", getCellValue(sheet, 9, "D"));
            extractedData.put("Deadline", getCellValue(sheet, 9, "E"));
            String researchProgram = extractResearchProgram(sheet);
            extractedData.put("Forskningsprogram", researchProgram);


            // 🔹 Hämta finansiär med dynamisk metod
            String financier = extractFinancier(sheet);
            extractedData.put("Finansiär", financier);

            // 🔹 Hämta budgetrader och årtal
            Map<String, Object> budgetData = extractBudgetData(sheet);
            extractedData.put("BudgetRows", budgetData.get("BudgetRows"));
            extractedData.put("Years", budgetData.get("Years"));

            System.out.println("✅ Extraherad data: " + extractedData);
            System.out.println("BudgetRows: " + extractedData.get("BudgetRows"));

        } catch (Exception e) {
            System.err.println("❌ Fel vid läsning av Excel-fil: " + e.getMessage());
        }

        return extractedData;
    }

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


    public Map<String, Object> extractBudgetData(Sheet sheet) {
        Map<String, Object> budgetData = new HashMap<>();
        List<String> years = extractYears(sheet); // Hämta dynamiskt antal år
        int yearStartCol = columnLetterToIndex("D"); // Första årskolumn
        int totalCol = findColumnIndex(sheet, "Totalt"); // Dynamisk sökning efter "Totalt"-kolumnen

        if (totalCol == -1) {
            System.err.println("❌ 'Totalt' hittades inte! Budget kan bli fel.");
        }

        int budgetStartRow = findRowIndex(sheet, "Intäkter"); // Hitta första budgetraden
        int totalBudgetRow = findRowIndex(sheet, "Totala intäkter"); // Hitta slutet av budgetraderna

        if (budgetStartRow == -1 || totalBudgetRow == -1) {
            System.err.println("❌ Fel: 'Intäkter' eller 'Totala intäkter' hittades inte i Excel-filen.");
            return budgetData;
        }

        System.out.println("📌 Börjar läsa budget från rad " + budgetStartRow + " till " + totalBudgetRow);

        List<Map<String, String>> budgetRows = new ArrayList<>();
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE); // 🔹 Svenska formatet med mellanslag

        for (int rowIndex = budgetStartRow + 1; rowIndex <= totalBudgetRow; rowIndex++) { // 🔹 Hoppa över "Intäkter"
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            String rowTitle = parseCellValue(row.getCell(columnLetterToIndex("A"))); // Hämta rubriken

            // 🔹 Hoppa över tomma rader eller rader med bara en punkt
            if (rowTitle.isEmpty() || rowTitle.equals(".")) continue;

            Map<String, String> budgetRow = new LinkedHashMap<>();
            budgetRow.put("Rubrik", rowTitle);

            // 🔹 Hämta värden för varje år och formatera snyggt
            for (int i = 0; i < years.size(); i++) {
                String year = years.get(i);
                double rawValue = getCellValueAsDouble(row.getCell(yearStartCol + i));
                int roundedValue = (int) Math.round(rawValue);
                String formattedValue = (roundedValue == 0) ? "0" : numberFormat.format(roundedValue); // 🔹 Formatera snyggt
                budgetRow.put(year, formattedValue);
            }

            // 🔹 Hämta totalvärdet om "Totalt" hittades
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

    private List<String> extractYears(Sheet sheet) {
        List<String> years = new ArrayList<>();
        int yearRowIndex = 12; // Rad 13 i Excel (0-index)
        Row yearRow = sheet.getRow(yearRowIndex);
        int totalColIndex = -1; // Dynamisk kolumn för "Totalt"

        if (yearRow != null) {
            for (int col = columnLetterToIndex("D"); col < columnLetterToIndex("Z"); col++) { // Sök brett efter årtal
                Cell cell = yearRow.getCell(col);
                if (cell == null) continue;

                String yearValue = parseCellValue(cell);

                if (yearValue.equalsIgnoreCase("Totalt")) {
                    totalColIndex = col; // Spara var "Totalt" finns
                    break; // Stoppa loopen här
                }

                if (yearValue.matches("\\d{4}")) { // Om det är ett textbaserat årtal
                    years.add(yearValue);
                } else {
                    try {
                        int numericYear = (int) cell.getNumericCellValue(); // Om det är numeriskt
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


    private int findColumnIndex(Sheet sheet, String columnName) {
        Row headerRow = sheet.getRow(12); // Rad 13 i Excel (0-index)
        if (headerRow != null) {
            for (int col = columnLetterToIndex("D"); col < columnLetterToIndex("Z"); col++) {
                Cell cell = headerRow.getCell(col);
                if (cell != null && parseCellValue(cell).equalsIgnoreCase(columnName)) {
                    return col;
                }
            }
        }
        return -1; // Returnera -1 om kolumnen inte hittades
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
}