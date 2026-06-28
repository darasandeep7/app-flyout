package com.atlas.career.service;

import com.atlas.career.api.AddCompanyRequest;
import com.atlas.career.api.AnalyzeJobRequest;
import com.atlas.career.api.CareerDashboard;
import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.VisaAssessment;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CareerWorkflow {
    private final CareerRepository repository;
    private final VisaIntelligenceService visaIntelligence;
    private final MatchEngine matchEngine;

    public CareerWorkflow(CareerRepository repository, VisaIntelligenceService visaIntelligence, MatchEngine matchEngine) {
        this.repository = repository;
        this.visaIntelligence = visaIntelligence;
        this.matchEngine = matchEngine;
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

    public CompanyRecord addCompany(AddCompanyRequest request) {
        String id = repository.companyId(request.name());
        CompanyRecord company = new CompanyRecord(
                id,
                request.name(),
                request.careerUrl(),
                "Unknown",
                "Tracked",
                "Learning from future scans",
                45,
                request.locations() == null ? List.of() : request.locations(),
                request.priority(),
                false,
                Instant.EPOCH,
                request.notes(),
                List.of("Company added manually.")
        );
        return repository.saveCompany(company);
    }

    public JobRecord analyzeJob(AnalyzeJobRequest request) {
        CompanyRecord company = repository.findCompany(request.companyId() == null || request.companyId().isBlank() ? request.company() : request.companyId()).orElse(null);
        VisaAssessment visa = visaIntelligence.assess(request.description(), company);
        MatchAssessment match = matchEngine.score(request.title(), request.location(), request.description(), request.salary(), visa);
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
                visa.score() < 40 ? "Skipped - visa risk" : "Discovered",
                match.overallMatch() >= 75 && visa.score() >= 50,
                match.overallMatch() >= 70 && visa.score() >= 50,
                Instant.now(),
                Instant.now(),
                List.of("Analyzed locally by Career Copilot.")
        );
        return repository.saveJob(job);
    }
}
