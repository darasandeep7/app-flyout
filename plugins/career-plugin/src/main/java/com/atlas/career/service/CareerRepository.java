package com.atlas.career.service;

import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.ApplicationHistoryRecord;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MasterResume;
import com.atlas.common.Slug;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
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

    public List<ApplicationPackage> applications() {
        return readList(applicationsPath(), new TypeReference<>() {
        });
    }

    public List<ApplicationHistoryRecord> applicationHistory() {
        return readList(applicationHistoryPath(), new TypeReference<>() {
        });
    }

    public CareerPreferences preferences() {
        if (Files.notExists(preferencesPath())) {
            CareerPreferences defaults = CareerPreferences.defaults();
            write(preferencesPath(), defaults);
            return defaults;
        }
        try {
            return objectMapper.readValue(preferencesPath().toFile(), CareerPreferences.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + preferencesPath(), ex);
        }
    }

    public CareerPreferences savePreferences(CareerPreferences preferences) {
        CareerPreferences safe = new CareerPreferences(
                listOrDefault(preferences.preferredTitles(), CareerPreferences.defaults().preferredTitles()),
                listOrDefault(preferences.preferredSkills(), CareerPreferences.defaults().preferredSkills()),
                listOrDefault(preferences.preferredLocations(), CareerPreferences.defaults().preferredLocations()),
                blankToDefault(preferences.remotePreference(), CareerPreferences.defaults().remotePreference()),
                blankToDefault(preferences.hybridPreference(), CareerPreferences.defaults().hybridPreference()),
                Math.max(0, preferences.minimumSalary()),
                preferences.visaRequired(),
                clamp(preferences.minimumMatchScore(), 0, 100),
                listOrEmpty(preferences.blacklistCompanies()),
                listOrEmpty(preferences.whitelistCompanies()),
                blankToDefault(preferences.dailyScanTime(), CareerPreferences.defaults().dailyScanTime()),
                Math.max(1, preferences.maximumApplicationsPerDay())
        );
        write(preferencesPath(), safe);
        return safe;
    }

    public MasterResume masterResume() {
        if (Files.notExists(masterResumePath())) {
            MasterResume empty = MasterResume.empty();
            write(masterResumePath(), empty);
            return empty;
        }
        try {
            MasterResume saved = objectMapper.readValue(masterResumePath().toFile(), MasterResume.class);
            return new MasterResume(
                    saved.content(),
                    listOrDefault(saved.preferredSkills(), MasterResume.empty().preferredSkills()),
                    listOrDefault(saved.preferredKeywords(), MasterResume.empty().preferredKeywords()),
                    resumeVersions(),
                    saved.updatedAt() == null ? Instant.EPOCH : saved.updatedAt()
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + masterResumePath(), ex);
        }
    }

    public MasterResume saveMasterResume(MasterResume masterResume) {
        MasterResume current = masterResume();
        String content = masterResume.content() == null ? "" : masterResume.content();
        if (!content.equals(current.content())) {
            writeResumeVersion(current.content());
        }
        MasterResume saved = new MasterResume(
                content,
                listOrDefault(masterResume.preferredSkills(), MasterResume.empty().preferredSkills()),
                listOrDefault(masterResume.preferredKeywords(), MasterResume.empty().preferredKeywords()),
                resumeVersions(),
                Instant.now()
        );
        write(masterResumePath(), saved);
        return new MasterResume(saved.content(), saved.preferredSkills(), saved.preferredKeywords(), resumeVersions(), saved.updatedAt());
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

    public int removeExpiredScannerJobs(String companyId, List<String> activeUrls) {
        List<JobRecord> jobs = new ArrayList<>(jobs());
        int before = jobs.size();
        jobs.removeIf(job -> companyId.equals(job.companyId())
                && job.notes() != null
                && job.notes().stream().anyMatch(note -> note.equalsIgnoreCase("Discovered by career page scanner."))
                && (job.url() == null || activeUrls.stream().noneMatch(url -> url.equalsIgnoreCase(job.url()))));
        if (jobs.size() != before) {
            write(jobsPath(), jobs);
        }
        return before - jobs.size();
    }

    public void appendLog(String fileName, Object value) {
        write(careerFolder.resolve("logs").resolve(fileName), value);
    }

    public Path resolveCareerPath(String relativePath) {
        Path path = careerFolder.resolve(relativePath).normalize();
        if (!path.startsWith(careerFolder)) {
            throw new IllegalArgumentException("Invalid career path");
        }
        return path;
    }

    public ApplicationPackage saveApplication(ApplicationPackage applicationPackage) {
        List<ApplicationPackage> applications = new ArrayList<>(applications());
        applications.removeIf(existing -> existing.id().equals(applicationPackage.id()));
        applications.add(applicationPackage);
        applications.sort(Comparator.comparing(ApplicationPackage::createdAt).reversed());
        write(applicationsPath(), applications);
        return applicationPackage;
    }

    public ApplicationHistoryRecord saveApplicationHistory(ApplicationHistoryRecord record) {
        List<ApplicationHistoryRecord> history = new ArrayList<>(applicationHistory());
        history.add(record);
        history.sort(Comparator.comparing(ApplicationHistoryRecord::recordedAt).reversed());
        write(applicationHistoryPath(), history);
        return record;
    }

    public void writeApplicationArtifact(ApplicationPackage applicationPackage, String relativePath, Object value) {
        Path path = careerFolder.resolve(relativePath).normalize();
        if (!path.startsWith(careerFolder)) {
            throw new IllegalArgumentException("Invalid application artifact path");
        }
        write(path, value);
    }

    public void writeApplicationText(ApplicationPackage applicationPackage, String relativePath, String value) {
        Path path = careerFolder.resolve(relativePath).normalize();
        if (!path.startsWith(careerFolder)) {
            throw new IllegalArgumentException("Invalid application artifact path");
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, value);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write " + path, ex);
        }
    }

    public void writeApplicationBytes(ApplicationPackage applicationPackage, String relativePath, byte[] value) {
        Path path = careerFolder.resolve(relativePath).normalize();
        if (!path.startsWith(careerFolder)) {
            throw new IllegalArgumentException("Invalid application artifact path");
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, value);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write " + path, ex);
        }
    }

    public Optional<CompanyRecord> findCompany(String companyIdOrName) {
        return companies().stream()
                .filter(company -> company.id().equals(companyIdOrName) || company.name().equalsIgnoreCase(companyIdOrName))
                .findFirst();
    }

    public Optional<JobRecord> findJob(String jobId) {
        return jobs().stream()
                .filter(job -> job.id().equals(jobId))
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

    private Path applicationsPath() {
        return careerFolder.resolve("applications/applications.json");
    }

    private Path applicationHistoryPath() {
        return careerFolder.resolve("applications/history.json");
    }

    private Path preferencesPath() {
        return careerFolder.resolve("preferences/preferences.json");
    }

    private Path masterResumePath() {
        return careerFolder.resolve("resumes/master-resume.json");
    }

    private Path resumeVersionsFolder() {
        return careerFolder.resolve("resumes/versions");
    }

    private void initialize() {
        try {
            Files.createDirectories(careerFolder.resolve("companies"));
            Files.createDirectories(careerFolder.resolve("jobs"));
            Files.createDirectories(careerFolder.resolve("applications"));
            Files.createDirectories(careerFolder.resolve("preferences"));
            Files.createDirectories(careerFolder.resolve("resumes"));
            Files.createDirectories(resumeVersionsFolder());
            Files.createDirectories(careerFolder.resolve("coverLetters"));
            Files.createDirectories(careerFolder.resolve("answers"));
            Files.createDirectories(careerFolder.resolve("reports"));
            Files.createDirectories(careerFolder.resolve("logs"));
            seedIfEmpty();
            importSeedCompanies();
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
                "Unknown",
                "https://example.com",
                "https://example.com/careers",
                "Unknown",
                "Unknown",
                "Unknown",
                "Not enough scan history yet",
                40,
                List.of("Remote"),
                0,
                0,
                0,
                0,
                0,
                1,
                false,
                Instant.EPOCH,
                Instant.now(),
                40,
                "Replace this with a real target company.",
                List.of(),
                List.of("Seed record created by Atlas Career Copilot.")
        );
        write(companiesPath(), List.of(sample));
        write(jobsPath(), List.of());
        write(preferencesPath(), CareerPreferences.defaults());
        write(masterResumePath(), MasterResume.empty());
    }

    private void importSeedCompanies() {
        InputStream stream = getClass().getResourceAsStream("/career-seed-companies.csv");
        if (stream == null) {
            return;
        }

        Map<String, CompanyRecord> merged = new LinkedHashMap<>();
        for (CompanyRecord company : companies()) {
            merged.put(company.id(), company);
        }

        int before = merged.size();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.lines()
                    .skip(1)
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .forEach(line -> addSeedCompany(line, merged));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not import career seed companies", ex);
        }

        if (merged.size() != before) {
            merged.remove("sample-company");
            List<CompanyRecord> companies = merged.values().stream()
                    .sorted(Comparator.comparing(CompanyRecord::priority).reversed().thenComparing(CompanyRecord::name))
                    .toList();
            write(companiesPath(), companies);
        }
    }

    private void addSeedCompany(String line, Map<String, CompanyRecord> merged) {
        String[] parts = line.split(",", 2);
        if (parts.length != 2) {
            return;
        }
        String name = parts[0].trim();
        String careerUrl = parts[1].trim();
        if (name.isBlank() || careerUrl.isBlank()) {
            return;
        }
        String id = Slug.of(name);
        if (merged.containsKey(id)) {
            return;
        }
        String website = careerUrl
                .replaceFirst("^https://careers\\.", "https://www.")
                .replaceFirst("^https://jobs\\.", "https://www.");
        CompanyRecord company = new CompanyRecord(
                id,
                name,
                "Technology",
                website,
                careerUrl,
                "Unknown",
                "Unknown",
                "Unknown",
                "Seed company. Atlas has not learned sponsorship history yet.",
                50,
                List.of("United States", "Remote"),
                0,
                0,
                0,
                0,
                0,
                5,
                false,
                Instant.EPOCH,
                Instant.now(),
                50,
                "Seed company imported from Sandeep's starter target list.",
                List.of(),
                List.of("Seed company imported by Atlas Career Copilot.")
        );
        merged.put(id, company);
    }

    private void writeResumeVersion(String content) {
        if (content == null || content.isBlank() || content.equals(MasterResume.empty().content())) {
            return;
        }
        try {
            Files.createDirectories(resumeVersionsFolder());
            Files.writeString(resumeVersionsFolder().resolve("master-resume-" + Instant.now().toEpochMilli() + ".md"), content);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write resume version", ex);
        }
    }

    private List<String> resumeVersions() {
        if (Files.notExists(resumeVersionsFolder())) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(resumeVersionsFolder())) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .map(path -> careerFolder.relativize(path).toString())
                    .sorted(Comparator.reverseOrder())
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not list resume versions", ex);
        }
    }

    private List<String> listOrDefault(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private List<String> listOrEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
