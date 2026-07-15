package com.example.gameqacopilot.analysis.loop;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LatsLoopLogRepository extends JpaRepository<LatsLoopLog, Long> {
    List<LatsLoopLog> findAllByOutput_IdOrderByDepthStep(Long outputId);
}
