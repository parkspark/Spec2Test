# BACKLOG — Game QA Copilot

표기: [ ] 미착수 / [x] 완료 / BLOCKED: 사유

## Phase 1. 프로젝트 기반
- [x] T-01 Spring Boot 프로젝트 골격 생성 (Java 21, Gradle, 기획서 §25.2 패키지 구조, Swagger, 공통 응답/예외 처리)
- [x] T-02 PostgreSQL 연결 + Flyway 설정 + V1 마이그레이션 (User, Project 테이블 — 기획서 §15.1~15.2)
- [x] T-03 Spring Security 로그인/로그아웃/me API + USER·QA 역할 분리 (기획서 §16.1, §26 작업1)
- [x] T-04 프로젝트 생성(QA)·목록·상세 API + 권한 검증 403 (기획서 §16.2)
     → status는 생성 시 항상 ACTIVE로 고정 저장 (PROGRESS.md DECISION NEEDED 답변 참고)
     → 재개 지침: AuthIntegrationTest.allowsOnlyQaToCallQaEndpoints 테스트 중
        QA 사용자가 빈 JSON({})으로 요청 시 400을 기대하나 500이 발생함.
        원인: ProjectCreateRequest의 @Valid 검증 실패(MethodArgumentNotValidException)가
        400으로 매핑되지 않고 있음. 전역 예외 핸들러(@RestControllerAdvice)에
        MethodArgumentNotValidException → 400 매핑 핸들러를 추가할 것.
        기존 예외 핸들러 클래스가 있으면 거기에 메서드만 추가하고,
        없으면 common.exception 패키지에 새로 생성할 것.
        QA 역할 제한(403) 자체는 이미 정상 동작 중이므로 건드리지 말 것.
        수정 후 ./gradlew test 전체 통과 확인할 것.
        Gradle 환경 문제(한글 경로)는 이미 해결됨 (gradle.properties 참고).
- [x] T-05 React 프로젝트 골격 (Vite+TS, 라우팅, axios, TanStack Query, 로그인 화면, 역할별 UI 분기)

## Phase 2. PDF 기획서 처리
- [x] T-06 PlanningDocument 테이블 마이그레이션 + PDF 업로드 API (형식/크기/암호화/손상 검증 — 기획서 §15.3, §16.3, §22.1)
- [x] T-07 페이지별 텍스트 추출 + 페이지 이미지 생성 + 텍스트 좌표 추출 → pageContents JSONB 저장 (기획서 §10.1)
- [x] T-08 기획서 조회 API (문서/페이지 단위) + 프론트 PDF 미리보기 화면 (기획서 §16.3, §17.5)

## Phase 3. AI 분석
- [x] T-09 AnalysisJob 테이블 + 분석 요청/조회 API 골격 (기획서 §15.4, §16.4)
- [x] T-10 AI 응답 DTO 및 JSON 스키마 전체 정의 + 역직렬화 테스트 (CategoryTree, Requirement, TestCase 10컬럼, Ambiguity, Evidence — 기획서 §11, §26 작업5)
- [x] T-11 프롬프트 리소스 파일 작성 (시스템/분류/테스트케이스/모호성/Evidence/금지 규칙 — 기획서 §20)
- [x] T-12 Spring AI ChatClient 멀티모달 호출 + 기능 분류 추출 단계 구현 (기획서 §10.2)
- [x] T-13 요구사항 추출 단계 구현 + Requirement 저장 (기획서 §10.3, §15.5)
- [x] T-14 10컬럼 테스트 케이스 생성 단계 구현 + TestCase 저장 (기획서 §10.4, §15.6)
- [x] T-15 모호한 요구사항 생성 단계 구현 + Ambiguity 저장 + 관련 TC 비고 연동 (기획서 §10.5, §15.7)
- [x] T-16 분석 결과 스키마/Enum/분류 일관성/중복 검증 + 실패 시 1회 재시도 (기획서 §10.6, §22.2)

