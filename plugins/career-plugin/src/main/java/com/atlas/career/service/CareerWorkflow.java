package com.atlas.career.service;

import com.atlas.career.api.AddCompanyRequest;
import com.atlas.career.api.AnalyzeJobRequest;
import com.atlas.career.api.CareerDashboard;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobDiscoveryResult;
import com.atlas.career.domain.JobIntelligence;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MasterResume;
import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.VisaAssessment;
import com.atlas.careerintelligence.CareerIntelligenceEngine;
import com.atlas.company.CompanyIntelligenceService;
import com.atlas.jobranking.DuplicateAssessment;
import com.atlas.jobranking.JobIntelligenceRequest;
import com.atlas.recommendation.RecommendationCategory;
import com.atlas.resume.ResumeHealth;
import com.atlas.resume.ResumeIntelligenceService;
import com.atlas.resume.ResumeProfile;
import com.atlas.visa.VisaAnalysisRequest;
import java.time.Instant;
import java.util.ArrayList;
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
    private final ResumeIntelligenceService resumeIntelligence;
    private final JobDiscoveryService jobDiscovery;

    public CareerWorkflow(CareerRepository repository, VisaIntelligenceService visaIntelligence, MatchEngine matchEngine, CareerIntelligenceEngine intelligenceEngine, CompanyIntelligenceService companyIntelligence, ApplicationPackageService applicationPackageService, ResumeIntelligenceService resumeIntelligence, JobDiscoveryService jobDiscovery) {
        this.repository = repository;
        this.visaIntelligence = visaIntelligence;
        this.matchEngine = matchEngine;
        this.intelligenceEngine = intelligenceEngine;
        this.companyIntelligence = companyIntelligence;
        this.applicationPackageService = applicationPackageService;
        this.resumeIntelligence = resumeIntelligence;
        this.jobDiscovery = jobDiscovery;
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

    public MasterResume masterResume() {
        return repository.masterResume();
    }

    public MasterResume saveMasterResume(MasterResume masterResume) {
        return repository.saveMasterResume(masterResume);
    }

    public ResumeHealth resumeHealth() {
        MasterResume masterResume = repository.masterResume();
        return resumeIntelligence.health(new ResumeProfile(
                masterResume.content(),
                masterResume.preferredSkills(),
                masterResume.preferredKeywords(),
                masterResume.versions()
        ), "");
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

    public JobDiscoveryResult importCompanies(String text) {
        if (text == null || text.isBlank()) {
            return new JobDiscoveryResult(Instant.now(), 0, 0, 0, 0, List.of("No companies provided."));
        }
        List<String> messages = new ArrayList<>();
        int saved = 0;
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String[] parts = trimmed.split(",", 2);
            String name = parts.length == 2 ? parts[0].trim() : domainName(trimmed);
            String url = parts.length == 2 ? parts[1].trim() : trimmed;
            if (!url.startsWith("http")) {
                messages.add("Skipped invalid URL: " + trimmed);
                continue;
            }
            addCompany(new AddCompanyRequest(name, url, List.of("Remote"), 5, "Imported for job discovery."));
            saved++;
        }
        messages.add("Imported " + saved + " companies.");
        return new JobDiscoveryResult(Instant.now(), saved, 0, saved, 0, messages);
    }

    public JobDiscoveryResult scanCompanies() {
        List<CompanyRecord> companies = repository.companies().stream()
                .filter(company -> !company.blocked())
                .filter(company -> company.careerUrl() != null && company.careerUrl().startsWith("http"))
                .filter(company -> !company.id().equals("sample-company"))
                .toList();
        List<String> messages = new ArrayList<>();
        int found = 0;
        int saved = 0;
        int expired = 0;
        for (CompanyRecord company : companies) {
            JobDiscoveryService.ScanPage page = jobDiscovery.scan(company);
            found += page.jobs().size();
            List<String> activeUrls = page.jobs().stream().map(JobDiscoveryService.DiscoveredJob::url).toList();
            expired += repository.removeExpiredScannerJobs(company.id(), activeUrls);
            CompanyRecord updatedCompany = updateCompanyAfterScan(company, page.atsPlatform());
            for (JobDiscoveryService.DiscoveredJob discovered : page.jobs()) {
                analyzeJob(new AnalyzeJobRequest(
                        updatedCompany.name(),
                        updatedCompany.id(),
                        discovered.title(),
                        discovered.location(),
                        discovered.url(),
                        discovered.description(),
                        ""
                ));
                saved++;
            }
            messages.add(updatedCompany.name() + ": " + page.jobs().size() + " jobs found via " + page.atsPlatform());
        }
        JobDiscoveryResult result = new JobDiscoveryResult(Instant.now(), companies.size(), found, saved, expired, messages);
        repository.appendLog("job-discovery-last-run.json", result);
        return result;
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
                request.companyId() == null || request.companyId().isBlank()
                        ? List.of("Analyzed locally by Career Copilot.")
                        : List.of("Analyzed locally by Career Copilot.", "Discovered by career page scanner.")
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
        String resume = applicationPackageService.resumeMarkdown(job);
        String coverLetter = applicationPackageService.coverLetterMarkdown(job);
        repository.writeApplicationText(applicationPackage, applicationPackage.resumePath(), resume);
        repository.writeApplicationText(applicationPackage, "resumes/generated/" + applicationPackage.resumeVersion() + ".md", resume);
        repository.writeApplicationText(applicationPackage, applicationPackage.coverLetterPath(), coverLetter);
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

    private CompanyRecord updateCompanyAfterScan(CompanyRecord company, String atsPlatform) {
        return repository.saveCompany(new CompanyRecord(
                company.id(),
                company.name(),
                company.industry(),
                company.website(),
                company.careerUrl(),
                atsPlatform == null || atsPlatform.isBlank() ? company.atsPlatform() : atsPlatform,
                company.remotePolicy(),
                "Hiring page scanned",
                company.visaSponsorshipHistory(),
                company.visaConfidence(),
                company.locations(),
                company.historicalApplications(),
                company.historicalInterviews(),
                company.historicalRejections(),
                company.historicalOffers(),
                company.averageMatchScore(),
                company.priority(),
                company.blocked(),
                Instant.now(),
                Instant.now(),
                Math.max(company.confidenceScore(), 60),
                company.notes(),
                company.technologyStack(),
                append(company.learningHistory(), "Career page scanned by Atlas.")
        ));
    }

    private List<String> append(List<String> values, String value) {
        List<String> next = new ArrayList<>(values == null ? List.of() : values);
        next.add(value);
        return next;
    }

    private String domainName(String url) {
        return url.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0];
    }
}
