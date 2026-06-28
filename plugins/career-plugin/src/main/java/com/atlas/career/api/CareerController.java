package com.atlas.career.api;

import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.ApplicationExecutionResult;
import com.atlas.career.domain.ApplicationHistoryRecord;
import com.atlas.career.domain.JobDiscoveryResult;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MasterResume;
import com.atlas.career.service.CareerWorkflow;
import com.atlas.resume.ResumeHealth;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugins/career")
public class CareerController {
    private final CareerWorkflow workflow;

    public CareerController(CareerWorkflow workflow) {
        this.workflow = workflow;
    }

    @GetMapping("/dashboard")
    public CareerDashboard dashboard() {
        return workflow.dashboard();
    }

    @GetMapping("/companies")
    public List<CompanyRecord> companies() {
        return workflow.companies();
    }

    @PostMapping("/companies")
    public CompanyRecord addCompany(@RequestBody AddCompanyRequest request) {
        return workflow.addCompany(request);
    }

    @PostMapping("/companies/import")
    public JobDiscoveryResult importCompanies(@RequestBody ImportCompaniesRequest request) {
        return workflow.importCompanies(request.text());
    }

    @PostMapping("/jobs/scan")
    public JobDiscoveryResult scanJobs() {
        return workflow.scanCompanies();
    }

    @GetMapping("/jobs")
    public List<JobRecord> jobs() {
        return workflow.jobs();
    }

    @PostMapping("/jobs/analyze")
    public JobRecord analyzeJob(@RequestBody AnalyzeJobRequest request) {
        return workflow.analyzeJob(request);
    }

    @GetMapping("/applications")
    public List<ApplicationPackage> applications() {
        return workflow.applications();
    }

    @GetMapping("/applications/history")
    public List<ApplicationHistoryRecord> applicationHistory() {
        return workflow.applicationHistory();
    }

    @GetMapping("/preferences")
    public CareerPreferences preferences() {
        return workflow.preferences();
    }

    @PostMapping("/preferences")
    public CareerPreferences savePreferences(@RequestBody CareerPreferences preferences) {
        return workflow.savePreferences(preferences);
    }

    @GetMapping("/resume/master")
    public MasterResume masterResume() {
        return workflow.masterResume();
    }

    @PostMapping("/resume/master")
    public MasterResume saveMasterResume(@RequestBody MasterResume masterResume) {
        return workflow.saveMasterResume(masterResume);
    }

    @GetMapping("/resume/health")
    public ResumeHealth resumeHealth() {
        return workflow.resumeHealth();
    }

    @PostMapping("/daily/run")
    public List<ApplicationPackage> runDailyPreparation() {
        return workflow.runDailyPreparation();
    }

    @PostMapping("/applications/{applicationId}/approve")
    public ApplicationPackage approveApplication(@org.springframework.web.bind.annotation.PathVariable String applicationId) {
        return workflow.approveApplication(applicationId);
    }

    @PostMapping("/applications/{applicationId}/execute")
    public ApplicationExecutionResult executeApplication(@org.springframework.web.bind.annotation.PathVariable String applicationId) {
        return workflow.executeApplication(applicationId);
    }

    @PostMapping("/applications/{applicationId}/mark")
    public ApplicationHistoryRecord markApplication(@org.springframework.web.bind.annotation.PathVariable String applicationId, @RequestBody MarkApplicationRequest request) {
        return workflow.markApplication(applicationId, request.status(), request.note());
    }

    public record ImportCompaniesRequest(String text) {
    }

    public record MarkApplicationRequest(String status, String note) {
    }
}
