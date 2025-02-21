package se.rmdesign.crm.Services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelService {

    public Map<String, Object> processPdfFile(MultipartFile file) throws Exception {
        Map<String, Object> extractedData = new HashMap<>();
        List<Map<String, String>> budgetRows = new ArrayList<>();
        List<String> years = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String pdfText = pdfStripper.getText(document);

            System.out.println("📄 Fullständig PDF-text:\n" + pdfText);

            String[] lines = pdfText.split("\n");
            boolean isBudgetSection = false;

            for (String line : lines) {
                line = line.trim();
                System.out.println("Reading line: " + line);

                if (line.toLowerCase().contains("projektnamn")) {
                    String value = getValueAfterKeyword(line, "Projektnamn");
                    extractedData.put("Projektnamn", value);
                    System.out.println("✅ Sparar Projektnamn: " + value);
                }
                    else if (line.toLowerCase().contains("diarienummer")) {
                        String value = getValueAfterKeyword(line, "Diarienummer");
                        extractedData.put("Diarienummer", value);
                        System.out.println("✅ Sparar Diarienummer: " + value);

                } else if (line.toLowerCase().contains("projektledares för- och efternamn")) {
                    String value = getValueAfterKeyword(line, "Projektledares för- och efternamn");
                    extractedData.put("Projektledares för- och efternamn", value);
                    System.out.println("✅ Sparar Projektledare: " + value);
                } else if (line.toLowerCase().contains("finansiär")) {
                    String value = getSingleWordAfterKeyword(line, "Finansiär");
                    extractedData.put("Finansiär", value);
                    System.out.println("✅ Sparar Finansiär: " + value);
                } else if (line.toLowerCase().contains("forskningsprogram")) {
                    String value = getValueAfterKeyword(line, "Forskningsprogram");
                    extractedData.put("Forskningsprogram", value);
                    System.out.println("✅ Sparar Forskningsprogram: " + value);
                } else if (line.toLowerCase().contains("total löptid")) {
                    Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                    Matcher matcher = datePattern.matcher(line);
                    List<String> dates = new ArrayList<>();
                    while (matcher.find()) {
                        dates.add(matcher.group());
                    }
                    if (dates.size() >= 2) {
                        extractedData.put("Startdatum", dates.get(0));
                        extractedData.put("Deadline", dates.get(1));
                        System.out.println("✅ Sparar Startdatum: " + dates.get(0) + ", Deadline: " + dates.get(1));
                    }
                }

                if (line.toLowerCase().contains("intäkter")) {
                    isBudgetSection = true;
                    years.clear();
                    for (String word : line.split("\\s+")) {
                        if (word.matches("\\d{4}")) {
                            years.add(word);
                        }
                    }
                    extractedData.put("Years", years);
                    continue;
                }

                if (isBudgetSection) {
                    budgetRows = parseBudgetHeadersOnly(Arrays.asList(lines), years);
                    extractedData.put("BudgetRows", budgetRows);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing PDF: " + e.getMessage());
        }

        // 🔥 Fallback: Om `extractedData` är tom, sätt standardvärden
        if (extractedData.isEmpty()) {
            System.err.println("⚠️ Inga projektdata hittades i PDF!");
            extractedData.put("Projektnamn", "Ej angivet");
            extractedData.put("Projektledares för- och efternamn", "Ej angivet");
            extractedData.put("Startdatum", "1970-01-01");
            extractedData.put("Deadline", "1970-01-01");
            extractedData.put("Finansiär", "Ej angivet");
            extractedData.put("Forskningsprogram", "Ej angivet");
        }

        System.out.println("🔎 FINAL EXTRACTED DATA: " + extractedData);
        return extractedData;
    }


    /**
     * Hämtar endast rubrikerna från budgetsektionen och lämnar siffror tomma.
     * Om rubriken bara är "." ignoreras raden.
     */
    private List<Map<String, String>> parseBudgetHeadersOnly(List<String> lines, List<String> years) {
        List<Map<String, String>> budgetRows = new ArrayList<>();
        boolean isBudgetSection = false;

        for (String line : lines) {
            line = line.trim();
            System.out.println("Reading line: " + line);

            if (line.startsWith("Intäkter")) {
                isBudgetSection = true;
                continue;
            }

            if (isBudgetSection) {
                if (line.toLowerCase().contains("totala intäkter")) {
                    break; // Stoppar vid "Totala intäkter"
                }

                // Ta endast rubriken
                Matcher matcher = Pattern.compile("^(\\D+)\\s+([\\d\\s]*)$").matcher(line);
                if (!matcher.find()) {
                    System.out.println("⚠️ Skipping malformed row: " + line);
                    continue;
                }

                String title = matcher.group(1).trim(); // Rubrik
                if (title.equals(".")) {
                    System.out.println("⚠️ Ignoring row with only '.' as title: " + line);
                    continue; // Hoppa över rader som bara innehåller en punkt
                }

                Map<String, String> row = new LinkedHashMap<>();
                row.put("Rubrik", title);

                for (String year : years) {
                    row.put(year, ""); // Lämnar tomt för manuell inmatning
                }

                row.put("Total", ""); // Lämnar totalen tom för manuell inmatning
                budgetRows.add(row);
                System.out.println("✅ Parsad rad: " + row);
            }
        }

        return budgetRows;
    }

    /**
     * Hämtar endast **ett ord** efter ett givet nyckelord.
     */
    private String getSingleWordAfterKeyword(String line, String keyword) {
        int keywordIndex = line.toLowerCase().indexOf(keyword.toLowerCase());
        if (keywordIndex != -1) {
            String[] parts = line.substring(keywordIndex + keyword.length()).trim().split("\\s+");
            if (parts.length > 0) {
                return parts[0]; // Tar endast **ett ord**
            }
        }
        return "";
    }

    /**
     * Hämtar all text efter ett nyckelord.
     */
    private String getValueAfterKeyword(String line, String keyword) {
        int keywordIndex = line.toLowerCase().indexOf(keyword.toLowerCase());
        if (keywordIndex != -1) {
            return line.substring(keywordIndex + keyword.length()).trim();
        }
        return "";
    }
    }

