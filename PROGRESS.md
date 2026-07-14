# PROGRESS — Game QA Copilot 랄프 루프 진행 로그

- 최신 기록이 맨 위에 온다.
- 형식: "## [날짜 시각] T-XX 작업명 — DONE | BLOCKED"
- DECISION NEEDED 항목은 사람이 답을 적은 뒤 BLOCKED를 해제한다.

## DECISION NEEDED (사람 확인 대기)


---
(이하 회차 기록)

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
