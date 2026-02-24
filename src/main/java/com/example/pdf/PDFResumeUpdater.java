package com.example.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.io.*;
import java.util.*;

/**
 * Automated PDF Resume Updater
 * Preserves layout while adding/modifying content
 * 
 * @author Fresh B.Tech CSE Graduate
 */
public class PDFResumeUpdater {

    private PDDocument document;
    private Map<String, SectionInfo> sections;
    private FontInfo defaultFont;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java PDFResumeUpdater <input.pdf> <output.pdf>");
            return;
        }

        try {
            PDFResumeUpdater updater = new PDFResumeUpdater();
            updater.loadPDF(args[0]);

            // Example modifications
            updater.addExperience(
                    "Senior Software Engineer",
                    "Tech Innovations Inc.",
                    "Jan 2024 - Present",
                    new String[] {
                            "Led development of microservices architecture serving 1M+ users",
                            "Implemented CI/CD pipeline reducing deployment time by 60%",
                            "Mentored team of 5 junior developers in clean code practices",
                            "Optimized database queries improving response time by 40%",
                            "Designed RESTful APIs using Spring Boot and PostgreSQL"
                    });

            updater.modifySkill("JavaScript", "JavaScript (React, Node.js, TypeScript)");
            updater.addCertification("AWS Certified Solutions Architect - Associate (2024)");

            updater.savePDF(args[1]);
            System.out.println("Resume updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPDF(String inputPath) throws IOException {
        document = PDDocument.load(new File(inputPath));
        sections = new HashMap<>();
        analyzePDFStructure();
    }

    private void analyzePDFStructure() throws IOException {
        CustomTextStripper stripper = new CustomTextStripper();
        stripper.setSortByPosition(true);

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            stripper.getText(document);
        }

        sections = stripper.getSections();
        defaultFont = stripper.getDefaultFont();
    }

    public void addExperience(String title, String company, String duration, String[] bullets)
            throws IOException {

        SectionInfo expSection = sections.get("EXPERIENCE");
        if (expSection == null) {
            expSection = sections.get("WORK EXPERIENCE");
        }

        if (expSection == null) {
            System.out.println("Warning: Experience section not found");
            return;
        }

        PDPage page = document.getPage(expSection.pageIndex);
        PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true);

        // Calculate insertion point
        float yPos = expSection.yPosition - 20; // Start below section header
        float xPos = expSection.xPosition;
        float lineHeight = defaultFont.size * 1.2f;

        // Add job title
        contentStream.beginText();
        contentStream.setFont(defaultFont.bold, defaultFont.size);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText(title);
        contentStream.endText();
        yPos -= lineHeight;

        // Add company and duration
        contentStream.beginText();
        contentStream.setFont(defaultFont.regular, defaultFont.size - 1);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText(company + " | " + duration);
        contentStream.endText();
        yPos -= lineHeight;

        // Add bullet points
        for (String bullet : bullets) {
            contentStream.beginText();
            contentStream.setFont(defaultFont.regular, defaultFont.size - 1);
            contentStream.newLineAtOffset(xPos + 10, yPos);
            contentStream.showText("• " + bullet);
            contentStream.endText();
            yPos -= lineHeight;
        }

        contentStream.close();

        // Shift content below
        shiftContentBelow(expSection.pageIndex, yPos,
                Math.abs(yPos - expSection.yPosition) + 10);
    }

    public void modifySkill(String oldSkill, String newSkill) throws IOException {
        SectionInfo skillSection = sections.get("SKILLS");
        if (skillSection == null) {
            skillSection = sections.get("TECHNICAL SKILLS");
        }

        if (skillSection == null) {
            System.out.println("Warning: Skills section not found");
            return;
        }

        // Find and replace skill text
        PDPage page = document.getPage(skillSection.pageIndex);
        ContentReplacer replacer = new ContentReplacer(document, page);
        replacer.replaceText(oldSkill, newSkill, skillSection.yPosition);
    }

    public void addCertification(String certification) throws IOException {
        SectionInfo certSection = sections.get("CERTIFICATIONS");
        if (certSection == null) {
            certSection = sections.get("CERTIFICATES");
        }

        if (certSection == null) {
            // Create new section
            createCertificationSection(certification);
            return;
        }

        PDPage page = document.getPage(certSection.pageIndex);
        PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true);

        float yPos = certSection.yPosition - 30;
        float xPos = certSection.xPosition;

        contentStream.beginText();
        contentStream.setFont(defaultFont.regular, defaultFont.size - 1);
        contentStream.newLineAtOffset(xPos + 10, yPos);
        contentStream.showText("• " + certification);
        contentStream.endText();

        contentStream.close();
    }

    private void createCertificationSection(String certification) throws IOException {
        // Add new section at bottom of first page
        PDPage page = document.getPage(0);
        PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true);

        float yPos = 100; // Bottom margin
        float xPos = 50; // Left margin

        // Section header
        contentStream.beginText();
        contentStream.setFont(defaultFont.bold, defaultFont.size + 2);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText("CERTIFICATIONS");
        contentStream.endText();

        yPos -= defaultFont.size * 1.5f;

        // Certification entry
        contentStream.beginText();
        contentStream.setFont(defaultFont.regular, defaultFont.size - 1);
        contentStream.newLineAtOffset(xPos + 10, yPos);
        contentStream.showText("• " + certification);
        contentStream.endText();

        contentStream.close();
    }

    private void shiftContentBelow(int pageIndex, float fromY, float shiftAmount) {
        // This would use PDFBox's content stream manipulation
        // to shift existing content down to make room for new content
        // Complex implementation - placeholder for concept
    }

    public void savePDF(String outputPath) throws IOException {
        document.save(outputPath);
        document.close();
    }

    // Helper classes

    static class SectionInfo {
        int pageIndex;
        float xPosition;
        float yPosition;
        float width;
        String sectionName;

        public SectionInfo(int page, float x, float y, String name) {
            this.pageIndex = page;
            this.xPosition = x;
            this.yPosition = y;
            this.sectionName = name;
        }
    }

    static class FontInfo {
        PDFont regular;
        PDFont bold;
        float size;

        public FontInfo(PDFont reg, PDFont b, float s) {
            regular = reg;
            bold = b;
            size = s;
        }
    }

    class CustomTextStripper extends PDFTextStripper {
        private Map<String, SectionInfo> detectedSections = new HashMap<>();
        private FontInfo fontInfo;
        private int currentPage = 0;

        public CustomTextStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions)
                throws IOException {

            if (positions.isEmpty())
                return;

            TextPosition first = positions.get(0);
            String upperText = text.trim().toUpperCase();

            // Detect section headers
            if (isSectionHeader(upperText, first)) {
                detectedSections.put(upperText, new SectionInfo(
                        currentPage,
                        first.getXDirAdj(),
                        first.getYDirAdj(),
                        upperText));
            }

            // Capture font info
            if (fontInfo == null && first.getFont() != null) {
                fontInfo = new FontInfo(
                        first.getFont(),
                        first.getFont(), // Assume same for now
                        first.getFontSizeInPt());
            }

            super.writeString(text, positions);
        }

        private boolean isSectionHeader(String text, TextPosition pos) {
            String[] headers = { "EXPERIENCE", "EDUCATION", "SKILLS",
                    "CERTIFICATIONS", "PROJECTS", "WORK EXPERIENCE",
                    "TECHNICAL SKILLS" };

            for (String header : headers) {
                if (text.contains(header) && pos.getFontSizeInPt() > 11) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void startPage(PDPage page) throws IOException {
            currentPage = getCurrentPageNo() - 1;
            super.startPage(page);
        }

        public Map<String, SectionInfo> getSections() {
            return detectedSections;
        }

        public FontInfo getDefaultFont() {
            return fontInfo != null ? fontInfo
                    : new FontInfo(PDType1Font.HELVETICA,
                            PDType1Font.HELVETICA_BOLD, 11);
        }
    }

    class ContentReplacer {
        private PDDocument doc;
        private PDPage page;

        public ContentReplacer(PDDocument d, PDPage p) {
            doc = d;
            page = p;
        }

        public void replaceText(String oldText, String newText, float nearY)
                throws IOException {
            // Use PDFBox's content stream editing to replace text
            // This is a simplified placeholder
            System.out.println("Replacing: " + oldText + " -> " + newText);
        }
    }
}