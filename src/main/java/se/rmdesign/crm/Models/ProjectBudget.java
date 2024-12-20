package se.rmdesign.crm.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "project_budget")
public class ProjectBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unikt ID för varje budgetpost

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project; // Koppling till projektet som budgeten tillhör

    @Column(nullable = false)
    private Integer year; // Året som budgeten gäller för

    @Column(nullable = false)
    private BigDecimal lönekostnader; // Budget för lönekostnader

    @Column(nullable = false)
    private BigDecimal resorKonferenser; // Budget för resor och konferenser

    @Column(nullable = false)
    private BigDecimal lokalkostnader; // Budget för lokalkostnader

    @Column(nullable = false)
    private BigDecimal externtKöptTjänst; // Budget för externt köpta tjänster

    @Column(nullable = false)
    private BigDecimal investeringar; // Budget för investeringar

    @Column(nullable = false)
    private BigDecimal totalaKostnader; // Totala kostnader för året

    // Formaterade fält för visning i frontend
    @Transient
    private String formattedLönekostnader;

    @Transient
    private String formattedResorKonferenser;

    @Transient
    private String formattedLokalkostnader;

    @Transient
    private String formattedExterntKöptTjänst;

    @Transient
    private String formattedInvesteringar;

    @Transient
    private String formattedTotalaKostnader;

    // Getters och Setters för originalvärden
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getLönekostnader() {
        return lönekostnader;
    }

    public void setLönekostnader(BigDecimal lönekostnader) {
        this.lönekostnader = lönekostnader;
    }

    public BigDecimal getResorKonferenser() {
        return resorKonferenser;
    }

    public void setResorKonferenser(BigDecimal resorKonferenser) {
        this.resorKonferenser = resorKonferenser;
    }

    public BigDecimal getLokalkostnader() {
        return lokalkostnader;
    }

    public void setLokalkostnader(BigDecimal lokalkostnader) {
        this.lokalkostnader = lokalkostnader;
    }

    public BigDecimal getExterntKöptTjänst() {
        return externtKöptTjänst;
    }

    public void setExterntKöptTjänst(BigDecimal externtKöptTjänst) {
        this.externtKöptTjänst = externtKöptTjänst;
    }

    public BigDecimal getInvesteringar() {
        return investeringar;
    }

    public void setInvesteringar(BigDecimal investeringar) {
        this.investeringar = investeringar;
    }

    public BigDecimal getTotalaKostnader() {
        return totalaKostnader;
    }

    public void setTotalaKostnader(BigDecimal totalaKostnader) {
        this.totalaKostnader = totalaKostnader;
    }

    // Getters och Setters för formaterade värden
    public String getFormattedLönekostnader() {
        return formattedLönekostnader;
    }

    public void setFormattedLönekostnader(String formattedLönekostnader) {
        this.formattedLönekostnader = formattedLönekostnader;
    }

    public String getFormattedResorKonferenser() {
        return formattedResorKonferenser;
    }

    public void setFormattedResorKonferenser(String formattedResorKonferenser) {
        this.formattedResorKonferenser = formattedResorKonferenser;
    }

    public String getFormattedLokalkostnader() {
        return formattedLokalkostnader;
    }

    public void setFormattedLokalkostnader(String formattedLokalkostnader) {
        this.formattedLokalkostnader = formattedLokalkostnader;
    }

    public String getFormattedExterntKöptTjänst() {
        return formattedExterntKöptTjänst;
    }

    public void setFormattedExterntKöptTjänst(String formattedExterntKöptTjänst) {
        this.formattedExterntKöptTjänst = formattedExterntKöptTjänst;
    }

    public String getFormattedInvesteringar() {
        return formattedInvesteringar;
    }

    public void setFormattedInvesteringar(String formattedInvesteringar) {
        this.formattedInvesteringar = formattedInvesteringar;
    }

    public String getFormattedTotalaKostnader() {
        return formattedTotalaKostnader;
    }

    public void setFormattedTotalaKostnader(String formattedTotalaKostnader) {
        this.formattedTotalaKostnader = formattedTotalaKostnader;
    }
}
