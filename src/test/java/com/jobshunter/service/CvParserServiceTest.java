package com.jobshunter.service;

import com.jobshunter.model.CvProfile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CvParserServiceTest {

    private final CvParserService service = new CvParserService();

    @Test
    void parseExtractsKeywords() throws Exception {
        Path tempPdf = Files.createTempFile("cv", ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Java developer with Spring Boot, REST APIs and cloud experience.");
                contentStream.endText();
            }
            document.save(tempPdf.toFile());
        }

        CvProfile profile = service.parse(tempPdf);
        assertThat(profile.keywords()).contains("java", "spring");
    }
}
