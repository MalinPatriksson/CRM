package se.rmdesign.crm.Repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.rmdesign.crm.Models.Project;

import java.util.List;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByCurrentStatus(String currentStatus);
    List<Project> findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(String keyword, String keyword1);
}
