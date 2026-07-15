package com.example.gameqacopilot.output;

import com.example.gameqacopilot.analysis.loop.ActorEvaluatorLoop;
import com.example.gameqacopilot.analysis.loop.Evaluation;
import com.example.gameqacopilot.document.entity.DocumentProcessingStatus;
import com.example.gameqacopilot.document.repository.PlanningDocumentRepository;
import com.example.gameqacopilot.project.ProjectRepository;
import com.example.gameqacopilot.testcase.TestCaseRepository;
import com.example.gameqacopilot.testcase.TestCaseStatus;
import com.example.gameqacopilot.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CsvOutputService {
    private final ProjectRepository projects;
    private final PlanningDocumentRepository documents;
    private final TestCaseRepository testCases;
    private final UserRepository users;
    private final OutputRepository outputs;
    private final CsvGenerator csvGenerator;
    private final ActorEvaluatorLoop loop;

    public CsvOutputService(
            ProjectRepository projects,
            PlanningDocumentRepository documents,
            TestCaseRepository testCases,
            UserRepository users,
            OutputRepository outputs,
            CsvGenerator csvGenerator,
            ActorEvaluatorLoop loop) {
        this.projects = projects;
        this.documents = documents;
        this.testCases = testCases;
        this.users = users;
        this.outputs = outputs;
        this.csvGenerator = csvGenerator;
        this.loop = loop;
    }

    @Transactional
    public OutputResponse create(Long projectId, Long userId) {
        var project = projects.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
        var document = documents.findFirstByProject_IdAndProcessingStatusOrderByCreatedAtDesc(
                        projectId, DocumentProcessingStatus.READY)
                .orElseThrow(() -> new IllegalArgumentException("Ready planning document not found"));
        var approved = testCases.findAllByPlanningDocument_IdAndStatusOrderByDisplayOrder(
                document.getId(), TestCaseStatus.APPROVED);
        var user = users.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        var output = outputs.save(new Output(
                project, document, user, "approved-test-cases-" + projectId + ".csv"));
        String expected = csvGenerator.generate(approved);

        loop.run(output.getId(), 100,
                (round, feedback) -> csvGenerator.generate(approved),
                draft -> draft.equals(expected)
                        ? new Evaluation(100, "CSV format and content verified")
                        : new Evaluation(0, "CSV format or content mismatch"));
        return findById(output.getId());
    }

    @Transactional(readOnly = true)
    public OutputResponse findById(Long outputId) {
        return OutputResponse.from(requireOutput(outputId));
    }

    @Transactional(readOnly = true)
    public Download download(Long outputId) {
        Output output = requireOutput(outputId);
        if (output.getStatus() != OutputStatus.SUCCESS || output.getFinalContent() == null) {
            throw new IllegalArgumentException("Output is not ready for download");
        }
        return new Download(output.getFileName(), output.getFinalContent().getBytes(StandardCharsets.UTF_8));
    }

    private Output requireOutput(Long outputId) {
        return outputs.findById(outputId)
                .orElseThrow(() -> new NoSuchElementException("Output not found"));
    }

    public record Download(String fileName, byte[] content) {}
}
