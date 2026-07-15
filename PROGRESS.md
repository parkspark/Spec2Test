# PROGRESS — Game QA Copilot 랄프 루프 진행 로그

- 최신 기록이 맨 위에 온다.
- 형식: "## [날짜 시각] T-XX 작업명 — DONE | BLOCKED"
- DECISION NEEDED 항목은 사람이 답을 적은 뒤 BLOCKED를 해제한다.

## DECISION NEEDED (사람 확인 대기)

- T-26 CSV 생성 API는 `projectId`만 받지만 `Output.planning_document_id`는 NOT NULL 단일 FK다. 프로젝트에 여러 기획서의 승인 테스트 케이스가 있을 때 (1) 분석/기획서 ID를 요청에 추가할지, (2) 기획서별 Output을 여러 개 만들지, (3) 특정 기획서 선택 규칙을 둘지 결정이 필요하다.
- T-27 Markdown 생성 API도 `projectId`만 받고 §18.2 본문은 기획서명을 단수로 표시하며 `Output.planning_document_id`는 NOT NULL 단일 FK다. 여러 기획서가 있을 때 Markdown 대상 범위와 Output 기록 기준 결정이 필요하다.
→ 답변: 별도 ID를 요청에 추가하지 않는다. 해당 프로젝트의 PlanningDocument 중
    processingStatus=READY인 것을 createdAt 내림차순으로 정렬해 가장 최근 것 하나를
    산출물 대상으로 고정한다. READY 상태 기획서가 없으면 400을 반환하고
    Output을 생성하지 않는다.
    - T-26 CSV: 선택된 기획서에 속한 APPROVED 테스트 케이스만 포함
    - T-27 Markdown: 선택된 기획서에 속한 모호한 요구사항만 포함
    - Output.planning_document_id는 선택된 기획서 ID로 저장 (스키마 변경 없음)
    이 규칙은 MVP 범위 내 단순화이며, 여러 기획서를 사용자가 직접 선택하는 기능은
    보류 범위로 남긴다.

---

## [2026-07-15 14:17] T-27 모호 요구사항 Markdown 생성·다운로드 — DONE
- 구현 내용: 프로젝트의 최신 READY 기획서를 선택하고 해당 문서의 모호 요구사항을 질문·영향·심각도·상태·관련 요구사항·Evidence와 함께 기획서 §18.2 형식의 Markdown으로 생성한다.
  생성 결과와 파일명·상태 및 Actor–Evaluator 검증 로그를 Output/Lats_Loop_Log에 기록하는 QA 생성 API를 추가하고, 기존 USER·QA 조회·다운로드 API에서 Markdown 미디어 타입을 지원한다.
- 생성/수정 파일: MarkdownGenerator.java, CsvOutputService.java, OutputController.java, Output.java, AmbiguityRepository.java, RequirementRepository.java, MarkdownGeneratorTest.java, CsvOutputServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test` 전체 57개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-26/T-27 선행 API가 모두 완료되어 T-28 BLOCKED 표기를 해제했다. 프론트 산출물 화면과 Lats_Loop_Log 진행 상태 표시를 구현하면 된다.

## [2026-07-15 14:11] T-26 승인 테스트 케이스 CSV 생성·다운로드 — DONE
- 구현 내용: 프로젝트의 최신 READY 기획서를 선택하고 해당 문서의 APPROVED 테스트 케이스만 displayOrder 순서로 10컬럼 CSV에 출력한다.
  RFC 4180 이스케이프와 UTF-8 BOM을 적용하고, 생성 결과·파일명·상태 및 Actor–Evaluator 검증 로그를 Output/Lats_Loop_Log에 기록하는 QA 생성 API와 USER·QA 조회·다운로드 API를 구현했다.
- 생성/수정 파일: CsvGenerator.java, CsvOutputService.java, OutputController.java, OutputResponse.java, Output.java, PlanningDocumentRepository.java, TestCaseRepository.java, CsvGeneratorTest.java, CsvOutputServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain` 전체 55개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 다음 작업은 T-27 모호 요구사항 Markdown 생성·다운로드다. 동일한 최신 READY 기획서 선택 규칙을 재사용하고, READY 문서가 없으면 Output을 만들지 않고 400을 반환한다.

## [2026-07-15 14:01] T-29 JiraClient 인터페이스 + 목업 게시 + Issue 미리보기 API — DONE
- 구현 내용: Jira 호출을 JiraClient로 추상화하고 로컬 Key·URL을 반환하는 MockJiraClient를 추가했다.
  모호성의 질문·영향·Evidence·관련 요구사항을 기획서 §19.3 형식으로 조립하는 미리보기 API를 구현하고, 정확한 관련 요구사항 보존을 위해 JSONB 필드를 Flyway V8로 추가했다.
