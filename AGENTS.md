# Spec2Test — 프로젝트 규칙

## 기준 문서
- 유일한 요구사항 출처: specs/game-qa-copilot-final-spec.md
- 기획서와 코드가 충돌하면 기획서가 옳다. 기획서를 절대 수정하지 않는다.
- 기획서에 없는 결정이 필요하면 임의로 정하지 말고 PROGRESS.md에
  "DECISION NEEDED" 항목으로 기록하고 해당 작업을 건너뛴다.

## 기술 스택 (기획서 §5 고정)
- Backend: Java 21, Spring Boot 3.x, Spring AI, Spring Security,
  Spring Data JPA, PostgreSQL, Flyway, Gradle
- Frontend: React + TypeScript + Vite, TanStack Query,
  React Hook Form + Zod, UI 라이브러리는 shadcn/ui 하나만 사용
- DB 스키마 변경은 반드시 Flyway 마이그레이션 파일로만 수행한다.

## 외부 프론트엔드 연동 및 CORS 규칙
- 사람이 수동 개발하는 프론트엔드(React / Vite, 기본 포트 localhost:5173)와의 원활한 API 연동을 보장한다.
- CORS는 컨트롤러별 @CrossOrigin이 아니라 전역 CorsConfigurationSource Bean 하나로 설정한다.
  (컨트롤러가 계속 추가되는 랄프 루프 특성상 개별 어노테이션은 누락되기 쉽다.)
- 허용 origin은 하드코딩하지 않고 application-dev.yml 또는 환경 변수(예: CORS_ALLOWED_ORIGIN)로 분리하며,
  이 설정은 개발 환경 전용임을 주석으로 명시한다. 허용 메서드: GET, POST, PUT, DELETE, OPTIONS.
- 허용 설정 누락 시 브라우저 차단으로 인한 연동 실패가 발생하므로 절대 금지한다.

## 절대 금지 (기획서 §25.1, §29)
- 테스트 케이스 수정 / 추가 / 삭제 기능 구현 금지
- 일괄(bulk) 승인·반려 API 구현 금지 — 개별 승인/반려만
- TestCaseRevision 테이블 생성 금지
- Excel, PDF 출력 / Notion, Google Drive, GitHub 연동 구현 금지
- Entity를 API 응답으로 직접 반환 금지 (DTO 사용)
- Controller에 비즈니스 로직 작성 금지
- AI 호출을 DB 트랜잭션 내부에서 실행 금지
- AI가 자동으로 Jira Issue를 생성하는 코드 작성 금지
- 승인되지 않은 테스트 케이스를 CSV에 포함 금지
- API 키·토큰을 소스 코드에 하드코딩 금지 (환경 변수 사용)
- 구현하지 않은 기능을 동작하는 것처럼 위장(스텁에 가짜 성공 응답) 금지

## 필수 규율
- 프롬프트는 src/main/resources/prompts/ 아래 리소스 파일로 분리한다.
- 모든 AI 응답 DTO에 Evidence 필드를 포함한다.
- Evidence 원문은 백엔드에서 실존 검증한다 (EXACT/PARTIAL/SIMILAR/NOT_FOUND).
- Jira 호출부는 인터페이스(JiraClient)로 추상화하고
  1차는 목업 구현(MockJiraClient)만 작성한다.
- 테스트 케이스 출력 컬럼 순서 고정:
  No, 대분류, 중분류, 소분류, 테스트 항목, 사전조건,
  테스트 스텝, 기대결과, 기획서 원문 근거, 비고
- 새 기능에는 반드시 단위 테스트를 함께 작성한다.
- 매 회차 로컬 커밋은 필수다.
- git push, 원격 브랜치 생성/삭제 등 원격 저장소 조작은 하지 않는다. push는 사람이 직접 검토 후 수행한다.
- 커밋 메시지 형식: "[작업번호] 요약" (예: "[T-01] Spring Boot 초기 설정 및 CORS/Swagger 연동")

## .gitignore 규칙
- 루트 .gitignore는 사람이 미리 구성한 것이다. 임의로 삭제하거나 규칙을 약화시키지 않는다.
- 새로운 빌드 산출물/캐시 디렉토리가 생기면 커밋하지 말고 .gitignore에 추가한 뒤 커밋한다.

## 검증 명령
- Backend: cd backend && ./gradlew test
- Frontend: cd frontend && npm run build && npm test -- --run
- 위 명령이 실패한 상태로 커밋하지 않는다.