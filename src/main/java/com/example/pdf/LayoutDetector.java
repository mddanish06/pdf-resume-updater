package com.example.pdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.*;
import java.io.IOException;
import java.util.*;

public class LayoutDetector {
    
    private PDDocument document;
    private LayoutType layoutType;
    private List<ColumnInfo> columns;
    private Map<String, Region> sectionRegions;
    
    public enum LayoutType {
        SINGLE_COLUMN,
        TWO_COLUMN_LEFT_MAIN,
        TWO_COLUMN_RIGHT_MAIN,
        THREE_COLUMN,
        COMPLEX_GRID
    }
    
    public LayoutDetector(PDDocument doc) {
        this.document = doc;
        this.columns = new ArrayList<>();
        this.sectionRegions = new HashMap<>();
    }
    
    /**
     * Analyze the PDF layout and detect structure
     */
    public void analyze() throws IOException {
        PDPage firstPage = document.getPage(0);
        float pageWidth = firstPage.getMediaBox().getWidth();
        
        // Extract text positions
        LayoutTextStripper stripper = new LayoutTextStripper();
        stripper.setSortByPosition(true);
        stripper.getText(document);
        
        List<TextBlock> blocks = stripper.getTextBlocks();
        
        // Detect columns by clustering X positions
        detectColumns(blocks, pageWidth);
        
        // Identify layout type
        identifyLayoutType(pageWidth);
        
        // Map sections to regions
        mapSectionRegions(blocks);
        
        System.out.println("Detected layout: " + layoutType);
        System.out.println("Number of columns: " + columns.size());
    }
    
    private void detectColumns(List<TextBlock> blocks, float pageWidth) {
        // Cluster text blocks by X position
        List<Float> xPositions = new ArrayList<>();
        for (TextBlock block : blocks) {
            xPositions.add(block.x);
        }
        
        Collections.sort(xPositions);
        
        // Find column boundaries using gap detection
        List<Float> columnStarts = new ArrayList<>();
        columnStarts.add(xPositions.get(0));
        
        for (int i = 1; i < xPositions.size(); i++) {
            float gap = xPositions.get(i) - xPositions.get(i - 1);
            if (gap > 50) { // Significant gap indicates new column
                columnStarts.add(xPositions.get(i));
            }
        }
        
        // Create column info
        for (int i = 0; i < columnStarts.size(); i++) {
            float start = columnStarts.get(i);
            float end = (i < columnStarts.size() - 1) ? 
                       columnStarts.get(i + 1) - 10 : pageWidth - 50;
            
            columns.add(new ColumnInfo(i, start, end));
        }
    }
    
    private void identifyLayoutType(float pageWidth) {
        if (columns.size() == 1) {
            layoutType = LayoutType.SINGLE_COLUMN;
        } else if (columns.size() == 2) {
            // Determine which column is main
            float leftWidth = columns.get(0).width();
            float rightWidth = columns.get(1).width();
            
            if (leftWidth > rightWidth * 1.5) {
                layoutType = LayoutType.TWO_COLUMN_LEFT_MAIN;
            } else if (rightWidth > leftWidth * 1.5) {
                layoutType = LayoutType.TWO_COLUMN_RIGHT_MAIN;
            } else {
                layoutType = LayoutType.TWO_COLUMN_LEFT_MAIN;
            }
        } else if (columns.size() == 3) {
            layoutType = LayoutType.THREE_COLUMN;
        } else {
            layoutType = LayoutType.COMPLEX_GRID;
        }
    }
    
    private void mapSectionRegions(List<TextBlock> blocks) {
        String[] sectionKeywords = {
            "EXPERIENCE", "EDUCATION", "SKILLS", "CERTIFICATIONS",
            "PROJECTS", "WORK EXPERIENCE", "TECHNICAL SKILLS",
            "SUMMARY", "OBJECTIVE", "ACHIEVEMENTS"
        };
        
        for (TextBlock block : blocks) {
            String text = block.text.toUpperCase().trim();
            
            for (String keyword : sectionKeywords) {
                if (text.contains(keyword) && block.fontSize > 11) {
                    // Found section header
                    Region region = new Region(
                        block.x,
                        block.y,
                        determineColumnIndex(block.x),
                        block.pageIndex
                    );
                    sectionRegions.put(keyword, region);
                    break;
                }
            }
        }
    }
    
    private int determineColumnIndex(float x) {
        for (int i = 0; i < columns.size(); i++) {
            if (x >= columns.get(i).startX && x <= columns.get(i).endX) {
                return i;
            }
        }
        return 0; // Default to first column
    }
    
    /**
     * Get optimal insertion point for new content
     */
    public InsertionPoint getInsertionPoint(String sectionName) {
        Region region = sectionRegions.get(sectionName.toUpperCase());
        if (region == null) {
            // Return default position
            return new InsertionPoint(50, 700, 0, 0);
        }
        
        int columnIndex = region.columnIndex;
        ColumnInfo column = columns.get(columnIndex);
        
        return new InsertionPoint(
            column.startX + 10,
            region.y - 20,
            columnIndex,
            region.pageIndex
        );
    }
    
    // Getters
    public LayoutType getLayoutType() { return layoutType; }
    public List<ColumnInfo> getColumns() { return columns; }
    public Map<String, Region> getSectionRegions() { return sectionRegions; }
    
    // Helper classes 
    public static class ColumnInfo {
        int index;
        float startX;
        float endX;
        
        public ColumnInfo(int idx, float start, float end) {
            this.index = idx;
            this.startX = start;
            this.endX = end;
        }
        
        public float width() {
            return endX - startX;
        }
        
        @Override
        public String toString() {
            return String.format("Column %d: %.1f - %.1f (width: %.1f)", 
                               index, startX, endX, width());
        }
    }
    
    public static class Region {
        float x, y;
        int columnIndex;
        int pageIndex;
        
        public Region(float x, float y, int col, int page) {
            this.x = x;
            this.y = y;
            this.columnIndex = col;
            this.pageIndex = page;
        }
    }
    
    public static class InsertionPoint {
        public float x, y;
        public int columnIndex;
        public int pageIndex;
        
        public InsertionPoint(float x, float y, int col, int page) {
            this.x = x;
            this.y = y;
            this.columnIndex = col;
            this.pageIndex = page;
        }
    }
    
    public static class TextBlock {
        String text;
        float x, y;
        float fontSize;
        int pageIndex;
        
        public TextBlock(String txt, float x, float y, float size, int page) {
            this.text = txt;
            this.x = x;
            this.y = y;
            this.fontSize = size;
            this.pageIndex = page;
        }
    }
    
    // Custom stripper to extract text blocks
    class LayoutTextStripper extends PDFTextStripper {
        private List<TextBlock> textBlocks = new ArrayList<>();
        private int currentPage = 0;
        
        public LayoutTextStripper() throws IOException {
            super();
        }
        
        @Override
        protected void writeString(String text, List<TextPosition> positions) 
                throws IOException {
            if (!positions.isEmpty()) {
                TextPosition first = positions.get(0);
                textBlocks.add(new TextBlock(
                    text.trim(),
                    first.getXDirAdj(),
                    first.getYDirAdj(),
                    first.getFontSizeInPt(),
                    currentPage
                ));
            }
            super.writeString(text, positions);
        }
        
        @Override
        protected void startPage(PDPage page) throws IOException {
            currentPage = getCurrentPageNo() - 1;
            super.startPage(page);
        }
        
        public List<TextBlock> getTextBlocks() {
            return textBlocks;
        }
    }
}
