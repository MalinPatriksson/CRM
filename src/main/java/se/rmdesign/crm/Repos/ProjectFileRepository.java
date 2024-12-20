package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import se.rmdesign.crm.Models.ProjectFile;

import java.util.List;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Integer> {
    // Hämta alla filer för ett visst projekt
    List<ProjectFile> findByProjectId(int project_id);
}