- 생성/수정 파일: jira 패키지(JiraClient, MockJiraClient, 미리보기 DTO·서비스·컨트롤러), Ambiguity.java, AmbiguityGenerationService.java, Requirement.java, V8__add_ambiguity_related_requirements.sql, JiraIssuePreviewServiceTest.java, AmbiguityGenerationServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test` 전체 52개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-30에서 QA의 명시적 POST 요청에만 JiraClient.publish를 호출하고 Output 기록·중복 차단·Ambiguity 상태 갱신을 한 트랜잭션 경계로 연결한다. T-26/T-27은 다중 기획서의 Output FK 기준 결정 대기 상태다.

## [2026-07-15 14:00] T-28 프론트 산출물 화면 + Lats_Loop_Log 진행 상태 표시 — BLOCKED
- 차단 사유: 화면이 호출해야 할 T-26/T-27 생성·다운로드 API가 대상 기획서 결정 대기 상태다. 구현되지 않은 기능을 동작하는 것처럼 표시할 수 없어 선행 결정과 API 구현 후 진행해야 한다.

## [2026-07-15 13:52] T-25 Actor–Evaluator 루프 프레임워크 — DONE
- 구현 내용: 호출자가 지정한 통과 점수로 Actor 초안을 Evaluator가 평가하고, 피드백을 반영해 최대 3회 재생성하는 루프를 구현했다.
  모든 완료 회차를 Lats_Loop_Log에 저장하며 통과 시 Output을 SUCCESS로, 3회 미통과 시 실패 사유와 함께 FAILED로 전환한다. AI 콜백 실행과 DB 트랜잭션은 분리했다.
- 생성/수정 파일: ActorEvaluatorLoop.java, Evaluation.java, LoopPersistenceService.java, LatsLoopLog.java, Output.java 및 저장소·Enum, ActorEvaluatorLoopTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test` 전체 50개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-26에서 CSV Output을 PENDING으로 생성한 뒤 ActorEvaluatorLoop에 CSV 초안 생성기와 평가기를 연결한다. 통과 점수는 명세에 고정값이 없어 호출자가 명시한다.

## [2026-07-15 13:48] T-24 Output + Lats_Loop_Log 테이블 마이그레이션 — DONE
- 구현 내용: Output 최종 상태·파일·외부 연동·요청/응답 데이터를 통합 저장하는 outputs 테이블과 Actor–Evaluator 회차별 초안·점수·피드백을 저장하는 lats_loop_logs 테이블을 V7 Flyway 마이그레이션으로 추가했다.
  산출물 유형·상태·외부 서비스 CHECK 제약과 프로젝트·문서·모호성·사용자·부모 Output FK를 적용했다.
- 생성/수정 파일: V7__create_outputs_and_lats_loop_logs.sql, OutputMigrationTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain` 전체 48개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 다음 작업은 T-25 Actor–Evaluator 루프 프레임워크다. 이번 작업은 스키마만 추가했으며 루프 실행·상태 전이 로직은 구현하지 않았다.

## [2026-07-15 13:43] T-23 개별 승인/반려 API + 확인 모달 + 반려 사유 필수 입력 — DONE
- 구현 내용: QA 전용 개별 승인·반려 API와 GENERATED 상태에서만 가능한 단방향 상태 전이를 구현하고 검토자·검토 시각·반려 사유를 기존 TestCase 필드에 저장한다.
  프론트에 승인 확인 모달과 필수 반려 사유 입력 모달을 연결했으며 USER에게는 승인·반려 동작을 노출하지 않는다.
- 생성/수정 파일: TestCase.java, TestCaseController.java, TestCaseReviewService.java, TestCaseRejectRequest.java, TestCaseResponse.java, App.tsx, App.css, TestCaseReviewServiceTest.java, App.test.tsx, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --rerun-tasks --no-daemon --console=plain`, `cd frontend && npm run build && npm test -- --run` 통과 (Gradle BUILD SUCCESSFUL, Vitest 7개 통과)
- 다음 작업자를 위한 메모: 다음 작업은 T-24 Output + Lats_Loop_Log 테이블 마이그레이션이다. 승인·반려 후 상태 되돌리기와 bulk API는 명세상 구현하지 않았다.

## [2026-07-15 13:38] T-22 프론트 Evidence 연동 — DONE
- 구현 내용: 테스트 케이스의 전체 Evidence를 조회하고 분석 작업에 연결된 PDF 문서의 해당 페이지로 이동하는 원문 패널을 구현했다.
  여러 근거 선택, 페이지·섹션·유형·검증 상태·원문·선정 이유 표시를 제공하며 boundingBox가 있는 근거만 이미지 위에 하이라이트한다.
- 생성/수정 파일: frontend/src/App.tsx, frontend/src/App.css, frontend/src/App.test.tsx, TestCaseResponse.java, TestCaseQueryServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test`, `cd frontend && npm run build && npm test -- --run` 통과 (Gradle BUILD SUCCESSFUL, Vitest 6개 통과)
- 다음 작업자를 위한 메모: 다음 작업은 T-23 개별 승인/반려 API와 확인 모달이다. Evidence 좌표가 없으면 하이라이트 없이 페이지 이미지와 원문을 표시한다.

## [2026-07-15 13:34] T-21 프론트 테스트 시트 화면 — DONE
- 구현 내용: 프로젝트별 테스트 케이스 API를 연결한 검토 경로를 추가하고 고정 순서 10컬럼 표, 셀 줄바꿈, 가로 스크롤 및 컬럼 너비 조절을 구현했다.
  행 선택 시 상세 API 결과를 패널에 표시하며 Evidence 요약과 비고 태그, QA 전용 승인·반려 버튼을 제공한다.
- 생성/수정 파일: frontend/src/App.tsx, frontend/src/App.css, frontend/src/App.test.tsx, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd frontend && npm run build && npm test -- --run` 통과 (Test Files 1 passed, Tests 5 passed)
- 다음 작업자를 위한 메모: 다음 작업은 T-22 Evidence PDF 연동이다. 승인·반려 버튼은 T-23 API·모달 구현 전까지 가짜 동작을 피하도록 비활성 상태다.

