package com.atlas.career.service;

import com.atlas.career.domain.CompanyRecord;
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
import org.springframework.stereotype.Service;

@Service
public class JobDiscoveryService {
    private static final Pattern ANCHOR = Pattern.compile("<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ScanPage scan(CompanyRecord company) {
        String html = fetch(company.careerUrl());
        String ats = classifyAts(company.careerUrl(), html);
        Map<String, DiscoveredJob> jobs = new LinkedHashMap<>();
        var matcher = ANCHOR.matcher(html);
        while (matcher.find() && jobs.size() < 40) {
            String href = absoluteUrl(company.careerUrl(), decode(matcher.group(1)));
            String title = cleanText(matcher.group(2));
            if (looksLikeJobLink(href, title)) {
                String normalizedTitle = normalizeTitle(title, href);
                if (!normalizedTitle.isBlank()) {
                    String description = normalizeDescription(fetchQuietly(href), normalizedTitle);
                    jobs.putIfAbsent(href, new DiscoveredJob(normalizedTitle, locationFrom(description), href, description));
                }
            }
        }
        return new ScanPage(ats, new ArrayList<>(jobs.values()));
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
        if (title.length() > 120 || title.length() < 4) {
            return false;
        }
        return value.contains("job")
                || value.contains("career")
                || value.contains("position")
                || value.contains("opening")
                || value.contains("lever.co")
                || value.contains("greenhouse.io")
                || value.contains("myworkdayjobs")
                || value.contains("smartrecruiters")
                || value.contains("icims")
                || value.contains("successfactors")
                || value.contains("oraclecloud");
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
}
