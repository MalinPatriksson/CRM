package se.rmdesign.crm.Repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.rmdesign.crm.Models.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(String nameKeyword, String managerKeyword);

    Page<Project> findByNameContainingIgnoreCaseOrManagerContainingIgnoreCase(String nameKeyword, String managerKeyword, Pageable pageable);
}
