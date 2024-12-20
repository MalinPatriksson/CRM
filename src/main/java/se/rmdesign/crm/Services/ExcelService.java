package se.rmdesign.crm.Services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

    private Map<String, String> processPdfFile(MultipartFile file) throws Exception {
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

                // Om raden innehåller rubriken, hämta värdet efter rubriken
                if (line.toLowerCase().contains("projektnamn")) {
                    String value = getValueAfterKeyword(line, "Projektnamn");
                    System.out.println("Projektnamn hittat: " + value);
                    extractedData.put("Projektnamn", value);
                } else if (line.toLowerCase().contains("projektledares för- och efternamn")) {
                    String value = getValueAfterKeyword(line, "Projektledares för- och efternamn");
                    System.out.println("Projektledares för- och efternamn hittat: " + value);
                    extractedData.put("Projektledares för- och efternamn", value);
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
