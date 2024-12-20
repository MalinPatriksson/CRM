package se.rmdesign.crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import se.rmdesign.crm.Models.Project;
import se.rmdesign.crm.Models.ProjectBudget;
import se.rmdesign.crm.Services.BudgetService;
import se.rmdesign.crm.Services.ProjectService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/projects")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private ProjectService projectService;

    @GetMapping("/{id}/budget")
    public String viewBudget(@PathVariable Integer id, Model model) {
        Project project = projectService.getProjectById(id);

        // Generera alla år mellan startdatum och deadline
        List<Integer> years = budgetService.generateYearsBetweenDates(project.getStartDate(), project.getDeadline());

        // Hämta och formatera budgetposter
        List<ProjectBudget> budgetList = budgetService.getBudgetForProject(id);
        budgetList.forEach(budget -> {
            budget.setFormattedLönekostnader(budgetService.formatCurrency(budget.getLönekostnader()));
            budget.setFormattedResorKonferenser(budgetService.formatCurrency(budget.getResorKonferenser()));
            budget.setFormattedLokalkostnader(budgetService.formatCurrency(budget.getLokalkostnader()));
            budget.setFormattedExterntKöptTjänst(budgetService.formatCurrency(budget.getExterntKöptTjänst()));
            budget.setFormattedInvesteringar(budgetService.formatCurrency(budget.getInvesteringar()));
            budget.setFormattedTotalaKostnader(budgetService.formatCurrency(budget.getTotalaKostnader()));
        });

        model.addAttribute("project", project);
        model.addAttribute("years", years);
        model.addAttribute("budgetList", budgetList);

        return "budget-table";
    }


    @PostMapping("/{id}/budget/add")
    public String addBudget(@PathVariable Integer id,
                            @RequestParam Integer year,
                            @RequestParam BigDecimal lönekostnader,
                            @RequestParam BigDecimal resorKonferenser,
                            @RequestParam BigDecimal lokalkostnader,
                            @RequestParam BigDecimal externtKöptTjänst,
                            @RequestParam BigDecimal investeringar,
                            RedirectAttributes redirectAttributes) {

        Project project = projectService.getProjectById(id);

        ProjectBudget budget = new ProjectBudget();
        budget.setProject(project);
        budget.setYear(year);
        budget.setLönekostnader(lönekostnader);
        budget.setResorKonferenser(resorKonferenser);
        budget.setLokalkostnader(lokalkostnader);
        budget.setExterntKöptTjänst(externtKöptTjänst);
        budget.setInvesteringar(investeringar);
        budget.setTotalaKostnader(
                lönekostnader.add(resorKonferenser)
                        .add(lokalkostnader)
                        .add(externtKöptTjänst)
                        .add(investeringar)
        );

        budgetService.saveBudget(budget);

        redirectAttributes.addFlashAttribute("message", "Budgetpost har lagts till/uppdaterats!");
        return "redirect:/projects/" + id + "/budget";
    }
}
