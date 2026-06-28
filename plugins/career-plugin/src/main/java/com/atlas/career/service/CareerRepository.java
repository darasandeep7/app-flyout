package com.atlas.career.service;

import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.JobRecord;
import com.atlas.common.Slug;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class CareerRepository {
    private final Path careerFolder;
    private final ObjectMapper objectMapper;

    public CareerRepository(@Value("${atlas.workspace-folder:workspace}") String workspaceFolder, ObjectMapper objectMapper) {
        this.careerFolder = Path.of(workspaceFolder).resolve("career");
        this.objectMapper = objectMapper.findAndRegisterModules();
        initialize();
    }

    public List<CompanyRecord> companies() {
        return readList(companiesPath(), new TypeReference<>() {
        });
    }

    public List<JobRecord> jobs() {
        return readList(jobsPath(), new TypeReference<>() {
        });
    }

    public CompanyRecord saveCompany(CompanyRecord company) {
        List<CompanyRecord> companies = new ArrayList<>(companies());
        companies.removeIf(existing -> existing.id().equals(company.id()));
        companies.add(company);
        companies.sort(Comparator.comparing(CompanyRecord::priority).reversed().thenComparing(CompanyRecord::name));
        write(companiesPath(), companies);
        return company;
    }

    public JobRecord saveJob(JobRecord job) {
        List<JobRecord> jobs = new ArrayList<>(jobs());
        jobs.removeIf(existing -> existing.id().equals(job.id()) || samePosting(existing, job));
        jobs.add(job);
        jobs.sort(Comparator.comparing(JobRecord::discoveredAt).reversed());
        write(jobsPath(), jobs);
        return job;
    }

    public Optional<CompanyRecord> findCompany(String companyIdOrName) {
        return companies().stream()
                .filter(company -> company.id().equals(companyIdOrName) || company.name().equalsIgnoreCase(companyIdOrName))
                .findFirst();
    }

    public String companyId(String name) {
        return Slug.of(name);
    }

    public String jobId(String company, String title, String location) {
        return Slug.of(company + "-" + title + "-" + location);
    }

    private boolean samePosting(JobRecord left, JobRecord right) {
        if (left.url() != null && !left.url().isBlank() && left.url().equalsIgnoreCase(right.url())) {
            return true;
        }
        return left.company().equalsIgnoreCase(right.company())
                && left.title().equalsIgnoreCase(right.title())
                && left.location().equalsIgnoreCase(right.location());
    }

    private <T> List<T> readList(Path path, TypeReference<List<T>> type) {
        if (Files.notExists(path)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(path.toFile(), type);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + path, ex);
        }
    }

    private void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write " + path, ex);
        }
    }

    private Path companiesPath() {
        return careerFolder.resolve("companies/companies.json");
    }

    private Path jobsPath() {
        return careerFolder.resolve("jobs/jobs.json");
    }

    private void initialize() {
        try {
            Files.createDirectories(careerFolder.resolve("companies"));
            Files.createDirectories(careerFolder.resolve("jobs"));
            Files.createDirectories(careerFolder.resolve("applications"));
            Files.createDirectories(careerFolder.resolve("reports"));
            Files.createDirectories(careerFolder.resolve("logs"));
            seedIfEmpty();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize career workspace", ex);
        }
    }

    private void seedIfEmpty() {
        if (Files.exists(companiesPath())) {
            return;
        }
        CompanyRecord sample = new CompanyRecord(
                "sample-company",
                "Sample Company",
                "https://example.com/careers",
                "Unknown",
                "Unknown",
                "Not enough scan history yet",
                40,
                List.of("Remote"),
                1,
                false,
                Instant.EPOCH,
                "Replace this with a real target company.",
                List.of("Seed record created by Atlas Career Copilot.")
        );
        write(companiesPath(), List.of(sample));
        write(jobsPath(), List.of());
    }
}