## [2026-07-15 13:29] T-20 테스트 케이스 목록·상세 API — DONE
- 구현 내용: 프로젝트별 테스트 케이스 목록과 개별 상세 조회 API를 추가하고 analysisId·상태·대/중/소분류·테스트 유형·신뢰도·키워드 필터 및 displayOrder 정렬을 구현했다.
  JSONB의 사전조건·스텝·기대결과·Evidence·비고를 DTO로 반환하며, 목록에는 첫 Evidence 요약을, 상세에는 전체 Evidence 배열을 포함한다.
- 생성/수정 파일: TestCaseController.java, TestCaseQueryService.java, TestCaseResponse.java, TestCaseListResponse.java, TestCase.java, TestCaseRepository.java, TestCaseQueryServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain --no-problems-report` 전체 45개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 다음 작업은 T-21 프론트 테스트 시트 화면이다. 키워드 필터는 테스트 항목(testItem)의 대소문자 무시 부분 일치이며, 승인·반려 API는 T-23 범위라 이번 작업에서 추가하지 않았다.

## [2026-07-15 12:19] T-19 비고 자동 생성 규칙 구현 — DONE
- 구현 내용: 검증된 Evidence의 INFERRED·UNSUPPORTED 유형과 SIMILAR·NOT_FOUND 원문 검증 상태에 따라 기획서 §20.8 비고를 자동 생성한다.
  AI가 생성한 기존 비고와 기대결과 정책 미정 표시는 보존하고, 동일 비고는 중복 저장하지 않으며 관련 모호성 비고는 기존 연동 흐름을 유지한다.
- 생성/수정 파일: TestCaseNotes.java, TestCaseGenerationService.java, TestCaseNotesTest.java, TestCaseGenerationServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: cd backend && ./gradlew test --no-daemon --console=plain --no-problems-report 전체 42개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 다음 작업은 T-20 테스트 케이스 목록·상세 API다. 기대결과 정책 미정은 구조화 플래그가 없어 AI가 생성한 notes 값을 보존하며, 관련 모호성은 AmbiguityGenerationService가 기획 확인 필요를 추가한다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 12:09] T-17 Evidence 원문 일치 검증 — DONE
- 구현 내용: 해당 페이지 텍스트의 공백·줄바꿈을 정규화하고 EXACT→PARTIAL→Levenshtein 유사도 0.7 이상 SIMILAR→NOT_FOUND 순서로 원문을 검증해 저장한다.
  NOT_FOUND는 UNSUPPORTED로 전환하고, SIMILAR·NOT_FOUND 테스트 케이스는 requiresHumanReview=true로 저장하며 페이지 범위 밖 Evidence는 거부한다.
- 생성/수정 파일: EvidenceVerifier.java, RequirementExtractionService.java, TestCaseGenerationService.java, AmbiguityGenerationService.java, TestCase.java, EvidenceVerifierTest.java, 관련 생성 서비스 테스트, plan/BACKLOG.md, PROGRESS.md
- 테스트: cd backend && ./gradlew test --no-daemon --console=plain --no-problems-report 전체 41개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 유사도 임계값 0.7은 잠정치이며 추후 실제 데이터에 따라 조정할 수 있다. 다음 작업은 T-19 비고 자동 생성 규칙이다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 11:40] T-18 Evidence 조회 API 3종 + 페이지 범위 검증 — DONE
- 구현 내용: 테스트 케이스·요구사항·모호성별 Evidence 조회 API 3종을 추가하고 저장된 Evidence JSON을 DTO로 반환한다.
  페이지 번호가 있는 Evidence는 기획서 pageCount 범위를 검증하며, 범위를 벗어난 저장 데이터는 반환하지 않는다.
- 생성/수정 파일: EvidenceController.java, EvidenceService.java, Requirement.java, TestCase.java, Ambiguity.java, EvidenceServiceTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain --no-problems-report` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-17은 PARTIAL/SIMILAR 판정 기준 결정 대기 상태다. 결정 후 원문 검증 결과를 저장 단계에 반영해야 한다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 11:26] T-16 분석 결과 스키마/Enum/분류 일관성/중복 검증 + 실패 시 1회 재시도 — DONE
- 구현 내용: 분류·요구사항·테스트 케이스·모호성 AI 응답에 공통 1회 재시도를 적용하고, 두 번째 실패 후에만 기존 AnalysisJob 실패 처리로 전달한다.
  JSON 필수 필드와 Enum 역직렬화, Evidence 구조·좌표 범위, 분류명 일관성, ID·출력 순서·동일 목적 테스트 케이스 중복을 저장 전에 검증한다.
