package com.atlas.career.api;

import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.service.CareerWorkflow;
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

    @GetMapping("/preferences")
    public CareerPreferences preferences() {
        return workflow.preferences();
    }

    @PostMapping("/preferences")
    public CareerPreferences savePreferences(@RequestBody CareerPreferences preferences) {
        return workflow.savePreferences(preferences);
    }

    @PostMapping("/daily/run")
    public List<ApplicationPackage> runDailyPreparation() {
        return workflow.runDailyPreparation();
    }

    @PostMapping("/applications/{applicationId}/approve")
    public ApplicationPackage approveApplication(@org.springframework.web.bind.annotation.PathVariable String applicationId) {
        return workflow.approveApplication(applicationId);
    }
}
