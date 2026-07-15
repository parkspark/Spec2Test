package com.example.gameqacopilot.document.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

@Component
public class PdfDocumentProcessor {
    private final ObjectMapper objectMapper;

    public PdfDocumentProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProcessedPdf process(byte[] content, Path documentDirectory) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            Files.createDirectories(documentDirectory);
            var stripper = new PositionedTextStripper(pdf);
            String extractedText = stripper.getText(pdf);
            var renderer = new PDFRenderer(pdf);
            for (int pageIndex = 0; pageIndex < pdf.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 144);
                if (!ImageIO.write(image, "png", documentDirectory.resolve("page-" + (pageIndex + 1) + ".png").toFile())) {
                    throw new IOException("PNG image writer is unavailable");
                }
            }
            return new ProcessedPdf(extractedText, toJson(stripper.pages()));
        }
    }

    private String toJson(List<PageContent> pages) throws JsonProcessingException {
        return objectMapper.writeValueAsString(pages);
    }

    public record ProcessedPdf(String extractedText, String pageContents) {}
    public record PageContent(int pageNumber, List<PageElement> elements) {}
    public record PageElement(String elementId, String elementType, String text, BoundingBox boundingBox) {}
    public record BoundingBox(double x, double y, double width, double height) {}

    private static final class PositionedTextStripper extends PDFTextStripper {
        private final PDDocument document;
        private final List<List<PageElement>> elementsByPage;
        private int pageIndex;

        private PositionedTextStripper(PDDocument document) throws IOException {
            this.document = document;
            this.elementsByPage = new ArrayList<>();
            for (int i = 0; i < document.getNumberOfPages(); i++) elementsByPage.add(new ArrayList<>());
            setSortByPosition(true);
        }

        @Override
        protected void startPage(PDPage page) throws IOException {
            pageIndex = getCurrentPageNo() - 1;
            super.startPage(page);
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) throws IOException {
            String value = text.strip();
            if (!value.isEmpty() && !positions.isEmpty()) {
                PDPage page = document.getPage(pageIndex);
                double pageWidth = page.getCropBox().getWidth();
                double pageHeight = page.getCropBox().getHeight();
                double minX = positions.stream().mapToDouble(TextPosition::getXDirAdj).min().orElse(0);
                double minY = positions.stream().mapToDouble(TextPosition::getYDirAdj).min().orElse(0);
                double maxX = positions.stream().mapToDouble(p -> p.getXDirAdj() + p.getWidthDirAdj()).max().orElse(minX);
                double maxY = positions.stream().mapToDouble(p -> p.getYDirAdj() + p.getHeightDir()).max().orElse(minY);
                var elements = elementsByPage.get(pageIndex);
                elements.add(new PageElement(
                        "PAGE-%d-TEXT-%02d".formatted(pageIndex + 1, elements.size() + 1), "TEXT", value,
                        new BoundingBox(ratio(minX, pageWidth), ratio(minY, pageHeight),
                                ratio(maxX - minX, pageWidth), ratio(maxY - minY, pageHeight))));
            }
            super.writeString(text, positions);
        }

        private List<PageContent> pages() {
            var pages = new ArrayList<PageContent>();
            for (int i = 0; i < elementsByPage.size(); i++) pages.add(new PageContent(i + 1, elementsByPage.get(i)));
            return pages;
        }

        private static double ratio(double value, double total) {
            return Math.max(0, Math.min(1, value / total));
        }
    }
}
