package se.rmdesign.crm.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectFile;
import se.rmdesign.crm.Services.ProjectFileService;
import se.rmdesign.crm.Services.ProjectService;


@RestController
@RequestMapping("/files")
public class ProjectFileController {
    private final ProjectFileService projectFileService;
    private final ProjectService projectService;

    public ProjectFileController(ProjectFileService projectFileService, ProjectService projectService) {
        this.projectFileService = projectFileService;
        this.projectService = projectService;
    }

}