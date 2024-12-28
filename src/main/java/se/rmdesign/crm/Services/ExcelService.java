package se.rmdesign.crm.Services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelService {

    public Map<String, String> processFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filnamn saknas!");
        }

        if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xlsm")) {
            return processExcelFile(file);
        } else if (filename.toLowerCase().endsWith(".pdf")) {
            return processPdfFile(file);
        } else {
            throw new IllegalArgumentException("Endast filer av typen Excel (.xlsx, .xlsm) och PDF (.pdf) stöds.");
        }
    }

    private Map<String, String> processExcelFile(MultipartFile file) throws Exception {
        Map<String, String> extractedData = new HashMap<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue().trim();

                        // Kontrollera efter "Projektnamn"
                        if ("Projektnamn".equalsIgnoreCase(cellValue)) {
                            extractedData.put("Projektnamn", getNextNonEmptyCellValue(row, cell.getColumnIndex()));
                        } else if ("Projektledares för- och efternamn".equalsIgnoreCase(cellValue)) {
                            extractedData.put("Projektledares för- och efternamn", getNextNonEmptyCellValue(row, cell.getColumnIndex()));
                        }
                    }
                }
            }
        }

        return extractedData;
    }

    private static final Map<String, String> FINANCIER_MAPPING = new HashMap<>() {{
        put("VINNO", "Vinnova");
        put("Jordbruksverket", "Jordbruksverket");
        put("KK", "KK-Stiftelsen");
        put("Energimyndigheten", "Energimyndigheten");
        put("VETEN", "Vetenskapsrådet");
    }};

    public Map<String, String> processPdfFile(MultipartFile file) throws Exception {
        Map<String, String> extractedData = new HashMap<>();

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String pdfText = pdfStripper.getText(document);

            // Dela upp texten i rader
            String[] lines = pdfText.split("\n");

            System.out.println("=== Debug: Extraherade rader från PDF ===");
            for (String line : lines) {
                System.out.println(line.trim()); // Skriv ut varje rad för debugging
            }

            for (String line : lines) {
                line = line.trim();

                // Kontrollera efter "Projektnamn"
                if (line.toLowerCase().contains("projektnamn")) {
                    String value = getValueAfterKeyword(line, "Projektnamn");
                    System.out.println("Projektnamn hittat: " + value);
                    extractedData.put("Projektnamn", value);

                    // Kontrollera efter "Projektledares för- och efternamn"
                } else if (line.toLowerCase().contains("projektledares för- och efternamn")) {
                    String value = getValueAfterKeyword(line, "Projektledares för- och efternamn");
                    System.out.println("Projektledares för- och efternamn hittat: " + value);
                    extractedData.put("Projektledares för- och efternamn", value);

                    // Kontrollera efter "Finansiär"
                } else if (line.toLowerCase().contains("finansiär")) {
                    // Skanna raden och leta efter match i mappningen
                    for (Map.Entry<String, String> entry : FINANCIER_MAPPING.entrySet()) {
                        if (line.contains(entry.getKey())) {
                            String validFinancier = entry.getValue(); // Mappa till korrekt namn
                            System.out.println("Finansiär hittad och mappad: " + validFinancier);
                            extractedData.put("Finansiär", validFinancier);
                            break; // Sluta leta när vi hittat en match
                        }
                    }

                    // Om ingen match hittas, logga och lämna värdet tomt
                    if (!extractedData.containsKey("Finansiär")) {
                        System.out.println("Ingen giltig finansiär hittad på raden: " + line);
                        extractedData.put("Finansiär", "");
                    }

                    // Kontrollera efter "Forskningsprogram"
                } else if (line.toLowerCase().contains("forskningsprogram:")) {
                    String value = getValueAfterKeyword(line, "Forskningsprogram:");
                    System.out.println("Forskningsprogram: " + value);
                    extractedData.put("Forskningsprogram", value);

                    // Kontrollera efter "Total löptid"
                } else if (line.toLowerCase().contains("total löptid")) {
                    String value = getValueAfterKeyword(line, "Total löptid");
                    System.out.println("Total löptid hittat: " + value);

                    // Extrahera endast datumen
                    Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                    Matcher matcher = datePattern.matcher(value);

                    List<String> dates = new ArrayList<>();
                    while (matcher.find()) {
                        dates.add(matcher.group());
                    }

                    if (dates.size() >= 2) {
                        String startDate = dates.get(0); // Första datumet
                        String endDate = dates.get(1);   // Andra datumet

                        System.out.println("Startdatum: " + startDate);
                        System.out.println("Deadline: " + endDate);

                        extractedData.put("Startdatum", startDate);
                        extractedData.put("Deadline", endDate);
                    } else {
                        System.out.println("Felaktigt format för Total löptid: " + value);
                    }
                }
            }
        }

        System.out.println("=== Debug: Extraherade data ===");
        extractedData.forEach((key, value) -> System.out.println(key + ": " + value));

        return extractedData;
    }


    private String getNextNonEmptyCellValue(Row row, int columnIndex) {
        for (int i = columnIndex + 1; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isEmpty()) {
                return cell.getStringCellValue().trim();
            }
        }
        return ""; // Returnera tomt om inget hittas
    }

    private String getValueAfterKeyword(String line, String keyword) {
        int keywordIndex = line.toLowerCase().indexOf(keyword.toLowerCase());
        if (keywordIndex != -1) {
            // Hämta texten efter nyckelordet
            return line.substring(keywordIndex + keyword.length()).trim();
        }
        return ""; // Returnera tomt om nyckelordet inte hittas
    }

}