- 생성/수정 파일: AnalysisResultValidator.java, CategoryClassificationService.java, RequirementExtractionService.java, TestCaseGenerationService.java, AmbiguityGenerationService.java, 관련 단위 테스트, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --rerun-tasks --no-daemon --console=plain --no-problems-report` 전체 36개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-17에서 추출 원문을 기준으로 Evidence의 EXACT/PARTIAL/SIMILAR/NOT_FOUND를 판정하고 NOT_FOUND를 UNSUPPORTED로 전환한다. 현재 T-16은 스키마상 페이지 번호 최소값만 검증하며 문서 pageCount 상한·원문 일치는 T-17 범위다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 11:16] T-15 모호한 요구사항 생성 단계 구현 + Ambiguity 저장 + 관련 TC 비고 연동 — DONE
- 구현 내용: 요구사항과 테스트 케이스를 기반으로 임의 확정 없이 질문·영향도·심각도·Evidence를 포함한 모호성을 생성하고 OPEN 상태로 저장한다.
  관련 요구사항 ID와 분류를 실존 데이터로 검증하고, 연결된 테스트 케이스 비고에 관련 모호성 ID와 기획 확인 필요를 추가해 사람 검토 대상으로 표시한다.
- 생성/수정 파일: AmbiguityGenerationService.java, Ambiguity.java, AmbiguityRepository.java, AmbiguityStatus.java, AmbiguityGenerationResponse.java, V6__create_ambiguities.sql, TestCase.java, AmbiguityGenerationServiceTest.java
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain --no-problems-report` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-16에서 전체 분석 결과의 스키마·Enum·분류 일관성·중복을 검증하고 실패 시 1회 재시도한다. Ambiguity 관련 요구사항은 기획서 §15.7에 별도 FK가 없어 실존 ID 검증과 TC 비고로 연결했다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 10:53] T-14 10컬럼 테스트 케이스 생성 단계 구현 + TestCase 저장 — DONE
- 구현 내용: 확정 분류와 요구사항을 재사용해 10컬럼 테스트 케이스를 생성하고, 요구사항·분류 일치와 Evidence 필수값을 검증해 GENERATED 상태로 저장한다.
  TestCase 전체 필드와 JSONB 5종, 관계 및 Enum 제약조건을 V5 Flyway 마이그레이션과 JPA 엔티티로 추가했다.
- 생성/수정 파일: TestCaseGenerationService.java, TestCase.java, TestCaseRepository.java, TestCaseStatus.java, TestCaseGenerationResponse.java, V5__create_test_cases.sql, TestCaseGenerationServiceTest.java
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain --no-problems-report` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-15에서 모호성 저장 후 관련 테스트 케이스 notes에 `관련 모호성: AMB-001`, `기획 확인 필요`를 연동한다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 10:34] 사람 개입 — T-13 완료 확인
- 원인: 테스트 픽스처 JSON 문자열의 이스케이프 오류 ("[{\pageNumber\:1}]")로 컴파일 실패
- 조치: 텍스트 블록(""") 또는 올바른 \" 이스케이프로 수정
- ./gradlew test 전체 통과 확인

## [2026-07-15 10:24] T-13 요구사항 추출 단계 구현 + Requirement 저장 — BLOCKED
- 구현 내용: 분류 체계를 재사용하는 멀티모달 요구사항 추출 서비스, Requirement 엔티티·저장소·V4 Flyway 마이그레이션, 요구사항 프롬프트와 단위 테스트를 작성했으나 검증 실패로 완료 처리하지 않았다.
- 생성/수정 파일: RequirementExtractionService.java, Requirement.java, RequirementRepository.java, RequirementExtractionResponse.java, V4__create_requirements.sql, requirement.txt, RequirementExtractionServiceTest.java, AnalysisJob 관련 서비스·컨트롤러·통합 테스트
- 테스트: `cd backend && ./gradlew test --tests com.example.gameqacopilot.requirement.RequirementExtractionServiceTest` 실패. 3회 수정 후 테스트 픽스처 36행의 JSON 문자열이 `[{\pageNumber\:1}]`로 손상되어 `compileTestJava` illegal escape character 오류가 남았다. 전체 테스트는 실행하지 못했다.
- 다음 작업자를 위한 메모: RequirementExtractionServiceTest 36행의 페이지 JSON을 Java 텍스트 블록 등으로 유효하게 복구한 뒤 대상 테스트와 `./gradlew test` 전체를 다시 실행해야 한다. 이번 회차 변경은 커밋하지 않았다.

## [2026-07-15 10:07] T-12 Spring AI ChatClient 멀티모달 호출 + 기능 분류 추출 단계 구현 — DONE
- 구현 내용: Spring AI ChatClient에 추출 텍스트·페이지 요소 JSON·모든 페이지 이미지를 함께 전달해 기능 분류 구조를 생성한다.
  구조화 응답의 Evidence와 대/중/소분류 필수값(`-` 포함)을 검증하고, 모델·프롬프트 버전·원문 응답·토큰 사용량 및 실패 상태를 AnalysisJob에 기록한다.
- 생성/수정 파일: backend/build.gradle, CategoryClassificationResponse.java, CategoryClassificationService.java, AnalysisJob.java, AnalysisJobService.java, AnalysisJobController.java, application.yml, classification.txt, CategoryClassificationServiceTest.java, AnalysisJobIntegrationTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test` 전체 30개 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-13은 PROCESSING 상태 작업의 rawResponse에 저장된 categoryTree를 재사용해 동일 분류명으로 Requirement를 생성·저장한다. 실행 환경에 OPENAI_API_KEY와 멀티모달 지원 AI_MODEL이 필요하다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제). 사람이 수동 커밋 처리함

