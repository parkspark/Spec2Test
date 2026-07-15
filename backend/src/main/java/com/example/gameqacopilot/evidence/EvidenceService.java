package com.example.gameqacopilot.evidence;

import com.example.gameqacopilot.ambiguity.AmbiguityRepository;
import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse.Evidence;
import com.example.gameqacopilot.requirement.RequirementRepository;
import com.example.gameqacopilot.testcase.TestCaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceService {
    private final TestCaseRepository testCases;
    private final RequirementRepository requirements;
    private final AmbiguityRepository ambiguities;
    private final ObjectMapper objectMapper;

    public EvidenceService(TestCaseRepository testCases, RequirementRepository requirements,
            AmbiguityRepository ambiguities, ObjectMapper objectMapper) {
        this.testCases = testCases;
        this.requirements = requirements;
        this.ambiguities = ambiguities;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Evidence> findByTestCase(Long id) {
        var value = testCases.findById(id).orElseThrow(() -> new NoSuchElementException("Test case not found"));
        return read(value.getEvidences(), value.getDocumentPageCount());
    }

    @Transactional(readOnly = true)
    public List<Evidence> findByRequirement(Long id) {
        var value = requirements.findById(id).orElseThrow(() -> new NoSuchElementException("Requirement not found"));
        return read(value.getEvidences(), value.getDocumentPageCount());
    }

    @Transactional(readOnly = true)
    public List<Evidence> findByAmbiguity(Long id) {
        var value = ambiguities.findById(id).orElseThrow(() -> new NoSuchElementException("Ambiguity not found"));
        return read(value.getEvidences(), value.getDocumentPageCount());
    }

    private List<Evidence> read(String json, Integer pageCount) {
        try {
            List<Evidence> evidences = objectMapper.readValue(json, new TypeReference<>() {});
            if (pageCount == null || evidences.stream().anyMatch(evidence -> evidence.pageNumber() != null
                    && (evidence.pageNumber() < 1 || evidence.pageNumber() > pageCount))) {
                throw new IllegalStateException("Stored evidence page is outside the document range");
            }
            return evidences;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Stored evidence data is invalid", exception);
        }
    }
}
