package se.rmdesign.crm.Models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ProjectFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private Long fileSize;

    @Lob // Anger att fältet är ett stort objekt
    private byte[] fileData;

    private LocalDateTime uploadTime;

    @ManyToOne // Koppla filen till ett projekt
    private Project project;

    // Getters och Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
}
