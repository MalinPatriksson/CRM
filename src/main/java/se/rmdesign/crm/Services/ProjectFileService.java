package se.rmdesign.crm.Services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.rmdesign.crm.Models.ProjectFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProjectFileService {
    private final Map<String, ProjectFile> tempFiles = new HashMap<>(); // Temporär lagring av PDF-filer

    // Spara fil temporärt innan projektet skapas
    public void storeTempFile(String sessionId, MultipartFile file) throws IOException {
        ProjectFile projectFile = new ProjectFile();
        projectFile.setFileName(file.getOriginalFilename());
        projectFile.setFileSize(file.getSize());
        projectFile.setFileData(file.getBytes());
        projectFile.setUploadTime(LocalDateTime.now());

        tempFiles.put(sessionId, projectFile);
    }

    // Hämta temporär fil
    public Optional<ProjectFile> getTempFile(String sessionId) {
        return Optional.ofNullable(tempFiles.get(sessionId));
    }

    // Rensa fil efter att projektet sparats
    public void clearTempFile(String sessionId) {
        tempFiles.remove(sessionId);
    }

}
