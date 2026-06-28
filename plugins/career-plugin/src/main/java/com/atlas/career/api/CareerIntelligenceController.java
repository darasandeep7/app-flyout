package com.atlas.career.api;

import com.atlas.briefing.DailyBriefing;
import com.atlas.briefing.DailyBriefingService;
import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.service.CareerWorkflow;
import com.atlas.company.CompanyIntelligenceProfile;
import com.atlas.company.CompanyIntelligenceService;
import com.atlas.jobranking.DuplicateAssessment;
import com.atlas.jobranking.JobIntelligenceRequest;
import com.atlas.jobranking.JobIntelligenceScore;
import com.atlas.jobranking.JobRankingService;
import com.atlas.learning.CareerLearningService;
import com.atlas.learning.CareerLearningStats;
import com.atlas.recommendation.JobRecommendation;
import com.atlas.recommendation.RecommendationService;
import com.atlas.resume.ResumeHealth;
import com.atlas.resume.ResumeIntelligenceService;
import com.atlas.resume.ResumeProfile;
import com.atlas.visa.VisaAnalysisRequest;
import com.atlas.visa.VisaAnalysisResult;
import com.atlas.visa.VisaIntelligenceAnalyzer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugins/career/intelligence")
public class CareerIntelligenceController {
    private final CareerWorkflow workflow;
    private final VisaIntelligenceAnalyzer visaIntelligence;
    private final JobRankingService jobRanking;
    private final RecommendationService recommendationService;
    private final ResumeIntelligenceService resumeIntelligence;
    private final DailyBriefingService dailyBriefing;
    private final CareerLearningService learning;
    private final CompanyIntelligenceService companyIntelligence;

    public CareerIntelligenceController(
            CareerWorkflow workflow,
            VisaIntelligenceAnalyzer visaIntelligence,
            JobRankingService jobRanking,
            RecommendationService recommendationService,
            ResumeIntelligenceService resumeIntelligence,
            DailyBriefingService dailyBriefing,
            CareerLearningService learning,
            CompanyIntelligenceService companyIntelligence
    ) {
        this.workflow = workflow;
        this.visaIntelligence = visaIntelligence;
        this.jobRanking = jobRanking;
        this.recommendationService = recommendationService;
        this.resumeIntelligence = resumeIntelligence;
        this.dailyBriefing = dailyBriefing;
        this.learning = learning;
        this.companyIntelligence = companyIntelligence;
    }

    @GetMapping("/companies")
    public List<CompanyIntelligenceProfile> companies() {
        return workflow.companies().stream().map(this::profile).toList();
    }

    @PostMapping("/visa")
    public VisaAnalysisResult visa(@RequestBody VisaAnalysisRequest request) {
        return visaIntelligence.analyze(request);
    }

    @PostMapping("/rank")
    public JobIntelligenceScore rank(@RequestBody JobIntelligenceRequest request) {
        VisaAnalysisResult visa = request.visa() == null
                ? visaIntelligence.analyze(new VisaAnalysisRequest(request.company(), request.title(), request.description(), 0, List.of()))
                : request.visa();
        JobIntelligenceRequest enriched = new JobIntelligenceRequest(
                request.company(),
                request.title(),
                request.location(),
                request.salary(),
                request.remoteStatus(),
                request.postingDate() == null ? Instant.now() : request.postingDate(),
                request.description(),
                request.requiredSkills(),
                request.experienceLevel(),
                visa
        );
        return jobRanking.score(enriched);
    }

    @PostMapping("/duplicate")
    public DuplicateAssessment duplicate(@RequestBody DuplicateRequest request) {
        return jobRanking.duplicate(request.left(), request.right());
    }

    @GetMapping("/recommendations")
    public List<RecommendationView> recommendations() {
        return workflow.jobs().stream()
                .sorted(Comparator.comparing((JobRecord job) -> job.intelligence() == null ? 0 : job.intelligence().ranking().overallMatch()).reversed())
                .map(job -> new RecommendationView(job.id(), job.company(), job.title(), job.location(),
                        job.intelligence() == null ? null : job.intelligence().recommendation(),
                        job.intelligence() == null ? null : job.intelligence().ranking()))
                .toList();
    }

    @PostMapping("/recommend")
    public JobRecommendation recommend(@RequestBody JobIntelligenceRequest request) {
        VisaAnalysisResult visa = visaIntelligence.analyze(new VisaAnalysisRequest(request.company(), request.title(), request.description(), 0, List.of()));
        JobIntelligenceScore score = jobRanking.score(new JobIntelligenceRequest(request.company(), request.title(), request.location(), request.salary(), request.remoteStatus(), request.postingDate(), request.description(), request.requiredSkills(), request.experienceLevel(), visa));
        return recommendationService.recommend(score, visa, false, false);
    }

    @PostMapping("/resume-health")
    public ResumeHealth resumeHealth(@RequestBody ResumeHealthRequest request) {
        return resumeIntelligence.health(request.profile(), request.targetJobDescription());
    }

    @GetMapping("/learning")
    public CareerLearningStats learning() {
        return learning.emptyStats();
    }

    @GetMapping("/daily-briefing")
    public DailyBriefing dailyBriefing() {
        CareerDashboard dashboard = workflow.dashboard();
        return dailyBriefing.briefing(
                dashboard.newJobs(),
                dashboard.excellentMatches(),
                (int) workflow.jobs().stream().filter(job -> job.intelligence() != null && job.intelligence().visa().visaEligible()).count(),
                dashboard.applicationsReady(),
                dashboard.topMatches().stream().map(job -> job.company() + " - " + job.title()).toList(),
                dashboard.topCompanies().stream().map(CompanyRecord::name).toList()
        );
    }

    private CompanyIntelligenceProfile profile(CompanyRecord company) {
        return companyIntelligence.updateAfterScan(
                companyIntelligence.profile(company.name(), company.careerUrl(), company.locations(), company.notes(), company.priority()),
                0,
                company.averageMatchScore(),
                company.technologyStack()
        );
    }

    public record DuplicateRequest(JobIntelligenceRequest left, JobIntelligenceRequest right) {
    }

    public record ResumeHealthRequest(ResumeProfile profile, String targetJobDescription) {
    }

    public record RecommendationView(String jobId, String company, String title, String location, JobRecommendation recommendation, JobIntelligenceScore ranking) {
    }
}
