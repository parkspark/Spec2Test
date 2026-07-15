package com.example.gameqacopilot.analysis.validator;

import com.example.gameqacopilot.analysis.dto.AiAnalysisResponse;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AnalysisResultValidator {
    private AnalysisResultValidator() {}

    public static <T> T validateWithOneRetry(Supplier<T> call, Consumer<T> validation) {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                T result = call.get();
                validation.accept(result);
                return result;
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure;
    }

    public static boolean invalidEvidence(AiAnalysisResponse.Evidence evidence) {
        if (evidence == null || evidence.evidenceType() == null || evidence.pageNumber() == null
                || evidence.pageNumber() < 1 || blank(evidence.sectionTitle())
                || blank(evidence.sourceText()) || evidence.sourceElementType() == null
                || blank(evidence.reason())) {
            return true;
        }
        var box = evidence.boundingBox();
        return box != null && (outside(box.x()) || outside(box.y())
                || outside(box.width()) || outside(box.height()));
    }

    private static boolean outside(double value) {
        return value < 0 || value > 1;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
