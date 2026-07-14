package com.example.gameqacopilot.document.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfDocumentProcessorTest {
    @TempDir Path directory;

    @Test
    void extractsTextCoordinatesAndPageImage() throws Exception {
        var processor = new PdfDocumentProcessor(new ObjectMapper());

        var result = processor.process(pdfWithText("Login button"), directory);
        var page = new ObjectMapper().readTree(result.pageContents()).get(0);
        var element = page.get("elements").get(0);

        assertThat(result.extractedText()).contains("Login button");
        assertThat(page.get("pageNumber").asInt()).isEqualTo(1);
        assertThat(element.get("elementId").asText()).isEqualTo("PAGE-1-TEXT-01");
        assertThat(element.get("boundingBox").get("x").asDouble()).isBetween(0.0, 1.0);
        assertThat(element.get("boundingBox").get("y").asDouble()).isBetween(0.0, 1.0);
        assertThat(Files.isRegularFile(directory.resolve("page-1.png"))).isTrue();
    }

    private byte[] pdfWithText(String text) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);
            try (var stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 720);
                stream.showText(text);
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
