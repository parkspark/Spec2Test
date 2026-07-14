# PROGRESS — Game QA Copilot 랄프 루프 진행 로그

- 최신 기록이 맨 위에 온다.
- 형식: "## [날짜 시각] T-XX 작업명 — DONE | BLOCKED"
- DECISION NEEDED 항목은 사람이 답을 적은 뒤 BLOCKED를 해제한다.

## DECISION NEEDED (사람 확인 대기)

- T-04: 프로젝트 생성 요청에는 status 입력이 없지만 `Project.status`는 NOT NULL이다. 생성 시 사용할 초기값과 허용 상태값을 결정해야 한다.
→ 답변: 허용값 ACTIVE/ARCHIVED, 생성 시 기본값 ACTIVE로 고정.
    상태 변경 API는 MVP 범위에 없음 (기획서 §16.2 프로젝트 수정 보류와 일치).
    T-04는 생성 시 ACTIVE로만 저장하면 됨.

---
(이하 회차 기록)

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
