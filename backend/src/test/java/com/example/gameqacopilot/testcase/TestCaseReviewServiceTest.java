package com.example.gameqacopilot.testcase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gameqacopilot.user.User;
import com.example.gameqacopilot.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TestCaseReviewServiceTest {
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TestCaseQueryService queries = mock(TestCaseQueryService.class);
    private final TestCaseReviewService service = new TestCaseReviewService(testCases, users, queries);

    @Test
    void approvesAndRejectsIndividualTestCasesWithReviewer() {
        var reviewer = mock(User.class);
        var approved = mock(TestCase.class);
        var rejected = mock(TestCase.class);
        when(users.findById(7L)).thenReturn(Optional.of(reviewer));
        when(testCases.findById(1L)).thenReturn(Optional.of(approved));
        when(testCases.findById(2L)).thenReturn(Optional.of(rejected));

        service.approve(1L, 7L);
        service.reject(2L, 7L, " 근거 부족 ");

        verify(approved).approve(reviewer);
        verify(rejected).reject(reviewer, " 근거 부족 ");
    }

    @Test
    void requiresReasonAndPreventsReviewingTwice() {
        var testCase = new TestCase();
        var reviewer = mock(User.class);
        when(reviewer.getId()).thenReturn(7L);
        ReflectionTestUtils.setField(testCase, "status", TestCaseStatus.GENERATED);

        assertThatThrownBy(() -> testCase.reject(reviewer, ""))
                .isInstanceOf(IllegalArgumentException.class);
        testCase.reject(reviewer, " 근거 부족 ");

        assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.REJECTED);
        assertThat(testCase.getReviewedById()).isEqualTo(7L);
        assertThat(testCase.getReviewedAt()).isNotNull();
        assertThat(testCase.getRejectionReason()).isEqualTo("근거 부족");
        assertThatThrownBy(() -> testCase.approve(reviewer))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
