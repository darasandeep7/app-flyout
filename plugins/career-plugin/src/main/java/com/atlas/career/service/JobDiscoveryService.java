package com.atlas.career.service;

import com.atlas.browser.BrowserAutomation;
import com.atlas.browser.BrowserRequest;
import com.atlas.browser.BrowserResult;
import com.atlas.career.domain.CompanyRecord;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JobDiscoveryService {
    private static final Pattern ANCHOR = Pattern.compile("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern JOB_ID = Pattern.compile("\\b(job|requisition|req|posting)\\s*(id|number|#)?\\s*[:#-]?\\s*[a-z0-9-]{4,}\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> NEGATIVE_TERMS = List.of(
            "cookie", "privacy", "search", "search results", "sign in", "login", "register", "create account",
            "forgot password", "reset password", "profile", "personal information", "account settings",
            "talent community", "join talent community", "university programs", "intern resources", "students",
            "faqs", "faq", "help", "support", "contact us", "benefits overview", "life at", "explore teams",
            "locations", "offices", "news", "events", "terms", "accessibility", "sitemap", "home", "next",
            "previous", "view all", "filter", "sort", "see next steps", "explore university opportunities"
    );
    private static final List<String> NEGATIVE_URL_TERMS = List.of(
            "login", "register", "cookie", "privacy", "support", "faq", "search", "community", "events",
            "news", "help", "signin", "sign-in", "account", "profile", "student", "university", "teams",
            "locations", "benefits"
    );
    private static final List<String> POSITIVE_URL_TERMS = List.of(
            "job", "jobs", "position", "opening", "requisition", "req", "posting", "role", "vacancy",
            "lever.co", "greenhouse.io", "myworkdayjobs", "smartrecruiters", "icims", "successfactors",
            "oraclecloud"
    );
    private static final List<String> POSITIVE_PAGE_TERMS = List.of(
            "apply", "apply now", "job description", "responsibilities", "qualifications", "required skills",
            "preferred skills", "benefits", "employment type", "department", "team", "location", "salary",
            "work authorization", "eeo", "equal employment", "about the role", "what you'll do", "what you will do",
            "requirements", "minimum qualifications", "preferred qualifications"
    );
    private final BrowserAutomation browserAutomation;
    private final Path careerFolder;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public JobDiscoveryService(BrowserAutomation browserAutomation, @Value("${atlas.workspace-folder:workspace}") String workspaceFolder) {
        this.browserAutomation = browserAutomation;
        this.careerFolder = Path.of(workspaceFolder).resolve("career");
    }

    public ScanPage scan(CompanyRecord company) {
        String html = fetch(company.careerUrl());
        String ats = classifyAts(company.careerUrl(), html);
        Map<String, DiscoveredJob> jobs = new LinkedHashMap<>();
        extractLinks(company.careerUrl(), html, jobs);
        for (String pageUrl : paginationUrls(company.careerUrl())) {
            if (jobs.size() >= 80) {
                break;
            }
            extractLinks(pageUrl, fetch(pageUrl), jobs);
        }
        if (jobs.size() < 5) {
            BrowserResult result = browserAutomation.inspect(new BrowserRequest(company.careerUrl(), careerFolder.resolve("logs/browser-scan-" + company.id()), true, false));
            ats = classifyAts(company.careerUrl(), html + " " + result.visibleText());
            Object links = result.structured().get("links");
            if (links instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        addJob(company.careerUrl(), String.valueOf(map.get("href")), String.valueOf(map.get("text")), jobs);
                    }
                }
            }
        }
        return new ScanPage(ats, new ArrayList<>(jobs.values()));
    }

    private void extractLinks(String baseUrl, String html, Map<String, DiscoveredJob> jobs) {
        var matcher = ANCHOR.matcher(html);
        while (matcher.find() && jobs.size() < 80) {
            addJob(baseUrl, decode(matcher.group(1)), cleanText(matcher.group(2)), jobs);
        }
    }

    private void addJob(String baseUrl, String hrefValue, String title, Map<String, DiscoveredJob> jobs) {
        String href = absoluteUrl(baseUrl, hrefValue);
        if (!looksLikeJobLink(href, title)) {
            return;
        }
        String normalizedTitle = normalizeTitle(title, href);
        if (normalizedTitle.isBlank()) {
            return;
        }
        String html = fetchQuietly(href);
        String description = normalizeDescription(html, normalizedTitle);
        JobQuality quality = quality(href, normalizedTitle, description);
        if (quality.score() >= 70) {
            String key = dedupeKey(href, normalizedTitle, locationFrom(description), description);
            jobs.putIfAbsent(key, new DiscoveredJob(normalizedTitle, locationFrom(description), href, description + "\n\nDiscovery Quality: " + quality.score() + "% - " + quality.reason()));
        }
    }

    private List<String> paginationUrls(String careerUrl) {
        if (careerUrl == null || careerUrl.isBlank()) {
            return List.of();
        }
        String separator = careerUrl.contains("?") ? "&" : "?";
        return List.of(
                careerUrl + separator + "page=2",
                careerUrl + separator + "page=3",
                careerUrl + separator + "start=10",
                careerUrl + separator + "start=20",
                careerUrl + separator + "offset=10",
                careerUrl + separator + "offset=20"
        );
    }

    private String fetch(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.trim()))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "AtlasCareerCopilot/1.0")
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception ex) {
            return "";
        }
    }

    private String fetchQuietly(String url) {
        String html = fetch(url);
        return html.length() > 80_000 ? html.substring(0, 80_000) : html;
    }

    private String normalizeDescription(String html, String fallbackTitle) {
        String text = cleanText(html);
        if (text.length() < 200) {
            return fallbackTitle;
        }
        return text.length() > 12_000 ? text.substring(0, 12_000) : text;
    }

    private boolean looksLikeJobLink(String href, String title) {
        String value = (href + " " + title).toLowerCase(Locale.ROOT);
        if (href == null || href.isBlank() || hasAny(value, NEGATIVE_TERMS) || hasAny(href.toLowerCase(Locale.ROOT), NEGATIVE_URL_TERMS)) {
            return false;
        }
        if (title.length() > 120 || title.length() < 4) {
            return false;
        }
        return hasAny(value, POSITIVE_URL_TERMS);
    }

    private JobQuality quality(String href, String title, String description) {
        String url = href == null ? "" : href.toLowerCase(Locale.ROOT);
        String titleText = title == null ? "" : title.toLowerCase(Locale.ROOT);
        String body = description == null ? "" : description.toLowerCase(Locale.ROOT);
        String combined = url + " " + titleText + " " + body;
        if (hasAny(combined, NEGATIVE_TERMS) || hasAny(url, NEGATIVE_URL_TERMS) || titleLooksLikeNavigation(titleText)) {
            return new JobQuality(10, "Rejected by negative navigation/account/search/talent-community signal.");
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (hasAny(url, POSITIVE_URL_TERMS)) {
            score += 18;
            reasons.add("job-like URL");
        }
        if (JOB_ID.matcher(combined).find() || url.matches(".*\\b(req|job)[-/]?[a-z0-9-]{4,}.*")) {
            score += 22;
            reasons.add("job id/requisition signal");
        }
        if (body.contains("apply now") || body.contains("apply for this job") || body.contains("submit application") || body.contains("apply")) {
            score += 20;
            reasons.add("apply action");
        } else {
            score -= 20;
            reasons.add("no apply action");
        }
        int pageSignals = 0;
        for (String signal : POSITIVE_PAGE_TERMS) {
            if (body.contains(signal)) {
                pageSignals++;
            }
        }
        score += Math.min(30, pageSignals * 5);
        if (pageSignals > 0) {
            reasons.add(pageSignals + " job content signals");
        }
        if (titleHasRoleSignal(titleText)) {
            score += 15;
            reasons.add("role-like title");
        }
        if (body.length() >= 700) {
            score += 8;
            reasons.add("substantial page text");
        }
        return new JobQuality(Math.max(0, Math.min(100, score)), String.join(", ", reasons));
    }

    private boolean titleLooksLikeNavigation(String title) {
        String cleaned = title == null ? "" : title.toLowerCase(Locale.ROOT).trim();
        if (cleaned.isBlank()) {
            return true;
        }
        return NEGATIVE_TERMS.stream().anyMatch(cleaned::contains)
                || cleaned.matches("^(next|previous|view all|search|filter|sort|home|apply|learn more|see next steps)$");
    }

    private boolean titleHasRoleSignal(String title) {
        String value = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return value.contains("engineer")
                || value.contains("developer")
                || value.contains("architect")
                || value.contains("analyst")
                || value.contains("manager")
                || value.contains("consultant")
                || value.contains("specialist")
                || value.contains("scientist")
                || value.contains("administrator")
                || value.contains("lead")
                || value.contains("software")
                || value.contains("backend")
                || value.contains("java")
                || value.contains("data")
                || value.contains("cloud")
                || value.contains("platform");
    }

    private boolean hasAny(String value, List<String> terms) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return terms.stream().anyMatch(text::contains);
    }

    private String dedupeKey(String href, String title, String location, String description) {
        String jobId = "";
        var matcher = JOB_ID.matcher(description == null ? "" : description);
        if (matcher.find()) {
            jobId = matcher.group().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        }
        String normalizedUrl = href == null ? "" : href.toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "").replaceAll("/+$", "");
        return (jobId + "|" + cleanText(title).toLowerCase(Locale.ROOT) + "|" + cleanText(location).toLowerCase(Locale.ROOT) + "|" + normalizedUrl).replaceAll("\\s+", " ");
    }

    private String normalizeTitle(String title, String href) {
        String cleaned = cleanText(title)
                .replace("Apply", "")
                .replace("View Job", "")
                .replace("Learn More", "")
                .trim();
        if (cleaned.isBlank()) {
            try {
                String path = URI.create(href).getPath();
                int slash = path.lastIndexOf('/');
                cleaned = slash >= 0 ? path.substring(slash + 1) : path;
                cleaned = cleaned.replace("-", " ").replace("_", " ");
            } catch (Exception ex) {
                cleaned = "";
            }
        }
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private String locationFrom(String description) {
        String lower = description.toLowerCase(Locale.ROOT);
        if (lower.contains("remote")) {
            return "Remote";
        }
        if (lower.contains("dallas")) {
            return "Dallas";
        }
        if (lower.contains("irving")) {
            return "Irving";
        }
        if (lower.contains("plano")) {
            return "Plano";
        }
        return "Unknown";
    }

    private String classifyAts(String url, String html) {
        String text = ((url == null ? "" : url) + " " + html).toLowerCase(Locale.ROOT);
        if (text.contains("greenhouse.io")) return "Greenhouse";
        if (text.contains("lever.co")) return "Lever";
        if (text.contains("myworkdayjobs")) return "Workday";
        if (text.contains("smartrecruiters")) return "SmartRecruiters";
        if (text.contains("successfactors")) return "SuccessFactors";
        if (text.contains("icims")) return "iCIMS";
        if (text.contains("oraclecloud") || text.contains("oracle taleo")) return "Oracle";
        return "Generic";
    }

    private String absoluteUrl(String baseUrl, String href) {
        if (href == null || href.isBlank() || href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("tel:")) {
            return "";
        }
        try {
            return URI.create(baseUrl).resolve(href).toString();
        } catch (Exception ex) {
            return href;
        }
    }

    private String cleanText(String value) {
        return decode(TAGS.matcher(value == null ? "" : value).replaceAll(" "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String decode(String value) {
        return value == null ? "" : value
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    public record ScanPage(String atsPlatform, List<DiscoveredJob> jobs) {
    }

    public record DiscoveredJob(String title, String location, String url, String description) {
    }

    private record JobQuality(int score, String reason) {
    }
}
