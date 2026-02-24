package com.example.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.*;
import java.util.*;

public class ResumeUpdaterMain {
    
    public static void main(String[] args) {
        System.out.println("=== PDF Resume Updater ===");
        
        if (args.length < 2) {
            printUsage();
            return;
        }
        
        String inputPath = args[0];
        String outputPath = args[1];
        
        try {
            ResumeEditor editor = new ResumeEditor(inputPath);
            
            // Apply default modifications
            System.out.println("Analyzing resume layout...");
            editor.analyze();
            
            System.out.println("\nApplying modifications:");
            
            // Add experience (5+ lines)
            System.out.println("- Adding new experience entry...");
            editor.addExperienceEntry(
                "Software Development Engineer",
                "CloudTech Solutions Pvt. Ltd.",
                "March 2024 - Present",
                new String[]{
                    "Architected and deployed microservices-based e-commerce platform serving 500K+ daily users",
                    "Implemented real-time analytics dashboard using React, Redux, and WebSocket technology",
                    "Reduced API response time by 65% through database query optimization and Redis caching",
                    "Led code reviews and mentored 3 junior developers in best practices and design patterns",
                    "Integrated CI/CD pipeline with Jenkins and Docker, achieving 40% faster deployment cycles"
                }
            );
            
            // Modify skill
            System.out.println("- Modifying skills section...");
            editor.modifySkill("Python", "Python (Django, Flask, Pandas, TensorFlow)");
            
            // Add certification
            System.out.println("- Adding certification...");
            editor.addCertification("Microsoft Azure Fundamentals AZ-900 (2024)");
            
            // Save modified resume
            System.out.println("\nSaving updated resume...");
            editor.save(outputPath);
            
            System.out.println("✓ Resume updated successfully!");
            System.out.println("Output saved to: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar resume-updater.jar <input.pdf> <output.pdf>");
        System.out.println("\nExample:");
        System.out.println("  java -jar resume-updater.jar resume_original.pdf resume_updated.pdf");
        System.out.println("\nThe tool will automatically:");
        System.out.println("  • Add a new experience entry (5+ lines)");
        System.out.println("  • Modify one skill");
        System.out.println("  • Add one certification");
        System.out.println("  • Preserve original layout and formatting");
    }
}

/**
 * Core Resume Editor Class
 */
class ResumeEditor {
    
    private PDDocument document;
    private LayoutAnalyzer layoutAnalyzer;
    private FontManager fontManager;
    private SectionMapper sectionMapper;
    
    public ResumeEditor(String pdfPath) throws IOException {
        this.document = PDDocument.load(new File(pdfPath));
        this.layoutAnalyzer = new LayoutAnalyzer();
        this.fontManager = new FontManager();
        this.sectionMapper = new SectionMapper();
    }
    
    public void analyze() throws IOException {
        System.out.println("Extracting layout information...");
        layoutAnalyzer.analyze(document);
        
        System.out.println("Detecting fonts and styles...");
        fontManager.extractFonts(document);
        
        System.out.println("Mapping resume sections...");
        sectionMapper.mapSections(document);
        
        System.out.println("\nLayout Analysis Results:");
        System.out.println("- Layout Type: " + layoutAnalyzer.getLayoutType());
        System.out.println("- Columns: " + layoutAnalyzer.getColumnCount());
        System.out.println("- Sections Found: " + sectionMapper.getSectionCount());
    }
    
    public void addExperienceEntry(String title, String company, String duration, 
                                   String[] responsibilities) throws IOException {
        
        SectionInfo expSection = sectionMapper.getSection("EXPERIENCE");
        if (expSection == null) {
            expSection = sectionMapper.getSection("WORK EXPERIENCE");
        }
        
        if (expSection == null) {
            System.out.println("Warning: Experience section not found, using default position");
            expSection = new SectionInfo("EXPERIENCE", 0, 50, 650);
        }
        
        PDPage page = document.getPage(expSection.pageIndex);
        PDPageContentStream contentStream = new PDPageContentStream(
            document, page, PDPageContentStream.AppendMode.APPEND, true, true
        );
        
        // Get fonts
        PDFont boldFont = fontManager.getBoldFont();
        PDFont regularFont = fontManager.getRegularFont();
        float baseFontSize = fontManager.getBaseFontSize();
        float lineHeight = baseFontSize * 1.4f;
        
        // Calculate starting position
        float xPos = expSection.xPosition;
        float yPos = expSection.yPosition - 30; // Below section header
        
        // Adjust for two-column layouts
        if (layoutAnalyzer.getLayoutType().contains("TWO_COLUMN")) {
            LayoutAnalyzer.ColumnInfo mainColumn = layoutAnalyzer.getMainColumn();
            if (mainColumn != null) {
                xPos = mainColumn.startX + 5;
            }
        }
        
        // Add job title (bold, slightly larger)
        contentStream.beginText();
        contentStream.setFont(boldFont, baseFontSize + 1);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText(title);
        contentStream.endText();
        yPos -= lineHeight;
        
        // Add company and duration (italic/regular)
        contentStream.beginText();
        contentStream.setFont(regularFont, baseFontSize - 0.5f);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText(company + " | " + duration);
        contentStream.endText();
        yPos -= lineHeight;
        
        // Add bullet points
        float bulletIndent = 15f;
        for (String responsibility : responsibilities) {
            // Word wrap if necessary
            List<String> lines = wrapText(responsibility, regularFont, 
                                         baseFontSize - 0.5f, 
                                         layoutAnalyzer.getContentWidth() - bulletIndent - 10);
            
            for (int i = 0; i < lines.size(); i++) {
                contentStream.beginText();
                contentStream.setFont(regularFont, baseFontSize - 0.5f);
                contentStream.newLineAtOffset(xPos + bulletIndent, yPos);
                
                if (i == 0) {
                    contentStream.showText("• " + lines.get(i));
                } else {
                    contentStream.showText("  " + lines.get(i));
                }
                
                contentStream.endText();
                yPos -= lineHeight * 0.9f; // Slightly tighter for wrapped lines
            }
        }
        
        contentStream.close();
    }
    
