package se.rmdesign.crm.Models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "project_status")
public class ProjectStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    @Column(name = "status_date")
    private LocalDate statusDate;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private int weighting = 0;

    public ProjectStatus() {
    }

    public ProjectStatus(String status, LocalDate statusDate, Project project) {
        this.status = status;
        this.statusDate = statusDate;
        this.project = project;
    }

    public Long getId() { return id; }
    public String getStatus() { return status; }

    public LocalDate getStatusDate() { return statusDate; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusDate(LocalDate statusDate) {
        this.statusDate = statusDate;
    }

    public int getWeighting() {
        return weighting;
    }

    public void setWeighting(int weighting) {
        this.weighting = weighting;
    }
}