## [2026-07-15 09:46] T-11 프롬프트 리소스 파일 작성 — DONE
- 구현 내용: 기획서 §20의 시스템·분류·테스트케이스·모호성·Evidence·금지 규칙을 v1.0 리소스 6종으로 분리했다.
  AI 응답 JSON 스키마의 필드명과 Enum을 사용하고, 리소스 패키징 및 핵심 규칙을 계약 테스트로 검증했다.
- 생성/수정 파일: backend/src/main/resources/prompts/v1.0/*.txt, PromptResourcesTest.java, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test --no-daemon --console=plain --rerun-tasks` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-12부터 promptVersion은 v1.0을 사용하고 필요한 리소스를 조합한다.
- 커밋 실패 — 사람이 수동 커밋 필요 (.git ACL 문제) 사람이 수동 커밋 처리함

## [2026-07-15 09:28] 사람 개입 — T-09/T-10 커밋 정리
- T-08 이후 T-09 자동 커밋이 누락되어 있었음 (원인: .git ACL 권한 문제로 추정)
- T-09, T-10 변경사항을 하나의 커밋으로 수동 처리함
- loop.sh/PROMPT.md에 커밋 직전 ACL 리셋 로직 추가 검토 필요

## [2026-07-15 09:00] T-10 AI 응답 DTO 및 JSON 스키마 전체 정의 — DONE
- 구현 내용: 문서 요약·기능 분류·요구사항·10컬럼 테스트 케이스·모호성·Evidence를 하나의 AI 분석 응답 DTO로 정의했다.
  기획서 Enum과 좌표 범위를 포함한 JSON Schema를 추가하고 정상 응답 역직렬화, 잘못된 JSON 및 미정의 Enum 거부를 검증했다.
- 생성/수정 파일: AiAnalysisResponse.java, ai-analysis-response.schema.json, AiAnalysisResponseTest.java, ai-analysis-response.json, plan/BACKLOG.md, PROGRESS.md
- 테스트: `cd backend && ./gradlew test` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-11 프롬프트에서 `/schemas/ai-analysis-response.schema.json` 구조와 동일한 필드명·Enum을 사용한다.

## [2026-07-15 08:38] T-09 AnalysisJob 테이블 + 분석 요청/조회 API 골격 — DONE
- 구현 내용: AnalysisJob과 상태 Enum을 V3 Flyway 마이그레이션 및 JPA 엔티티로 추가했다.
  READY 문서의 QA 분석 요청은 PENDING 작업을 생성하고, USER·QA는 작업 단건 및 문서별 최신 작업을 DTO로 조회할 수 있다.
- 생성/수정 파일: analysis 하위 controller/dto/entity/repository/service, V3__create_analysis_jobs.sql, AnalysisJob 단위·통합 테스트, 기존 통합 테스트 정리 순서
- 테스트: `cd backend && ./gradlew test` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: T-10에서 AI 전체 응답 DTO와 JSON 스키마를 정의한다. modelName·promptVersion 및 실행 결과 필드는 실제 호출 단계 전까지 null이다.

## [2026-07-15 08:06] 사람 개입 — T-08 완료 확인
- 원인: PlanningDocumentRepository.findAllByProjectIdOrderByCreatedAtDesc가 
  연관관계(Project) 경로를 못 찾아 ApplicationContext 생성 실패
- 조치: findAllByProject_IdOrderByCreatedAtDesc로 파생 쿼리 메서드명 수정
- ./gradlew test 전체 통과 확인 (BUILD SUCCESSFUL)

## [2026-07-14 17:03] T-08 기획서 조회 API (문서/페이지 단위) + 프론트 PDF 미리보기 화면 — BLOCKED
- 차단 사유: 허용된 수정 3회 후에도 `./gradlew test`가 Spring Data의 `PlanningDocumentRepository.findAllByProjectIdOrderByCreatedAtDesc`를 엔티티의 존재하지 않는 `projectId` 속성으로 해석해 ApplicationContext 생성 단계에서 실패했다.
- 구현 내용: 문서 목록·상세·페이지 텍스트/좌표·페이지 이미지 조회 API와 3열 PDF 미리보기 화면, 페이지 이동 및 단위·통합 테스트를 작성했으나 검증 실패로 완료 처리하지 않았다.
- 생성/수정 파일: document controller/dto/repository/service 및 테스트, frontend App.tsx/App.css/App.test.tsx/package.json, plan/BACKLOG.md, PROGRESS.md (모두 미커밋)
- 테스트: 프론트 빌드 통과, `npx vitest --configLoader native --run` 3개 통과. 백엔드 21개 중 ApplicationContext 의존 테스트 15개 실패로 전체 검증 실패.
- 다음 작업자를 위한 메모: 파생 쿼리를 엔티티 연관 경로에 맞게 수정한 뒤 백엔드·프론트 지정 검증 명령을 모두 재실행해야 한다. 이번 회차는 규칙에 따라 추가 수정·커밋하지 않았다.

## [2026-07-14 16:49] T-07 페이지별 텍스트 추출 + 페이지 이미지 생성 + 텍스트 좌표 추출 — DONE
- 구현 내용: PDFBox로 페이지별 텍스트와 상대 비율 좌표를 추출해 pageContents JSONB에 저장했다.
  문서별 디렉터리에 원본 PDF와 페이지 PNG를 함께 저장하고 처리 완료 상태를 READY로 전환했다.
- 생성/수정 파일: PdfDocumentProcessor, PlanningDocument, PlanningDocumentService, PDF 처리 단위·통합 테스트
- 테스트: `cd backend && ./gradlew test` 전체 통과 (Gradle BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 다음 작업은 T-08이다. 생성된 페이지 이미지는 원본 PDF와 같은 디렉터리의 page-{번호}.png에 있다.

## [2026-07-14 16:40] T-06 PlanningDocument 테이블 마이그레이션 + PDF 업로드 API — DONE
- 구현 내용: PlanningDocument V2 마이그레이션과 DTO 기반 QA 전용 업로드 API를 추가했다.
  PDF 형식·크기·암호화·손상을 검증하고 원본 파일, 페이지 수, 처리 상태와 실패 사유를 저장한다.
- 생성/수정 파일: backend/build.gradle, document 하위 Entity/DTO/Repository/Service/Controller, V2__create_planning_documents.sql, PlanningDocument 테스트, 기존 테스트 정리 순서
- 테스트: `cd backend && ./gradlew test` 전체 통과 (Gradle BUILD SUCCESSFUL, 단위·통합 테스트 포함)
- 다음 작업자를 위한 메모: 다음 작업은 T-07이다. 페이지별 텍스트·이미지·좌표 추출과 pageContents 저장은 이번 T-06 범위에서 제외했다.

## [2026-07-14 16:22] T-04 프로젝트 생성·목록·상세 API + 권한 검증 — DONE
- 구현 내용: QA 전용 프로젝트 생성과 USER·QA 목록/상세 조회 API를 구현하고 생성자 User 및 ACTIVE 상태를 연결했다.
  DTO 응답, 403 권한 제한, 404 미존재 처리와 요청 validation 400 매핑을 적용했다.
- 생성/수정 파일: backend project 도메인(Entity·DTO·Repository·Service·Controller), GlobalExceptionHandler, ProjectIntegrationTest, AuthIntegrationTest, .gitignore
- 테스트: `cd backend && ./gradlew test` 전체 통과 (5 actionable tasks, BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: 시스템 GRADLE_USER_HOME 권한 문제 시 저장소 `.gradle-local` 경로로 환경 변수를 재지정할 것. 다음 작업은 T-06이다.
(이하 회차 기록)

## [2026-07-14 16:16] 사람 개입 — T-04 재개 지침 작성
- 원인: validation 실패 시 MethodArgumentNotValidException이 400이 아닌 500으로 처리됨
- 결정: 전역 예외 핸들러에 해당 예외 → 400 매핑 추가하도록 지침 남김
- QA 권한 체크(403)는 이미 정상 동작하므로 수정 범위는 예외 처리 부분으로 한정

## [2026-07-14 15:06] T-04 프로젝트 생성·목록·상세 API + 권한 검증 403 — BLOCKED
- 차단 사유: 백엔드 검증 환경 복구를 3회 시도했으나 Gradle 기본 캐시 잠금 권한 거부, 임시 캐시 네트워크 대기, 한글 경로에서 테스트 워커 클래스패스 손상 및 잠금 파일 복사 실패가 연속 발생함
- 구현 내용: QA 프로젝트 생성, USER·QA 목록/상세 조회, 생성자 User 연결, 생성 상태 ACTIVE 고정, USER 생성 요청 403 처리를 구현했으나 커밋하지 않음
- 생성/수정 파일: backend project 도메인 Entity·DTO·Repository·Service·Controller, GlobalExceptionHandler, ProjectIntegrationTest, AuthIntegrationTest, .gitignore
- 테스트: compileJava 및 compileTestJava 통과; test 실행기는 GradleWorkerMain ClassNotFoundException으로 시작 실패
- 다음 작업자를 위한 메모: 남아 있는 변경을 유지하고 기존 Gradle 데몬/잠금을 해제한 뒤 ASCII 경로의 GRADLE_USER_HOME에서 전체 테스트를 재실행할 것. 통과 전 BACKLOG 완료 처리 및 커밋 금지

## [2026-07-14 14:40] 사람 개입 — T-05 실제 완료 확인
- vite.config.ts에 pool: 'threads' 적용 후 npm test -- --run 로컬 통과 확인 (Test Files 1 passed, Tests 2 passed)
- T-05는 실제로 완료된 상태였음 (BLOCKED 라벨은 테스트 실행 환경 문제로 인한 것)
 → BACKLOG DONE 처리
- T-04는 DECISION NEEDED 답변만 반영됐고 실제 구현은 안 된 상태로 확인됨 → BLOCKED 라벨만 제거, 미완료 상태 유지

## [2026-07-14 14:21] T-05 React 프로젝트 골격 — BLOCKED
- 차단 사유: 허용된 수정 시도 3회를 소진했으며, `npm test -- --run`에서 Vitest가 Vite 설정을 로드하는 중 Windows 샌드박스의 자식 프로세스 실행이 `spawn EPERM`으로 차단됨
- 구현 내용: 기존 Vite 템플릿에 React Router·TanStack Query·Axios·React Hook Form/Zod와 최소 shadcn/ui 컴포넌트를 연결하고, 실제 세션 API 기반 로그인 및 QA 전용 프로젝트 생성 버튼을 구현했으나 커밋하지 않음
- 생성/수정 파일: frontend/package.json, frontend/package-lock.json, frontend/vite.config.ts, frontend/src/App.tsx, frontend/src/App.test.tsx, frontend/src/main.tsx, frontend/src/components/ui/, frontend/src/lib/, frontend/src/test/, frontend/src/*.css
- 테스트: `npm run build` 통과; `npm test -- --run` 실패 (Vitest startup error: Vite externalize-deps의 `spawn EPERM`)
- 다음 작업자를 위한 메모: Vite 빌드는 `--configLoader native`로 통과한다. Vitest에도 네이티브 config loader를 적용하거나 자식 프로세스 실행이 허용된 환경에서 테스트를 재실행한 뒤 완료 처리할 것. 작업 전부터 존재한 loop.sh 변경은 본 작업과 무관하므로 커밋에서 제외할 것.

## [2026-07-14 14:01] T-03 Spring Security 로그인/로그아웃/me API + USER·QA 역할 분리 — DONE
- 구현 내용: BCrypt 비밀번호 검증과 HTTP 세션 기반 로그인·로그아웃·현재 사용자 조회 API를 구현했다.
  USER는 조회 요청만 가능하고 QA만 변경 요청을 수행하도록 전역 Spring Security 권한 규칙과 401/403 처리를 적용했다.
  별도 Vite 프론트엔드가 세션 쿠키를 사용할 수 있도록 개발용 전역 CORS에 credentials 허용을 추가했다.
- 생성/수정 파일: backend/build.gradle, backend/src/main/java/com/example/gameqacopilot/common/security/, backend/src/main/java/com/example/gameqacopilot/user/, backend/src/test/java/com/example/gameqacopilot/user/AuthIntegrationTest.java, .gitignore
- 테스트: `cd backend && ./gradlew test` 통과 (전체 10개, 인증·세션·로그아웃·USER 403·QA 허용 및 기존 회귀 테스트)
- 참고: .git ACL DENY 재발 문제로 자동 커밋 실패, 사람이 수동 커밋 처리
- 다음 작업자를 위한 메모: 사용자 생성 API는 기획서 범위에 없어 구현하지 않았다. DB의 users.password에는 BCrypt 해시를 저장해야 한다.

## [2026-07-14 13:33] T-02 PostgreSQL 연결 + Flyway 설정 + V1 마이그레이션 — DONE
- 구현 내용: PostgreSQL 연결과 JPA/Flyway 설정을 추가하고, User·Project 테이블 및 역할·소유자 제약조건을 V1 마이그레이션으로 구성했다.
  H2 PostgreSQL 호환 모드에서 마이그레이션 적용과 필수 제약조건을 검증하는 단위 테스트를 추가했다.
- 생성/수정 파일: backend/build.gradle, backend/src/main/resources/application.yml, backend/src/main/resources/db/migration/V1__create_users_and_projects.sql, backend/src/test/resources/application.yml, backend/src/test/java/com/example/gameqacopilot/DatabaseMigrationTest.java
- 테스트: `cd backend && ./gradlew test` 통과 (전체 백엔드 테스트 BUILD SUCCESSFUL)
- 다음 작업자를 위한 메모: DB 접속 정보는 DB_URL, DB_USERNAME, DB_PASSWORD 환경 변수로 주입하며, 다음 작업은 T-03이다.

## [2026-07-14 13:28] 사람 개입 — T-02 재개 지침 작성
- 원인: H2 PostgreSQL 호환 모드가 프로젝트에 잡힌 버전에서 INSERT...RETURNING 미지원
- 결정: 테스트를 DB 비종속적으로 재작성 (INSERT 후 별도 SELECT로 생성 ID 확인). RETURNING 문법 자체를 프로덕션/테스트 코드에서 사용하지 않는다.
- 다음 회차는 이 지침대로 DatabaseMigrationTest만 수정하고 나머지 코드는 그대로 유지할 것

## [2026-07-14 12:34] T-02 PostgreSQL 연결 + Flyway 설정 + V1 마이그레이션 — BLOCKED
- 차단 사유: 검증 실패 후 허용된 수정 시도 3회를 소진했으며, 최종 실행에서 H2 PostgreSQL 호환 모드가 테스트의 `INSERT ... RETURNING` 문법을 지원하지 않아 `DatabaseMigrationTest`가 실패함
- 구현 내용: PostgreSQL/JPA/Flyway 의존성과 환경 변수 기반 DB 설정, User·Project V1 마이그레이션, H2 기반 마이그레이션 제약조건 테스트를 작성했으나 커밋하지 않음
- 생성/수정 파일: backend/build.gradle, backend/src/main/resources/application.yml, backend/src/main/resources/db/migration/V1__create_users_and_projects.sql, backend/src/test/resources/application.yml, backend/src/test/java/com/example/gameqacopilot/DatabaseMigrationTest.java
- 테스트: `cd backend && ./gradlew test` 실패 (6개 중 1개 실패: DatabaseMigrationTest.java:31, H2 INSERT RETURNING 구문 오류)
- 다음 작업자를 위한 메모: 테스트의 생성 ID 조회를 H2/PostgreSQL 공통 방식으로 바꾼 뒤 전체 테스트를 다시 실행할 것. 현재 변경은 작업 트리에 남아 있고 커밋하지 않았음
## [2026-07-14 12:15] T-01 Spring Boot 프로젝트 골격 생성 — DONE
- 구현 내용: Java 21/Spring Boot 3.5.16 Gradle 프로젝트와 §25.2 패키지 골격을 구성함.
  전역 CORS 설정, Swagger/OpenAPI, 공통 응답 및 RFC 9457 기반 예외 처리를 추가함.
- 생성/수정 파일: backend/build.gradle, backend/src/main/java/com/example/gameqacopilot/, backend/src/main/resources/application*.yml
- 테스트: `cd backend && ./gradlew test` 통과 (CORS, 공통 응답/예외, OpenAPI 문서 노출 검증)
- 참고: 최초 실행 시 .git/index.lock 권한 문제로 자동 커밋 실패, 사람이 수동 커밋 처리함
- 다음 작업자를 위한 메모: 없음

## [2026-07-14 11:32] T-01 Spring Boot 프로젝트 골격 생성 — BLOCKED
- 차단 사유: Java 21 실행 파일은 VS Code 확장 경로에서 확인했으나 Gradle, Gradle Wrapper 및 Spring 의존성 캐시가 없고 외부 네트워크가 차단되어 프로젝트 생성과 필수 테스트 검증을 수행할 수 없음
- 확인 결과: `JAVA_HOME`은 존재하지 않는 `C:\Program Files\jdk-24.0.1`을 가리키며, `java`와 `gradle`은 PATH에 없음. Gradle 배포본 다운로드를 프록시·직접 연결 방식으로 총 3회 시도했으나 모두 실패
- 검증: 구현을 시작하지 않아 `backend && ./gradlew test` 실행 불가. 테스트 실패 상태 커밋 금지 규칙에 따라 코드 변경 및 커밋 없음
- 다음 작업자가 볼 내용: Java 21과 Gradle 의존성 다운로드가 가능한 환경을 제공하거나 Gradle 배포본/의존성 캐시를 준비한 뒤 T-01의 BLOCKED를 해제할 것

## [2026-07-14 11:26] 사람 개입 — 근본 원인 파악 및 정리
- 원인: 이전 파일명 수정이 커밋되지 않은 상태에서 git reset --hard origin/main으로 유실됨
- 조치: 파일명 재수정 및 즉시 커밋, T-01~T-04 BLOCKED 태그 전부 제거
- 다음 회차는 T-01부터 정상 진행

## [2026-07-14 11:18] T-03 Spring Security 로그인/로그아웃/me API + USER·QA 역할 분리 — BLOCKED
- 차단 사유: 선행 작업 T-01·T-02가 완료되지 않아 Spring Boot/Spring Security 실행 환경과 User 영속성 기반이 없음
- 확인 결과: `backend`에는 `.gitkeep`만 존재하며, T-03에서 기반까지 생성하면 한 회차에 T-01·T-02·T-03을 함께 수행하게 됨
- 검증: 구현을 시작하지 않아 백엔드 검증 생략
- 커밋: 실행 환경이 `.git/index.lock` 생성을 거부하여 로컬 커밋 실패 (`Permission denied`)
- 다음 작업자가 볼 내용: T-01의 기존 DECISION NEEDED와 BLOCKED 상태를 사람이 정리하고 T-01→T-02 순서로 완료한 뒤 T-03의 BLOCKED를 해제할 것

## [2026-07-14 10:30] T-02 PostgreSQL 연결 + Flyway 설정 + V1 마이그레이션 — BLOCKED
- 차단 사유: 선행 작업 T-01이 완료되지 않아 Spring Boot/Gradle 프로젝트 골격과 테스트 실행 환경이 없음
- 확인 결과: `backend`에는 `.gitkeep`만 존재하며, T-02에서 골격까지 생성하면 한 회차에 T-01과 T-02를 함께 수행하게 됨
- 검증: 구현을 시작하지 않아 백엔드 검증 생략
- 다음 작업자가 볼 내용: 기준 문서가 현재 지정 경로에 존재하므로 T-01의 기존 DECISION NEEDED와 BLOCKED 상태를 사람이 정리한 뒤 T-01부터 완료할 것

## [2026-07-14 09:48] T-01 Spring Boot 프로젝트 골격 생성 — BLOCKED
- 차단 사유: AGENTS.md가 지정한 유일한 기준 문서 경로에 파일이 없음
- 확인 결과: `specs/game-qa-copilot-final-spec-v1.0.md`만 존재함
- 검증: 구현을 시작하지 않아 백엔드/프론트엔드 검증 생략
- 다음 작업자가 볼 내용: DECISION NEEDED 답변 후 T-01의 BLOCKED 표기를 해제하고 진행
