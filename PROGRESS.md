# PROGRESS — Game QA Copilot 랄프 루프 진행 로그

- 최신 기록이 맨 위에 온다.
- 형식: "## [날짜 시각] T-XX 작업명 — DONE | BLOCKED"
- DECISION NEEDED 항목은 사람이 답을 적은 뒤 BLOCKED를 해제한다.

## DECISION NEEDED (사람 확인 대기)
(없음)

---
(이하 회차 기록)

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
