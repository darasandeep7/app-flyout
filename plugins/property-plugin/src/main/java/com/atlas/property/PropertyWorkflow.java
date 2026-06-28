package com.atlas.property;

import com.atlas.ai.ChatRequest;
import com.atlas.ai.ModelProvider;
import com.atlas.browser.BrowserAutomation;
import com.atlas.browser.BrowserRequest;
import com.atlas.memory.MemoryService;
import com.atlas.storage.ProjectRecord;
import com.atlas.storage.ProjectStorage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class PropertyWorkflow {
    private final BrowserAutomation browserAutomation;
    private final ModelProvider modelProvider;
    private final ProjectStorage storage;
    private final MemoryService memory;

    public PropertyWorkflow(BrowserAutomation browserAutomation, ModelProvider modelProvider, ProjectStorage storage, MemoryService memory) {
        this.browserAutomation = browserAutomation;
        this.modelProvider = modelProvider;
        this.storage = storage;
        this.memory = memory;
    }

    public PropertyAnalysisResult analyze(PropertyAnalysisRequest request) {
        ProjectRecord project = storage.createProject("property", "Property listing");
        var browser = browserAutomation.inspect(new BrowserRequest(request.url(), project.folder(), true, true));
        String source = browser.visibleText() == null || browser.visibleText().isBlank()
                ? "Public listing URL: " + request.url()
                : browser.visibleText();
        String prompt = """
                You are a real estate marketing strategist. Using the public listing information below, produce:
                1. improved title
                2. improved listing description
                3. short marketing summary
                4. listing score 1-100
                5. photo score 1-100
                6. improvement suggestions
                7. cinematic video storyboard with six shots

                Listing:
                %s
                """.formatted(source);
        var response = modelProvider.generate(new ChatRequest(request.model(), prompt, Map.of("temperature", 0.4)));
        Path pdfPath = storage.resolve(project, "reports/brochure.pdf");
        Path packagePath = storage.resolve(project, "package/property-project.zip");
        PropertyAnalysisResult result = new PropertyAnalysisResult(
                project,
                browser,
                fallbackTitle(browser.title()),
                response.text(),
                "Generated locally from the captured public listing information.",
                browser.fallback() ? 50 : 78,
                browser.images().isEmpty() ? 45 : 82,
                List.of("Verify extracted facts before publishing.", "Add brighter hero photography if available.", "Highlight neighborhood and lifestyle details."),
                List.of("Exterior arrival shot", "Entry reveal", "Kitchen movement", "Primary suite detail", "Outdoor living moment", "Final twilight pullback"),
                pdfPath.toString(),
                packagePath.toString()
        );
        storage.writeJson(project, "json/browser-result.json", browser);
        storage.writeJson(project, "json/analysis.json", result);
        storage.writeText(project, "reports/brochure.md", brochure(result));
        writeSimplePdf(pdfPath, result);
        zipProject(project.folder(), packagePath);
        memory.remember(project, "url", request.url(), Map.of("plugin", "property"));
        return result;
    }

    private String fallbackTitle(String browserTitle) {
        if (browserTitle != null && !browserTitle.isBlank()) {
            return browserTitle;
        }
        return "Elevated Property Listing";
    }

    private String brochure(PropertyAnalysisResult result) {
        return """
                # %s

                %s

                ## Marketing Summary

                %s

                ## Scores

                Listing score: %d
                Photo score: %d
                """.formatted(result.improvedTitle(), result.improvedDescription(), result.marketingSummary(), result.listingScore(), result.photoScore());
    }

    private void writeSimplePdf(Path path, PropertyAnalysisResult result) {
        try {
            Files.createDirectories(path.getParent());
            String text = (result.improvedTitle() + "\n\n" + result.marketingSummary() + "\n\nListing score: " + result.listingScore() + "\nPhoto score: " + result.photoScore())
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("\n", ") Tj T* (");
            String stream = "BT /F1 14 Tf 72 740 Td (" + text + ") Tj ET";
            List<String> objects = List.of(
                    "<< /Type /Catalog /Pages 2 0 R >>",
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                    "<< /Length " + stream.getBytes(StandardCharsets.UTF_8).length + " >> stream\n" + stream + "\nendstream"
            );
            Files.write(path, buildPdf(objects));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write brochure PDF", ex);
        }
    }

    private byte[] buildPdf(List<String> objects) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xref = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(out, "%010d 00000 n \n".formatted(offset));
        }
        write(out, "trailer << /Root 1 0 R /Size " + (objects.size() + 1) + " >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private void write(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private void zipProject(Path folder, Path packagePath) {
        try {
            Files.createDirectories(packagePath.getParent());
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(packagePath));
                 var stream = Files.walk(folder)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> !path.equals(packagePath))
                        .forEach(path -> addZipEntry(folder, zip, path));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not package project", ex);
        }
    }

    private void addZipEntry(Path folder, ZipOutputStream zip, Path path) {
        try {
            zip.putNextEntry(new ZipEntry(folder.relativize(path).toString()));
            Files.copy(path, zip);
            zip.closeEntry();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not add " + path + " to package", ex);
        }
    }
}
