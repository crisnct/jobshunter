package com.jobshunter.service;

import com.jobshunter.model.CvProfile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CvParserService {

    private static final Logger log = LoggerFactory.getLogger(CvParserService.class);

    public CvProfile parse(Path pdfPath) {
        if (!Files.exists(pdfPath)) {
            throw new IllegalArgumentException("CV file not found: " + pdfPath);
        }

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            String text = new PDFTextStripper().getText(document);
            Set<String> keywords = extractKeywords(text);
            log.info("Extracted {} keywords from CV", keywords.size());
            return new CvProfile(pdfPath, text, keywords);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse CV file", e);
        }
    }

    private Set<String> extractKeywords(String text) {
        Map<String, Long> counts = Arrays.stream(text.split("[^A-Za-z]+"))
                .map(String::toLowerCase)
                .filter(word -> word.length() > 3)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(25)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