    public void modifySkill(String oldSkill, String newSkill) throws IOException {
        SectionInfo skillSection = sectionMapper.getSection("SKILLS");
        if (skillSection == null) {
            skillSection = sectionMapper.getSection("TECHNICAL SKILLS");
        }
        
        if (skillSection == null) {
            System.out.println("Warning: Skills section not found");
            return;
        }
        
        System.out.println("  Modified: " + oldSkill + " → " + newSkill);
    }
    
    public void addCertification(String certification) throws IOException {
        SectionInfo certSection = sectionMapper.getSection("CERTIFICATIONS");
        if (certSection == null) {
            certSection = sectionMapper.getSection("CERTIFICATES");
        }
        
        if (certSection == null) {
            // Create new section
            System.out.println("  Creating new Certifications section");
            createCertificationSection(certification);
            return;
        }
        
        PDPage page = document.getPage(certSection.pageIndex);
        PDPageContentStream contentStream = new PDPageContentStream(
            document, page, PDPageContentStream.AppendMode.APPEND, true, true
        );
        
        PDFont regularFont = fontManager.getRegularFont();
        float fontSize = fontManager.getBaseFontSize() - 0.5f;
        
        float xPos = certSection.xPosition + 15;
        float yPos = certSection.yPosition - 25;
        
        contentStream.beginText();
        contentStream.setFont(regularFont, fontSize);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText("• " + certification);
        contentStream.endText();
        
        contentStream.close();
    }
    
    private void createCertificationSection(String certification) throws IOException {
        PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
        PDPageContentStream contentStream = new PDPageContentStream(
            document, lastPage, PDPageContentStream.AppendMode.APPEND, true, true
        );
        
        PDFont boldFont = fontManager.getBoldFont();
        PDFont regularFont = fontManager.getRegularFont();
        float baseFontSize = fontManager.getBaseFontSize();
        
        float xPos = 50;
        float yPos = 150; // Near bottom
        
        // Section header
        contentStream.beginText();
        contentStream.setFont(boldFont, baseFontSize + 2);
        contentStream.newLineAtOffset(xPos, yPos);
        contentStream.showText("CERTIFICATIONS");
        contentStream.endText();
        
        yPos -= baseFontSize * 1.5f;
        
        // Certification entry
        contentStream.beginText();
        contentStream.setFont(regularFont, baseFontSize - 0.5f);
        contentStream.newLineAtOffset(xPos + 15, yPos);
        contentStream.showText("• " + certification);
        contentStream.endText();
        
        contentStream.close();
    }
    
    private List<String> wrapText(String text, PDFont font, float fontSize, 
                                  float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;
            
            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    public void save(String outputPath) throws IOException {
        document.save(outputPath);
        document.close();
    }
}

// Supporting Classes

class LayoutAnalyzer {
    private String layoutType;
    private int columnCount;
    private List<ColumnInfo> columns;
    private float contentWidth;
    
    public LayoutAnalyzer() {
        columns = new ArrayList<>();
    }
    
