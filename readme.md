# Spec2Test

PDF 게임 기획서를 분석해 기능 분류, 요구사항, 10컬럼 QA 테스트 케이스와 모호한 요구사항을 생성하고, 원문 Evidence 검증과 개별 승인·반려를 거쳐 CSV/Markdown 산출물을 만드는 MVP입니다.

요구사항의 유일한 기준은 [`specs/game-qa-copilot-final-spec.md`](specs/game-qa-copilot-final-spec.md)입니다.

## 기술 스택

- Backend: Java 21, Spring Boot 3.5, Spring AI, Spring Security, Spring Data JPA, Flyway, PostgreSQL, Gradle
- Frontend: React 19, TypeScript, Vite, TanStack Query, React Hook Form, Zod, shadcn/ui

## 사전 준비

- Java 21
- Node.js와 npm
- PostgreSQL
- OpenAI 호환 Chat API 키와 사용할 모델명

기본 DB 이름은 `spec2test`, 기본 사용자는 `postgres`입니다. 데이터베이스와 접속 사용자는 실행 전에 직접 준비해야 하며, 스키마는 백엔드 시작 시 Flyway가 생성합니다.

## 환경 변수

### Backend

| 변수 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `DB_URL` | 아니요 | `jdbc:postgresql://localhost:5432/spec2test` | PostgreSQL JDBC URL |
| `DB_USERNAME` | 아니요 | `postgres` | DB 사용자 |
| `DB_PASSWORD` | 예 | 없음 | DB 비밀번호 |
| `OPENAI_API_KEY` | 예 | 없음 | Spring AI Chat API 키 |
| `AI_MODEL` | 예 | 없음 | 분석에 사용할 모델명 |
| `SPRING_PROFILES_ACTIVE` | 개발 시 예 | 없음 | 분리 실행하는 프론트의 CORS 허용을 위해 `dev` 사용 |
| `CORS_ALLOWED_ORIGIN` | 아니요 | `http://localhost:5173` | `dev` 프로필 전용 허용 Origin |
| `APP_DOCUMENT_STORAGE_PATH` | 아니요 | `./data/documents` | 업로드 PDF와 페이지 이미지 저장 경로 |

API 키와 비밀번호는 소스에 저장하지 않습니다.

### Frontend

| 변수 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `VITE_API_URL` | 아니요 | `http://localhost:8080` | Backend API 주소 |

## 실행 방법

PowerShell 예시입니다.

### 1. PostgreSQL 준비

```sql
CREATE DATABASE spec2test;
```

사용자 생성 API는 MVP 범위에 없습니다. 로그인할 USER/QA 계정은 운영 환경에서 BCrypt 비밀번호 해시와 함께 `users` 테이블에 별도로 등록해야 합니다.

### 2. Backend 실행

```powershell
cd backend
$env:DB_PASSWORD = '<db-password>'
$env:OPENAI_API_KEY = '<api-key>'
$env:AI_MODEL = '<model-name>'
$env:SPRING_PROFILES_ACTIVE = 'dev'
./gradlew bootRun
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend 실행

다른 터미널에서 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

화면은 `http://localhost:5173`에서 열립니다.

## 검증

```powershell
cd backend
./gradlew test
```

```powershell
cd frontend
npm run build
npm test -- --run
```

Backend 테스트는 H2의 PostgreSQL 호환 모드를 사용하므로 로컬 PostgreSQL이나 AI API 키 없이 실행됩니다.

## 구현 범위

- 세션 기반 로그인·로그아웃·현재 사용자 조회와 USER/QA 권한 분리
- QA 프로젝트 생성, 프로젝트 목록·상세 조회
- PDF 업로드와 형식·크기·암호화·손상 검증
- 페이지별 텍스트·이미지·텍스트 좌표 추출 및 조회
- Spring AI 구조화 응답 기반 기능 분류 → 요구사항 → 테스트 케이스 → 모호성 분석 파이프라인
- 10컬럼 테스트 케이스와 Evidence 저장, 페이지 범위 및 원문 일치 검증(`EXACT`, `PARTIAL`, `SIMILAR`, `NOT_FOUND`)
- 테스트 케이스 목록·상세·필터, PDF 원문 대조와 좌표 하이라이트
- 테스트 케이스 개별 승인·반려와 반려 사유 저장
- 승인된 테스트 케이스만 포함하는 RFC 4180 CSV 생성·다운로드
- 모호한 요구사항 Markdown 생성·다운로드
- Actor–Evaluator 최대 3회 검증과 `Output`/`Lats_Loop_Log` 이력 조회
- `JiraClient` 추상화, `MockJiraClient` 기반 Issue 미리보기·명시적 게시·중복 방지·실패 재시도

## 보류 및 제외 범위

- 실제 Jira REST API 연동: Jira Cloud/Data Center, 인증 방식과 대상 인스턴스가 확정될 때 별도 스펙으로 진행
- 테스트 케이스 수정·직접 추가·삭제와 수정 이력
- 테스트 케이스 일괄 승인·반려 및 상태 되돌리기
- 승인 테스트 케이스 Markdown 출력
- Excel, PDF 산출물
- Notion, Google Drive, GitHub 연동
- DOCX 등 PDF 외 기획서 업로드와 이미지 단독 업로드
- 게임 클라이언트 자동 테스트 및 테스트 자동화 코드 생성
- 복잡한 조직·팀·관리자 권한과 분석 결과 버전 비교

Jira Issue는 AI가 자동 생성하지 않으며 QA 사용자의 명시적 요청으로만 목업 게시됩니다.
