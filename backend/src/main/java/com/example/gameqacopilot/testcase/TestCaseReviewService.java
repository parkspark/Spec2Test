package com.example.gameqacopilot.testcase;

import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseReviewService {
    private final TestCaseRepository testCases;
    private final UserRepository users;
    private final TestCaseQueryService queries;

    public TestCaseReviewService(
            TestCaseRepository testCases, UserRepository users, TestCaseQueryService queries) {
        this.testCases = testCases;
        this.users = users;
        this.queries = queries;
    }

    @Transactional
    public TestCaseResponse approve(Long testCaseId, Long reviewerId) {
        var testCase = testCase(testCaseId);
        testCase.approve(reviewer(reviewerId));
        return queries.response(testCase);
    }

    @Transactional
    public TestCaseResponse reject(Long testCaseId, Long reviewerId, String reason) {
        var testCase = testCase(testCaseId);
        testCase.reject(reviewer(reviewerId), reason);
        return queries.response(testCase);
    }

    private TestCase testCase(Long id) {
        return testCases.findById(id).orElseThrow(() -> new NoSuchElementException("Test case not found"));
    }

    private User reviewer(Long id) {
        return users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
