package com.transproj.service;

import com.transproj.domain.Segment;
import com.transproj.domain.TranslationJob;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportService {

    public ResponseEntity<byte[]> export(TranslationJob job, List<Segment> segments, String format) {
        if (!"md".equalsIgnoreCase(format) && !"docx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().build();
        }
        if ("md".equalsIgnoreCase(format)) {
            String md = buildMarkdown(segments);
            byte[] bytes = md.getBytes(StandardCharsets.UTF_8);
            String filename = safeFileBase(job) + ".md";
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                    .body(bytes);
        }
        byte[] docx = buildDocx(segments);
        String filename = safeFileBase(job) + ".docx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docx);
    }

    /**
     * Keeps Unicode letters (e.g. CJK) and most safe punctuation; strips only path / OS-forbidden characters.
     * Historic behavior used {@code [^a-zA-Z0-9._-]}, which turned Chinese filenames into long underscore runs.
     */
    private static String safeFileBase(TranslationJob job) {
        String name = job.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "translation";
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.trim();
        if (name.isBlank()) {
            return "translation";
        }
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (name.isBlank()) {
            return "translation";
        }
        return name;
    }

    private static String buildMarkdown(List<Segment> segments) {
        StringBuilder sb = new StringBuilder();
        for (Segment s : segments) {
            sb.append("## Segment ").append(s.index()).append("\n\n");
            sb.append("**Source**\n\n");
            sb.append(s.source().trim()).append("\n\n");
            sb.append("**Translation**\n\n");
            sb.append(s.target().trim()).append("\n\n");
        }
        return sb.toString();
    }

    private static byte[] buildDocx(List<Segment> segments) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Segment s : segments) {
                XWPFParagraph p1 = doc.createParagraph();
                p1.createRun().setText("Segment " + s.index() + " — Source");
                XWPFParagraph p2 = doc.createParagraph();
                p2.createRun().setText(s.source());
                XWPFParagraph p3 = doc.createParagraph();
                p3.createRun().setText("Translation");
                XWPFParagraph p4 = doc.createParagraph();
                p4.createRun().setText(s.target());
                doc.createParagraph();
            }
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("DOCX export failed", e);
        }
    }
}
