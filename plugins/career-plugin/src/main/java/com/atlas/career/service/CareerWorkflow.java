package com.atlas.career.service;

import com.atlas.career.api.AddCompanyRequest;
import com.atlas.career.api.AnalyzeJobRequest;
import com.atlas.career.api.CareerDashboard;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobIntelligence;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.VisaAssessment;
import com.atlas.careerintelligence.CareerIntelligenceEngine;
import com.atlas.company.CompanyIntelligenceService;
import com.atlas.jobranking.DuplicateAssessment;
import com.atlas.jobranking.JobIntelligenceRequest;
import com.atlas.recommendation.RecommendationCategory;
import com.atlas.visa.VisaAnalysisRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CareerWorkflow {
    private final CareerRepository repository;
    private final VisaIntelligenceService visaIntelligence;
    private final MatchEngine matchEngine;
    private final CareerIntelligenceEngine intelligenceEngine;
    private final CompanyIntelligenceService companyIntelligence;
    private final ApplicationPackageService applicationPackageService;

    public CareerWorkflow(CareerRepository repository, VisaIntelligenceService visaIntelligence, MatchEngine matchEngine, CareerIntelligenceEngine intelligenceEngine, CompanyIntelligenceService companyIntelligence, ApplicationPackageService applicationPackageService) {
        this.repository = repository;
        this.visaIntelligence = visaIntelligence;
        this.matchEngine = matchEngine;
        this.intelligenceEngine = intelligenceEngine;
        this.companyIntelligence = companyIntelligence;
        this.applicationPackageService = applicationPackageService;
    }

    public CareerDashboard dashboard() {
        List<CompanyRecord> companies = repository.companies();
        List<JobRecord> jobs = repository.jobs();
        List<JobRecord> topMatches = jobs.stream()
                .sorted(Comparator.comparing((JobRecord job) -> job.match().overallMatch()).reversed())
                .limit(10)
                .toList();
        List<CompanyRecord> topCompanies = companies.stream()
                .filter(company -> !company.blocked())
                .sorted(Comparator.comparing(CompanyRecord::priority).reversed())
                .limit(10)
                .toList();
        return new CareerDashboard(
                companies.size(),
                jobs.size(),
                (int) jobs.stream().filter(job -> job.match().overallMatch() >= 80).count(),
                (int) jobs.stream().filter(job -> job.resumeReady() && job.coverLetterReady()).count(),
                (int) jobs.stream().filter(job -> job.visa().score() < 40).count(),
                topMatches,
                topCompanies
        );
    }

    public List<CompanyRecord> companies() {
        return repository.companies();
    }

    public List<JobRecord> jobs() {
        return repository.jobs();
    }

    public List<ApplicationPackage> applications() {
        return repository.applications();
    }

    public CareerPreferences preferences() {
        return repository.preferences();
    }

    public CareerPreferences savePreferences(CareerPreferences preferences) {
        return repository.savePreferences(preferences);
    }

    public CompanyRecord addCompany(AddCompanyRequest request) {
        String id = repository.companyId(request.name());
        var profile = companyIntelligence.profile(request.name(), request.careerUrl(), request.locations(), request.notes(), request.priority());
        CompanyRecord company = new CompanyRecord(
                id,
                request.name(),
                "Unknown",
                profile.website(),
                request.careerUrl(),
                profile.knownAtsPlatform(),
                profile.remotePolicy(),
                "Unknown",
                profile.historicalSponsorship(),
                profile.visaSponsorshipConfidence(),
                profile.locations(),
                profile.historicalApplications(),
                profile.historicalInterviews(),
                profile.historicalRejections(),
                profile.historicalOffers(),
                profile.averageMatchScore(),
                request.priority(),
                false,
                Instant.EPOCH,
                profile.lastUpdated(),
                profile.confidenceScore(),
                request.notes(),
                profile.technologyStack(),
                List.of("Company added manually.")
        );
        return repository.saveCompany(company);
    }

    public JobRecord analyzeJob(AnalyzeJobRequest request) {
        CompanyRecord company = repository.findCompany(request.companyId() == null || request.companyId().isBlank() ? request.company() : request.companyId()).orElse(null);
        VisaAssessment visa = visaIntelligence.assess(request.description(), company);
        MatchAssessment match = matchEngine.score(request.title(), request.location(), request.description(), request.salary(), visa);
        var intelligence = intelligenceEngine.evaluate(
                new VisaAnalysisRequest(request.company(), request.title(), request.description(), company == null ? 0 : company.visaConfidence(), company == null ? List.of() : company.learningHistory()),
                new JobIntelligenceRequest(request.company(), request.title(), request.location(), request.salary(), remoteStatus(request.location()), Instant.now(), request.description(), List.of(), experienceLevel(request.title(), request.description()), null),
                company != null && company.blocked(),
                alreadyApplied(request.company(), request.title(), request.location())
        );
        DuplicateAssessment duplicate = duplicateAssessment(request);
        JobIntelligence jobIntelligence = new JobIntelligence(intelligence.visa(), intelligence.ranking(), intelligence.recommendation(), duplicate);
        String companyId = company == null ? repository.companyId(request.company()) : company.id();
        JobRecord job = new JobRecord(
                repository.jobId(request.company(), request.title(), request.location()),
                companyId,
                request.company(),
                request.title(),
                request.location(),
                request.url(),
                request.description(),
                visa,
                match,
                jobIntelligence,
                applicationStatus(jobIntelligence),
                jobIntelligence.ranking().overallMatch() >= 75 && jobIntelligence.visa().visaScore() >= 50,
                jobIntelligence.ranking().overallMatch() >= 70 && jobIntelligence.visa().visaScore() >= 50,
                Instant.now(),
                Instant.now(),
                List.of("Analyzed locally by Career Copilot.")
        );
        return repository.saveJob(job);
    }

    public List<ApplicationPackage> runDailyPreparation() {
        CareerPreferences preferences = repository.preferences();
        return repository.jobs().stream()
                .filter(applicationPackageService::shouldPrepare)
                .filter(job -> !containsIgnoreCase(preferences.blacklistCompanies(), job.company()))
                .filter(job -> job.match().overallMatch() >= preferences.minimumMatchScore())
                .filter(job -> !preferences.visaRequired() || job.visa().score() >= 50)
                .limit(preferences.maximumApplicationsPerDay())
                .map(this::prepareApplication)
                .toList();
    }

    public ApplicationPackage prepareApplication(JobRecord job) {
        ApplicationPackage applicationPackage = applicationPackageService.create(job);
        repository.writeApplicationText(applicationPackage, applicationPackage.resumePath(), applicationPackageService.resumeMarkdown(job));
        repository.writeApplicationText(applicationPackage, applicationPackage.coverLetterPath(), applicationPackageService.coverLetterMarkdown(job));
        repository.writeApplicationArtifact(applicationPackage, applicationPackage.answersPath(), applicationPackage.answers());
        repository.writeApplicationText(applicationPackage, applicationPackage.reportPath(), applicationPackageService.reportMarkdown(job, applicationPackage));
        return repository.saveApplication(applicationPackage);
    }

    public ApplicationPackage approveApplication(String applicationId) {
        return repository.applications().stream()
                .filter(applicationPackage -> applicationPackage.id().equals(applicationId))
                .findFirst()
                .map(applicationPackage -> repository.saveApplication(new ApplicationPackage(
                        applicationPackage.id(),
                        applicationPackage.jobId(),
                        applicationPackage.company(),
                        applicationPackage.title(),
                        "APPROVED_FOR_BROWSER_AGENT",
                        applicationPackage.recommendationConfidence(),
                        applicationPackage.recommendation(),
                        applicationPackage.resumeVersion(),
                        applicationPackage.resumePath(),
                        applicationPackage.coverLetterPath(),
                        applicationPackage.answersPath(),
                        applicationPackage.reportPath(),
                        applicationPackage.answers(),
                        applicationPackage.createdAt(),
                        Instant.now()
                )))
                .orElseThrow(() -> new IllegalArgumentException("Application package not found: " + applicationId));
    }

    private String applicationStatus(JobIntelligence intelligence) {
        if (intelligence.recommendation().category() == RecommendationCategory.VISA_RISK) {
            return "Skipped - visa risk";
        }
        if (intelligence.recommendation().category() == RecommendationCategory.APPLY_TODAY) {
            return "Ready to apply";
        }
        return "Discovered";
    }

    private boolean alreadyApplied(String company, String title, String location) {
        return repository.jobs().stream().anyMatch(job ->
                job.company().equalsIgnoreCase(company)
                        && job.title().equalsIgnoreCase(title)
                        && job.location().equalsIgnoreCase(location)
                        && job.applicationStatus().toLowerCase().contains("applied"));
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        if (values == null || candidate == null) {
            return false;
        }
        return values.stream().anyMatch(value -> candidate.equalsIgnoreCase(value));
    }

    private DuplicateAssessment duplicateAssessment(AnalyzeJobRequest request) {
        boolean duplicate = repository.jobs().stream().anyMatch(job ->
                (request.url() != null && !request.url().isBlank() && request.url().equalsIgnoreCase(job.url()))
                        || (job.company().equalsIgnoreCase(request.company())
                        && job.title().equalsIgnoreCase(request.title())
                        && job.location().equalsIgnoreCase(request.location())));
        return new DuplicateAssessment(duplicate, duplicate ? 85 : 0, duplicate ? List.of("company", "title", "location or url") : List.of());
    }

    private String remoteStatus(String location) {
        return location != null && location.toLowerCase().contains("remote") ? "Remote" : "Unknown";
    }

    private String experienceLevel(String title, String description) {
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase();
        if (text.contains("principal") || text.contains("staff") || text.contains("architect")) {
            return "Staff+";
        }
        if (text.contains("senior") || text.contains("lead")) {
            return "Senior";
        }
        return "Unknown";
    }
}
