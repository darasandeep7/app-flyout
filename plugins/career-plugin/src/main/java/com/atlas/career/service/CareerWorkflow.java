package com.atlas.career.service;

import com.atlas.browser.BrowserApplicationRequest;
import com.atlas.browser.BrowserAutomation;
import com.atlas.career.api.AddCompanyRequest;
import com.atlas.career.api.AnalyzeJobRequest;
import com.atlas.career.api.CareerDashboard;
import com.atlas.career.domain.ApplicationExecutionResult;
import com.atlas.career.domain.ApplicationHistoryRecord;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.AnswerTrainingRule;
import com.atlas.career.domain.CareerLearningInsight;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobDiscoveryResult;
import com.atlas.career.domain.JobIntelligence;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MasterResume;
import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.MemoryRecord;
import com.atlas.career.domain.UserProfile;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
    private final BrowserAutomation browserAutomation;
    private boolean scoresRecalculated;

    public CareerWorkflow(CareerRepository repository, VisaIntelligenceService visaIntelligence, MatchEngine matchEngine, CareerIntelligenceEngine intelligenceEngine, CompanyIntelligenceService companyIntelligence, ApplicationPackageService applicationPackageService, ResumeIntelligenceService resumeIntelligence, JobDiscoveryService jobDiscovery, BrowserAutomation browserAutomation) {
        this.repository = repository;
        this.visaIntelligence = visaIntelligence;
        this.matchEngine = matchEngine;
        this.intelligenceEngine = intelligenceEngine;
        this.companyIntelligence = companyIntelligence;
        this.applicationPackageService = applicationPackageService;
        this.resumeIntelligence = resumeIntelligence;
        this.jobDiscovery = jobDiscovery;
        this.browserAutomation = browserAutomation;
    }

    public CareerDashboard dashboard() {
        repository.importSeedCompanies();
        recalculateStoredJobScores();
        List<CompanyRecord> companies = repository.companies();
        List<JobRecord> jobs = repository.jobs();
        List<JobRecord> topMatches = jobs.stream()
                .filter(this::fieldRelevant)
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
        repository.importSeedCompanies();
        return repository.companies();
    }

    public List<JobRecord> jobs() {
        recalculateStoredJobScores();
        return repository.jobs();
    }

    public List<ApplicationPackage> applications() {
        return repository.applications();
    }

    public List<ApplicationHistoryRecord> applicationHistory() {
        return repository.applicationHistory();
    }

    public List<MemoryRecord> memories() {
        return repository.memories();
    }

    public MemoryRecord saveMemory(MemoryRecord memory) {
        Instant now = Instant.now();
        String id = memory.id() == null || memory.id().isBlank()
                ? com.atlas.common.Slug.of(memory.type() + "-" + memory.scope() + "-" + memory.key() + "-" + now.toEpochMilli())
                : memory.id();
        return repository.saveMemory(new MemoryRecord(
                id,
                blank(memory.type(), "note"),
                blank(memory.scope(), "global"),
                blank(memory.key(), id),
                normalizeIntent(memory.intent() == null || memory.intent().isBlank() ? memory.question() : memory.intent()),
                blank(memory.question(), ""),
                blank(memory.answer(), ""),
                blank(memory.company(), ""),
                blank(memory.ats(), ""),
                memory.data() == null ? Map.of() : memory.data(),
                clamp(memory.confidence() <= 0 ? 70 : memory.confidence(), 1, 100),
                Math.max(0, memory.usageCount()),
                blank(memory.source(), "user"),
                memory.createdAt() == null ? now : memory.createdAt(),
                memory.lastUsed() == null ? now : memory.lastUsed(),
                now
        ));
    }

    public void deleteMemory(String id) {
        repository.deleteMemory(id);
    }

    public List<MemoryRecord> importMemories(List<MemoryRecord> memories) {
        repository.replaceMemories(memories == null ? List.of() : memories);
        return repository.memories();
    }

    public List<AnswerTrainingRule> answerTrainingRules() {
        return repository.answerTrainingRules();
    }

    public AnswerTrainingRule saveAnswerTrainingRule(AnswerTrainingRule request) {
        Instant now = Instant.now();
        String id = request.id() == null || request.id().isBlank()
                ? com.atlas.common.Slug.of(request.questionPattern() + "-" + now.toEpochMilli())
                : request.id();
        AnswerTrainingRule rule = new AnswerTrainingRule(
                id,
                request.questionPattern() == null ? "" : request.questionPattern().trim(),
                request.preferredFormat() == null ? "" : request.preferredFormat().trim(),
                request.exampleAnswer() == null ? "" : request.exampleAnswer().trim(),
                request.enabled(),
                request.createdAt() == null ? now : request.createdAt(),
                now
        );
        return repository.saveAnswerTrainingRule(rule);
    }

    public List<CareerLearningInsight> learningInsights() {
        return repository.applicationHistory().stream()
                .collect(Collectors.groupingBy(ApplicationHistoryRecord::company))
                .entrySet()
                .stream()
                .map(entry -> insight(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CareerLearningInsight::score).reversed())
                .toList();
    }

    public CareerPreferences preferences() {
        return repository.preferences();
    }

    public CareerPreferences savePreferences(CareerPreferences preferences) {
        CareerPreferences saved = repository.savePreferences(preferences);
        scoresRecalculated = false;
        return saved;
    }

    public UserProfile userProfile() {
        return repository.userProfile();
    }

    public UserProfile saveUserProfile(UserProfile profile) {
        UserProfile saved = repository.saveUserProfile(profile);
        learnProfile(saved);
        return saved;
    }

    public MasterResume masterResume() {
        return repository.masterResume();
    }

    public MasterResume saveMasterResume(MasterResume masterResume) {
        MasterResume saved = repository.saveMasterResume(masterResume);
        scoresRecalculated = false;
        return saved;
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
        int seedAdded = repository.importSeedCompanies();
        List<CompanyRecord> companies = repository.companies().stream()
                .filter(company -> !company.blocked())
                .filter(company -> company.careerUrl() != null && company.careerUrl().startsWith("http"))
                .filter(company -> !company.id().equals("sample-company"))
                .toList();
        List<String> messages = new ArrayList<>();
        if (seedAdded > 0) {
            messages.add("Imported " + seedAdded + " seed companies before scanning.");
        }
        messages.add("Scanning " + companies.size() + " configured companies.");
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
                if (!scannerCandidate(discovered)) {
                    continue;
                }
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
            messages.add(updatedCompany.name() + ": " + page.jobs().size() + " high-quality job pages found via " + page.atsPlatform());
        }
        List<ApplicationPackage> prepared = runDailyPreparation();
        messages.add(queueSummary(repository.jobs(), repository.preferences(), prepared.size()));
        if (!prepared.isEmpty()) {
            messages.add("Prepared " + prepared.size() + " application packages for the Apply Queue.");
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
        List<String> notes = request.companyId() == null || request.companyId().isBlank()
                ? new ArrayList<>(List.of("Analyzed locally by Career Copilot."))
                : new ArrayList<>(List.of("Analyzed locally by Career Copilot.", "Discovered by career page scanner."));
        notes.addAll(learningNotes(request.company()));
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
                notes
        );
        return repository.saveJob(job);
    }

    public List<ApplicationPackage> runDailyPreparation() {
        recalculateStoredJobScores();
        CareerPreferences preferences = repository.preferences();
        int threshold = preparationThreshold(preferences);
        return repository.jobs().stream()
                .filter(this::fieldRelevant)
                .filter(job -> !hasActivePackage(job.id()))
                .filter(applicationPackageService::shouldPrepare)
                .filter(job -> !containsIgnoreCase(preferences.blacklistCompanies(), job.company()))
                .filter(job -> !historicallyBlocked(job.company()))
                .filter(job -> job.match().overallMatch() >= threshold)
                .filter(job -> !preferences.visaRequired() || job.visa().score() >= 50)
                .sorted(Comparator.comparing((JobRecord job) -> job.match().overallMatch()).reversed())
                .limit(30)
                .limit(preferences.maximumApplicationsPerDay())
                .map(this::prepareApplication)
                .toList();
    }

    private String queueSummary(List<JobRecord> jobs, CareerPreferences preferences, int prepared) {
        int threshold = preparationThreshold(preferences);
        long fieldRelevant = jobs.stream().filter(this::fieldRelevant).count();
        long scoreReady = jobs.stream()
                .filter(this::fieldRelevant)
                .filter(job -> job.match().overallMatch() >= threshold)
                .count();
        long visaReady = jobs.stream()
                .filter(this::fieldRelevant)
                .filter(job -> job.match().overallMatch() >= threshold)
                .filter(job -> !preferences.visaRequired() || job.visa().score() >= 50)
                .count();
        long alreadyPackaged = jobs.stream()
                .filter(this::fieldRelevant)
                .filter(job -> job.match().overallMatch() >= threshold)
                .filter(job -> !preferences.visaRequired() || job.visa().score() >= 50)
                .filter(job -> hasActivePackage(job.id()))
                .count();
        return "Apply Queue filter: " + fieldRelevant + " field-relevant jobs, "
                + scoreReady + " above queue score " + threshold + ", "
                + visaReady + " visa-compatible, "
                + alreadyPackaged + " already packaged, "
                + prepared + " newly prepared.";
    }

    private int preparationThreshold(CareerPreferences preferences) {
        return Math.max(65, Math.min(75, preferences.minimumMatchScore()));
    }

    private synchronized void recalculateStoredJobScores() {
        if (scoresRecalculated) {
            return;
        }
        List<JobRecord> jobs = repository.jobs();
        for (JobRecord job : jobs) {
            CompanyRecord company = repository.findCompany(job.companyId()).orElse(null);
            VisaAssessment visa = visaIntelligence.assess(job.description(), company);
            MatchAssessment match = matchEngine.score(job.title(), job.location(), job.description(), "", visa);
            var intelligence = intelligenceEngine.evaluate(
                    new VisaAnalysisRequest(job.company(), job.title(), job.description(), company == null ? 0 : company.visaConfidence(), company == null ? List.of() : company.learningHistory()),
                    new JobIntelligenceRequest(job.company(), job.title(), job.location(), "", remoteStatus(job.location()), Instant.now(), job.description(), List.of(), experienceLevel(job.title(), job.description()), null),
                    company != null && company.blocked(),
                    appliedStatus(job.applicationStatus())
            );
            JobIntelligence updatedIntelligence = new JobIntelligence(
                    intelligence.visa(),
                    intelligence.ranking(),
                    intelligence.recommendation(),
                    job.intelligence() == null ? null : job.intelligence().duplicate()
            );
            repository.saveJob(new JobRecord(
                    job.id(),
                    job.companyId(),
                    job.company(),
                    job.title(),
                    job.location(),
                    job.url(),
                    job.description(),
                    visa,
                    match,
                    updatedIntelligence,
                    refreshedStatus(job.applicationStatus(), updatedIntelligence),
                    match.overallMatch() >= 75 && visa.score() >= 50,
                    match.overallMatch() >= 70 && visa.score() >= 50,
                    job.discoveredAt(),
                    Instant.now(),
                    job.notes()
            ));
        }
        scoresRecalculated = true;
    }

    private boolean appliedStatus(String status) {
        return status != null && status.toLowerCase(Locale.ROOT).contains("applied");
    }

    private String refreshedStatus(String current, JobIntelligence intelligence) {
        if (current == null || current.isBlank()
                || current.equalsIgnoreCase("Discovered")
                || current.equalsIgnoreCase("Ready to apply")
                || current.equalsIgnoreCase("Skipped - visa risk")) {
            return applicationStatus(intelligence);
        }
        return current;
    }

    private boolean hasActivePackage(String jobId) {
        return repository.applications().stream().anyMatch(applicationPackage ->
                applicationPackage.jobId().equals(jobId)
                        && !applicationPackage.status().equals("BLOCKED")
                        && !applicationPackage.status().equals("WITHDRAWN"));
    }

    public ApplicationPackage prepareApplication(JobRecord job) {
        ApplicationPackage applicationPackage = applicationPackageService.create(job);
        String resume = applicationPackageService.resumeMarkdown(job);
        String coverLetter = applicationPackageService.coverLetterMarkdown(job);
        String folder = "applications/" + applicationPackage.id();
        repository.writeApplicationText(applicationPackage, folder + "/resume.md", resume);
        repository.writeApplicationBytes(applicationPackage, applicationPackage.resumePath(), pdf(resume));
        repository.writeApplicationBytes(applicationPackage, folder + "/resume.docx", docx(resume));
        repository.writeApplicationText(applicationPackage, "resumes/generated/" + applicationPackage.resumeVersion() + ".md", resume);
        repository.writeApplicationText(applicationPackage, folder + "/cover-letter.md", coverLetter);
        repository.writeApplicationBytes(applicationPackage, applicationPackage.coverLetterPath(), pdf(coverLetter));
        repository.writeApplicationBytes(applicationPackage, folder + "/cover-letter.docx", docx(coverLetter));
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

    public ApplicationReview applicationReview(String applicationId) {
        ApplicationPackage applicationPackage = repository.applications().stream()
                .filter(item -> item.id().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application package not found: " + applicationId));
        String folder = "applications/" + applicationPackage.id();
        return new ApplicationReview(
                readText(folder + "/resume.md"),
                readText(folder + "/cover-letter.md"),
                applicationPackage.answers(),
                applicationPackage
        );
    }

    public ApplicationPackage saveApplicationReview(String applicationId, ApplicationReview review) {
        ApplicationPackage applicationPackage = repository.applications().stream()
                .filter(item -> item.id().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application package not found: " + applicationId));
        String folder = "applications/" + applicationPackage.id();
        repository.writeApplicationText(applicationPackage, folder + "/resume.md", review.resume());
        repository.writeApplicationBytes(applicationPackage, applicationPackage.resumePath(), pdf(review.resume()));
        repository.writeApplicationBytes(applicationPackage, folder + "/resume.docx", docx(review.resume()));
        repository.writeApplicationText(applicationPackage, folder + "/cover-letter.md", review.coverLetter());
        repository.writeApplicationBytes(applicationPackage, applicationPackage.coverLetterPath(), pdf(review.coverLetter()));
        repository.writeApplicationBytes(applicationPackage, folder + "/cover-letter.docx", docx(review.coverLetter()));
        repository.writeApplicationArtifact(applicationPackage, applicationPackage.answersPath(), review.answers());
        for (var answer : review.answers()) {
            rememberAnswer(answer.question(), answer.answer(), applicationPackage.company(), "user-approved");
        }
        return repository.saveApplication(new ApplicationPackage(
                applicationPackage.id(),
                applicationPackage.jobId(),
                applicationPackage.company(),
                applicationPackage.title(),
                "REVIEWED",
                applicationPackage.recommendationConfidence(),
                applicationPackage.recommendation(),
                applicationPackage.resumeVersion(),
                applicationPackage.resumePath(),
                applicationPackage.coverLetterPath(),
                applicationPackage.answersPath(),
                applicationPackage.reportPath(),
                review.answers(),
                applicationPackage.createdAt(),
                Instant.now()
        ));
    }

    public ApplicationExecutionResult executeApplication(String applicationId) {
        ApplicationPackage applicationPackage = repository.applications().stream()
                .filter(item -> item.id().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application package not found: " + applicationId));
        if (!applicationPackage.status().equals("APPROVED_FOR_BROWSER_AGENT")) {
            applicationPackage = approveApplication(applicationId);
        }
        String jobId = applicationPackage.jobId();
        JobRecord job = repository.findJob(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found for application package: " + jobId));
        if (job.url() == null || job.url().isBlank()) {
            return saveExecution(applicationPackage, "PAUSED_FOR_MANUAL_REVIEW", "Job URL is missing", List.of(), List.of(), true, "missing-job-url");
        }
        var result = browserAutomation.apply(new BrowserApplicationRequest(
                job.url(),
                repository.resolveCareerPath("applications/" + applicationPackage.id() + "/browser"),
                repository.resolveCareerPath(applicationPackage.resumePath()),
                repository.resolveCareerPath(applicationPackage.coverLetterPath()),
                applicationPackage.answers().stream()
                        .map(answer -> new BrowserApplicationRequest.QuestionAnswer(answer.question(), answer.answer()))
                        .toList(),
                profileMap(repository.userProfile())
        ));
        ApplicationExecutionResult execution = saveExecution(applicationPackage, result.status(), result.pauseReason(), result.actions(), result.screenshots(), result.fallback(), result.error());
        learnExecution(applicationPackage, job, execution);
        return execution;
    }

    public List<ApplicationExecutionResult> executeReadyApplications() {
        List<ApplicationExecutionResult> results = new ArrayList<>();
        List<ApplicationPackage> ready = repository.applications().stream()
                .filter(application -> application.status().equals("WAITING_FOR_REVIEW")
                        || application.status().equals("REVIEWED")
                        || application.status().equals("APPROVED_FOR_BROWSER_AGENT"))
                .sorted(Comparator.comparing(ApplicationPackage::recommendationConfidence).reversed())
                .limit(repository.preferences().maximumApplicationsPerDay())
                .toList();
        for (ApplicationPackage application : ready) {
            ApplicationExecutionResult result;
            try {
                result = executeApplication(application.id());
            } catch (Exception ex) {
                result = saveExecution(application, "PAUSED_FOR_MANUAL_REVIEW", "Unexpected application execution error", List.of(), List.of(), true, ex.getMessage());
            }
            results.add(result);
            if (!result.status().equals("SUBMITTED") && !result.status().equals("APPLIED")) {
                break;
            }
        }
        return results;
    }

    public ApplicationHistoryRecord markApplication(String applicationId, String status, String note) {
        ApplicationPackage applicationPackage = repository.applications().stream()
                .filter(item -> item.id().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application package not found: " + applicationId));
        String normalizedStatus = normalizeApplicationStatus(status);
        ApplicationPackage updated = new ApplicationPackage(
                applicationPackage.id(),
                applicationPackage.jobId(),
                applicationPackage.company(),
                applicationPackage.title(),
                normalizedStatus,
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
        );
        repository.saveApplication(updated);
        repository.findJob(updated.jobId()).ifPresent(job -> repository.saveJob(new JobRecord(
                job.id(),
                job.companyId(),
                job.company(),
                job.title(),
                job.location(),
                job.url(),
                job.description(),
                job.visa(),
                job.match(),
                job.intelligence(),
                normalizedStatus,
                job.resumeReady(),
                job.coverLetterReady(),
                job.discoveredAt(),
                Instant.now(),
                append(job.notes(), "Application marked " + normalizedStatus + ".")
        )));
        ApplicationHistoryRecord record = new ApplicationHistoryRecord(
                updated.id(),
                updated.jobId(),
                updated.company(),
                updated.title(),
                normalizedStatus,
                note == null ? "" : note,
                updated.resumeVersion(),
                updated.resumePath(),
                updated.coverLetterPath(),
                updated.answersPath(),
                updated.answers().stream().map(answer -> answer.question()).toList(),
                Instant.now()
        );
        repository.writeApplicationArtifact(updated, "applications/" + updated.id() + "/application-history-" + record.recordedAt().toEpochMilli() + ".json", record);
        return repository.saveApplicationHistory(record);
    }

    private ApplicationExecutionResult saveExecution(ApplicationPackage applicationPackage, String status, String pauseReason, List<String> actions, List<java.nio.file.Path> screenshots, boolean fallback, String error) {
        ApplicationExecutionResult execution = new ApplicationExecutionResult(applicationPackage.id(), status, pauseReason, actions, screenshots, fallback, error, Instant.now());
        repository.writeApplicationArtifact(applicationPackage, "applications/" + applicationPackage.id() + "/browser/execution-result.json", execution);
        repository.saveApplication(new ApplicationPackage(
                applicationPackage.id(),
                applicationPackage.jobId(),
                applicationPackage.company(),
                applicationPackage.title(),
                status,
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
        ));
        return execution;
    }

    private String readText(String relativePath) {
        try {
            java.nio.file.Path path = repository.resolveCareerPath(relativePath);
            return Files.exists(path) ? Files.readString(path) : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private String normalizeApplicationStatus(String status) {
        if (status == null || status.isBlank()) {
            return "NEEDS_REVIEW";
        }
        return switch (status.trim().toUpperCase()) {
            case "SUBMITTED", "APPLIED" -> "APPLIED";
            case "BLOCKED" -> "BLOCKED";
            case "WITHDRAWN" -> "WITHDRAWN";
            case "INTERVIEW" -> "INTERVIEW";
            case "REJECTED" -> "REJECTED";
            case "OFFER" -> "OFFER";
            case "GHOSTED" -> "GHOSTED";
            default -> status.trim().toUpperCase().replace(' ', '_');
        };
    }

    private void learnProfile(UserProfile profile) {
        rememberProfile("preferred_name", "Preferred name", profile.name());
        rememberProfile("email", "Email", profile.email());
        rememberProfile("phone", "Phone number", profile.phone());
        rememberProfile("address", "Address", profile.address());
        rememberProfile("linkedin", "LinkedIn", profile.linkedin());
        rememberProfile("github", "GitHub", profile.github());
        rememberProfile("portfolio", "Portfolio", profile.portfolio());
        rememberProfile("work_authorization", "Work authorization", profile.workAuthorization());
        rememberProfile("sponsorship_required", "Will you require sponsorship?", profile.sponsorshipRequirement());
        rememberProfile("education", "Education", profile.education());
        rememberProfile("employment_history", "Employment history", profile.employmentHistory());
    }

    private void rememberProfile(String intent, String question, String answer) {
        if (answer == null || answer.isBlank()) {
            return;
        }
        remember("question", "global", intent, intent, question, answer, "", "", Map.of(), "user", 85);
    }

    private void rememberAnswer(String question, String answer, String company, String source) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) {
            return;
        }
        String intent = normalizeIntent(question);
        remember("approved_answer", "global", intent, intent, question, answer, company, "", Map.of("jobCategory", "career_application"), source, 85);
    }

    private void learnExecution(ApplicationPackage applicationPackage, JobRecord job, ApplicationExecutionResult execution) {
        CompanyRecord company = repository.findCompany(job.companyId()).orElse(null);
        String ats = company == null ? "Generic" : company.atsPlatform();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", execution.status());
        data.put("pauseReason", execution.pauseReason());
        data.put("actions", execution.actions());
        data.put("screenshots", execution.screenshots().stream().map(Object::toString).toList());
        data.put("error", execution.error());
        remember("company_memory", job.company(), applicationPackage.id(), "", "", execution.pauseReason(), job.company(), ats, data, "browser", execution.fallback() ? 45 : 75);
        remember("ats_memory", ats, job.company() + "-" + applicationPackage.id(), "", "", execution.pauseReason(), job.company(), ats, data, "browser", execution.fallback() ? 45 : 75);
        if (execution.error() != null && !execution.error().isBlank()) {
            remember("workflow_memory", job.company(), "failure-" + applicationPackage.id(), "browser_failure", "Browser failure", execution.error(), job.company(), ats, data, "browser", 40);
        }
    }

    private void remember(String type, String scope, String key, String intent, String question, String answer, String company, String ats, Map<String, Object> data, String source, int confidence) {
        Instant now = Instant.now();
        String normalizedIntent = normalizeIntent(intent == null || intent.isBlank() ? question : intent);
        String id = com.atlas.common.Slug.of(type + "-" + scope + "-" + key + "-" + normalizedIntent);
        MemoryRecord existing = repository.memories().stream()
                .filter(memory -> memory.id().equals(id))
                .findFirst()
                .orElse(null);
        repository.saveMemory(new MemoryRecord(
                id,
                type,
                scope,
                key,
                normalizedIntent,
                question == null ? "" : question,
                answer == null ? "" : answer,
                company == null ? "" : company,
                ats == null ? "" : ats,
                data == null ? Map.of() : data,
                existing == null ? confidence : clamp((existing.confidence() + confidence + 5) / 2, 1, 100),
                existing == null ? 0 : existing.usageCount(),
                source,
                existing == null ? now : existing.createdAt(),
                existing == null ? now : existing.lastUsed(),
                now
        ));
    }

    public String rememberedAnswer(String question, String company) {
        String intent = normalizeIntent(question);
        return repository.memories().stream()
                .filter(memory -> memory.type().equals("approved_answer") || memory.type().equals("question"))
                .filter(memory -> intent.equals(memory.intent()))
                .filter(memory -> memory.answer() != null && !memory.answer().isBlank())
                .sorted(Comparator.comparing(MemoryRecord::confidence).reversed().thenComparing(MemoryRecord::lastUsed).reversed())
                .findFirst()
                .map(memory -> {
                    repository.saveMemory(new MemoryRecord(memory.id(), memory.type(), memory.scope(), memory.key(), memory.intent(), memory.question(), memory.answer(), memory.company(), memory.ats(), memory.data(), clamp(memory.confidence() + 2, 1, 100), memory.usageCount() + 1, memory.source(), memory.createdAt(), Instant.now(), Instant.now()));
                    return memory.answer();
                })
                .orElse("");
    }

    private String normalizeIntent(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (text.contains("sponsor") || text.contains("h1b") || text.contains("h-1b") || text.contains("visa")) return "sponsorship_required";
        if (text.contains("authorization") || text.contains("authorized") || text.contains("citizen")) return "work_authorization";
        if (text.contains("salary") || text.contains("compensation") || text.contains("pay")) return "salary_expectation";
        if (text.contains("notice") || text.contains("start date") || text.contains("available")) return "notice_period";
        if (text.contains("relocat")) return "relocation";
        if (text.contains("travel")) return "travel";
        if (text.contains("phone")) return "phone";
        if (text.contains("address")) return "address";
        if (text.contains("linkedin")) return "linkedin";
        if (text.contains("github")) return "github";
        if (text.contains("portfolio") || text.contains("website")) return "portfolio";
        if (text.contains("name")) return "preferred_name";
        return com.atlas.common.Slug.of(text);
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private CareerLearningInsight insight(String company, List<ApplicationHistoryRecord> records) {
        int applied = count(records, "APPLIED");
        int interviews = count(records, "INTERVIEW");
        int offers = count(records, "OFFER");
        int rejections = count(records, "REJECTED");
        int blocked = count(records, "BLOCKED");
        int score = applied * 5 + interviews * 25 + offers * 50 - rejections * 8 - blocked * 20;
        String recommendation = score >= 40 ? "Prioritize" : score < 0 ? "Deprioritize" : "Keep watching";
        return new CareerLearningInsight(company, applied, interviews, offers, rejections, blocked, score, recommendation);
    }

    private int count(List<ApplicationHistoryRecord> records, String status) {
        return (int) records.stream().filter(record -> record.status().equals(status)).count();
    }

    private List<String> learningNotes(String company) {
        return learningInsights().stream()
                .filter(insight -> insight.company().equalsIgnoreCase(company))
                .findFirst()
                .map(insight -> List.of("Learning: " + insight.recommendation() + " " + insight.company() + " based on past outcomes."))
                .orElse(List.of());
    }

    private boolean historicallyBlocked(String company) {
        return learningInsights().stream()
                .anyMatch(insight -> insight.company().equalsIgnoreCase(company) && insight.blocked() >= 2 && insight.score() < 0);
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

    private boolean scannerCandidate(JobDiscoveryService.DiscoveredJob job) {
        CareerPreferences preferences = repository.preferences();
        String text = ((job.title() == null ? "" : job.title()) + "\n" + (job.description() == null ? "" : job.description())).toLowerCase();
        if (text.contains("intern") || text.contains("internship") || text.contains("university grad") || text.contains("new grad")) {
            return false;
        }
        boolean staffAllowed = preferences.preferredTitles().stream().anyMatch(title -> title.toLowerCase().contains("principal") || title.toLowerCase().contains("staff"));
        if (!staffAllowed && (text.contains("principal") || text.contains("director") || text.contains("vp ") || text.contains("executive"))) {
            return false;
        }
        boolean titleMatch = preferences.preferredTitles().stream()
                .map(title -> title.toLowerCase().replace("senior", "").replace("lead", "").trim())
                .filter(title -> title.length() >= 4)
                .anyMatch(text::contains);
        boolean skillMatch = preferences.preferredSkills().stream()
                .map(String::toLowerCase)
                .filter(skill -> skill.length() >= 3)
                .anyMatch(text::contains);
        return titleMatch || skillMatch || text.contains("backend") || text.contains("spring") || text.contains("java") || text.contains("microservice");
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        if (values == null || candidate == null) {
            return false;
        }
        return values.stream().anyMatch(value -> candidate.equalsIgnoreCase(value));
    }

    private java.util.Map<String, String> profileMap(UserProfile profile) {
        return java.util.Map.ofEntries(
                java.util.Map.entry("name", profile.name()),
                java.util.Map.entry("email", profile.email()),
                java.util.Map.entry("defaultPassword", profile.defaultPassword()),
                java.util.Map.entry("phone", profile.phone()),
                java.util.Map.entry("address", profile.address()),
                java.util.Map.entry("linkedin", profile.linkedin()),
                java.util.Map.entry("github", profile.github()),
                java.util.Map.entry("portfolio", profile.portfolio()),
                java.util.Map.entry("workAuthorization", profile.workAuthorization()),
                java.util.Map.entry("sponsorshipRequirement", profile.sponsorshipRequirement()),
                java.util.Map.entry("education", profile.education()),
                java.util.Map.entry("employmentHistory", profile.employmentHistory())
        );
    }

    public boolean fieldRelevant(JobRecord job) {
        return job.match() != null
                && (job.match().javaMatch() >= 70
                || job.match().springMatch() >= 70
                || job.match().backendMatch() >= 70);
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

    private byte[] pdf(String markdown) {
        String text = markdown.replace("#", "").replace("*", "");
        StringBuilder stream = new StringBuilder("BT /F1 11 Tf 50 760 Td 14 TL ");
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                stream.append("(").append(pdfEscape(line.length() > 95 ? line.substring(0, 95) : line)).append(") Tj T* ");
            }
        }
        stream.append("ET");
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream"
        );
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.UTF_8).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer << /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private byte[] docx(String markdown) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip(zip, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>
                        """);
                zip(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>
                        """);
                String paragraphs = java.util.Arrays.stream(markdown.replace("#", "").split("\\R"))
                        .filter(line -> !line.isBlank())
                        .map(line -> "<w:p><w:r><w:t>" + xml(line) + "</w:t></w:r></w:p>")
                        .collect(Collectors.joining());
                zip(zip, "word/document.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>%s</w:body></w:document>
                        """.formatted(paragraphs));
            }
            return out.toByteArray();
        } catch (Exception ex) {
            return markdown.getBytes(StandardCharsets.UTF_8);
        }
    }

    private void zip(ZipOutputStream zip, String name, String value) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public record ApplicationReview(String resume, String coverLetter, List<com.atlas.career.domain.ApplicationQuestionAnswer> answers, ApplicationPackage applicationPackage) {
    }
}
