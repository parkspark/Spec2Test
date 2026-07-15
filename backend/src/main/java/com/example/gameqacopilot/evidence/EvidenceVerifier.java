package com.example.gameqacopilot.evidence;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.Evidence;
import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.EvidenceType;
import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.VerificationStatus;
import com.example.gameqacopilot.document.parser.PdfDocumentProcessor.PageContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public final class EvidenceVerifier {
    private static final double SIMILARITY_THRESHOLD = 0.7;
    private final ObjectMapper objectMapper;

    public EvidenceVerifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Evidence> verify(List<Evidence> evidences, String pageContents, int pageCount) {
        var pages = pages(pageContents);
        return evidences.stream().map(evidence -> {
            if (evidence.pageNumber() < 1 || evidence.pageNumber() > pageCount) {
                throw new IllegalArgumentException("Evidence page is outside document range");
            }
            String pageText = pages.stream()
                    .filter(page -> page.pageNumber() == evidence.pageNumber())
                    .findFirst()
                    .map(page -> page.elements() == null ? "" : page.elements().stream()
                            .map(element -> element.text() == null ? "" : element.text())
                            .reduce("", (left, right) -> left + " " + right))
                    .orElse("");
            var status = status(evidence.sourceText(), pageText);
            return new Evidence(status == VerificationStatus.NOT_FOUND ? EvidenceType.UNSUPPORTED
                    : evidence.evidenceType(), status, evidence.pageNumber(), evidence.sectionTitle(),
                    evidence.sourceElementId(), evidence.sourceText(), evidence.sourceElementType(),
                    evidence.boundingBox(), evidence.reason());
        }).toList();
    }

    public boolean requiresHumanReview(List<Evidence> evidences) {
        return evidences.stream().anyMatch(evidence -> evidence.verificationStatus() == VerificationStatus.SIMILAR
                || evidence.verificationStatus() == VerificationStatus.NOT_FOUND);
    }

    private VerificationStatus status(String sourceText, String pageText) {
        String source = normalize(sourceText);
        String page = normalize(pageText);
        if (source.equals(page)) return VerificationStatus.EXACT;
        if (page.contains(source)) return VerificationStatus.PARTIAL;
        if (similarity(source, page) >= SIMILARITY_THRESHOLD) return VerificationStatus.SIMILAR;
        return VerificationStatus.NOT_FOUND;
    }

    private double similarity(String left, String right) {
        int longest = Math.max(left.length(), right.length());
        if (longest == 0) return 1;
        int[] previous = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) previous[column] = column;
        for (int row = 1; row <= left.length(); row++) {
            int[] current = new int[right.length() + 1];
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int cost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + cost);
            }
            previous = current;
        }
        return 1d - (double) previous[right.length()] / longest;
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }

    private List<PageContent> pages(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Document page contents are invalid", exception);
        }
    }
}