## Phase 4. Evidence 추적
- [x] T-17 Evidence 원문 일치 검증 (EXACT/PARTIAL/SIMILAR/NOT_FOUND) + NOT_FOUND→UNSUPPORTED 처리 (기획서 §13) → 재개 지침: PROGRESS.md DECISION NEEDED 답변 참고.
        정규화(공백/줄바꿈 정리)→완전일치=EXACT→부분문자열포함=PARTIAL
        →Levenshtein 유사도>=0.7=SIMILAR→그외=NOT_FOUND(UNSUPPORTED 처리) 순서로 구현.
        비교 대상은 해당 페이지 텍스트로 한정.
- [x] T-18 Evidence 조회 API 3종 + 페이지 범위 검증 (기획서 §16.7, §13.2)
- [x] T-19 비고 자동 생성 규칙 구현 (기획서 §20.8)

## Phase 5. 검토 화면 및 승인/반려
- [x] T-20 테스트 케이스 목록 API (필터: 분류/상태/신뢰도/키워드) + 상세 API (기획서 §16.6)
- [x] T-21 프론트 테스트 시트 화면: 10컬럼 표 + 행 선택 상세 패널 + 비고 태그 (기획서 §17.7)
- [x] T-22 프론트 Evidence 연동: PDF 페이지 이동 + 원문 표시 + 다중 근거 선택 (+좌표 있으면 하이라이트) (기획서 §12.5)
- [x] T-23 개별 승인/반려 API + 확인 모달 + 반려 사유 필수 입력 (기획서 §14, §16.6, §17.8)

## Phase 6. 산출물 출력
- [x] T-24 Output + Lats_Loop_Log 테이블 마이그레이션 (기획서 §15.8~15.9)
- [x] T-25 Actor–Evaluator 루프 프레임워크 (통과 기준, 최대 3회, 서킷 브레이커, 로그 기록 — 기획서 §10.7, §22.3)
- [x] T-26 승인 테스트 케이스 CSV 생성·다운로드 (10컬럼, RFC4180, UTF-8 BOM — 기획서 §18.1, §16.9) → 재개 지침: 대상 기획서는 projectId 기준 READY 상태 중 최신 것 1개로 고정
        (PROGRESS.md DECISION NEEDED 답변 참고). READY 기획서 없으면 400 반환.
- [x] T-27 모호 요구사항 Markdown 생성·다운로드 (기획서 §18.2) → 재개 지침: T-26과 동일한 기획서 선택 규칙 적용 (PROGRESS.md DECISION NEEDED 답변 참고).
- [x] T-28 프론트 산출물 화면 + Lats_Loop_Log 진행 상태 표시 (기획서 §17.10)

## Phase 7. Jira 연동
- [x] T-29 JiraClient 인터페이스 + MockJiraClient(목업 게시) + Issue 미리보기 API (기획서 §19.2)
- [x] T-30 Jira Issue 생성 플로우: 중복 방지, Output 기록, Ambiguity 상태 갱신, 실패 재시도 (기획서 §19)
- [x] T-31 (선택/잔여 일정) 실제 Jira REST API 연동 구현체 교체 → MVP 범위에서 보류 결정 (PROGRESS.md DECISION NEEDED 답변 참고).
        MockJiraClient로 §19 요구사항 충족 완료.
## 마무리
- [x] T-32 통합 테스트: 업로드→분석→검토→CSV 전체 흐름 (기획서 §23.2)
- [x] T-33 README 작성 (실행 방법, 환경 변수, 구현/보류 범위 — 기획서 §29)
- [x] T-34 프론트 프로젝트 목록·생성 화면 + 로그인 후 랜딩 라우팅 (기획서 §17.2~17.3)
- [x] T-35 PostgreSQL 무필터 테스트 케이스 목록 조회 오류 수정
- [x] T-36 프론트 프로젝트 상세·PDF 업로드·AI 분석 요청 및 결과 탭 화면 (기획서 §17.4~17.6)
- [x] T-37 로컬 서버 PDF 업로드→AI 분석→TestCase E2E 검증 및 상태 폴링 보완