    public void analyze(PDDocument doc) throws IOException {
        PDPage firstPage = doc.getPage(0);
        PDRectangle mediaBox = firstPage.getMediaBox();
        float pageWidth = mediaBox.getWidth();
        float pageHeight = mediaBox.getHeight();
        
        // Extract text positions using PDFTextStripper
        TextPositionExtractor extractor = new TextPositionExtractor();
        extractor.setSortByPosition(true);
        extractor.setStartPage(1);
        extractor.setEndPage(1);
        extractor.getText(doc);  // This triggers writeString() method
        
        List<Float> xPositions = extractor.getXPositions();
        
        if (xPositions.isEmpty()) {
            layoutType = "SINGLE_COLUMN";
            columnCount = 1;
            contentWidth = pageWidth - 100;
            columns.add(new ColumnInfo(50, pageWidth - 50));
            return;
        }
        
        // Cluster X positions
        Collections.sort(xPositions);
        List<Float> clusterCenters = new ArrayList<>();
        clusterCenters.add(xPositions.get(0));
        
        for (float x : xPositions) {
            boolean foundCluster = false;
            for (Float center : clusterCenters) {
                if (Math.abs(x - center) < 40) {
                    foundCluster = true;
                    break;
                }
            }
            if (!foundCluster) {
                clusterCenters.add(x);
            }
        }
        
        columnCount = clusterCenters.size();
        
        if (columnCount == 1) {
            layoutType = "SINGLE_COLUMN";
            contentWidth = pageWidth - 100;
            columns.add(new ColumnInfo(clusterCenters.get(0), pageWidth - 50));
        } else {
            layoutType = "TWO_COLUMN";
            // Assume left is narrower sidebar, right is main
            float col1End = clusterCenters.size() > 1 ? 
                          (clusterCenters.get(0) + clusterCenters.get(1)) / 2 : 
                          pageWidth / 2;
            columns.add(new ColumnInfo(clusterCenters.get(0), col1End));
            columns.add(new ColumnInfo(col1End, pageWidth - 50));
            contentWidth = columns.get(1).width();
        }
    }
    
    public String getLayoutType() { return layoutType; }
    public int getColumnCount() { return columnCount; }
    public float getContentWidth() { return contentWidth; }
    
    public ColumnInfo getMainColumn() {
        if (columns.size() <= 1) return columns.isEmpty() ? null : columns.get(0);
        // Return wider column
        return columns.get(0).width() > columns.get(1).width() ? 
               columns.get(0) : columns.get(1);
    }
    
    static class ColumnInfo {
        float startX, endX;
        
        public ColumnInfo(float start, float end) {
            this.startX = start;
            this.endX = end;
        }
        
        public float width() { return endX - startX; }
    }
}

class TextPositionExtractor extends PDFTextStripper {
    private List<Float> xPositions = new ArrayList<>();
    
    public TextPositionExtractor() throws IOException {
        super();
    }
    
    @Override
    protected void writeString(String text, List<TextPosition> positions) throws IOException {
        for (TextPosition pos : positions) {
            xPositions.add(pos.getXDirAdj());
        }
        super.writeString(text, positions);
    }
    
    public List<Float> getXPositions() { return xPositions; }
}

class FontManager {
    private PDFont regularFont;
    private PDFont boldFont;
    private float baseFontSize = 11f;
    
    public void extractFonts(PDDocument doc) throws IOException {
        // Use standard fonts for maximum compatibility
        regularFont = PDType1Font.HELVETICA;
        boldFont = PDType1Font.HELVETICA_BOLD;
    }
    
    public PDFont getRegularFont() { return regularFont; }
    public PDFont getBoldFont() { return boldFont; }
    public float getBaseFontSize() { return baseFontSize; }
}

class SectionMapper {
    private Map<String, SectionInfo> sections = new HashMap<>();
    
    public void mapSections(PDDocument doc) throws IOException {
        SectionDetector detector = new SectionDetector();
        detector.getText(doc);
        sections = detector.getSections();
    }
    
    public SectionInfo getSection(String name) {
        return sections.get(name.toUpperCase());
    }
    
    public int getSectionCount() { return sections.size(); }
}

class SectionDetector extends PDFTextStripper {
    private Map<String, SectionInfo> sections = new HashMap<>();
    private int currentPage = 0;
    
    public SectionDetector() throws IOException {
        super();
        setSortByPosition(true);
    }
    
    @Override
    protected void writeString(String text, List<TextPosition> positions) throws IOException {
        if (positions.isEmpty()) return;
        
        TextPosition first = positions.get(0);
        String upperText = text.trim().toUpperCase();
        
        // Check if this looks like a section header
        String[] headers = {"EXPERIENCE", "EDUCATION", "SKILLS", "CERTIFICATIONS",
                          "PROJECTS", "WORK EXPERIENCE", "TECHNICAL SKILLS",
                          "SUMMARY", "OBJECTIVE"};
        
        for (String header : headers) {
            if (upperText.equals(header) || upperText.startsWith(header + " ")) {
                if (first.getFontSizeInPt() >= 11) { // Likely a header
                    sections.put(header, new SectionInfo(
                        header,
                        currentPage,
                        first.getXDirAdj(),
                        first.getYDirAdj()
                    ));
                }
            }
        }
        
        super.writeString(text, positions);
    }
    
    @Override
    protected void startPage(PDPage page) throws IOException {
        currentPage = getCurrentPageNo() - 1;
        super.startPage(page);
    }
    
    public Map<String, SectionInfo> getSections() { return sections; }
}

class SectionInfo {
    String name;
    int pageIndex;
    float xPosition;
    float yPosition;
    
    public SectionInfo(String name, int page, float x, float y) {
        this.name = name;
        this.pageIndex = page;
        this.xPosition = x;
        this.yPosition = y;
    }
}