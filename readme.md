# 설계·계약 문서 (Day 2 산출물) — 팀 랄먹

---

> 팀 레포에 `docs/design.md`(또는 README 설계 섹션)로 저장.
프론트·백이 **이 문서 하나로** 병렬 개발한다 — 변수명·타입·성공/실패 코드가 100% 일치해야 한다.
> 

---

## 0. 메타

| 항목 | 내용 |
| --- | --- |
| 팀 | 랄먹 |
| 작성일 | 2026-07-13 |
| 관련 Issue / PR | #1 |
| 스택 | Java 21 · Spring Boot 4.1.0 · Spring AI 2.0.0 · Gemini(`gemini-3.1-flash-lite`) · React(Vite) · base 패키지 `com.study` |

---

## 1. 문제·사용자 (한 문장)

- **사용자**: 게임 개발사 및 퍼블리셔의 **QA(Quality Assurance) 담당자 및 테스트 엔지니어**
- **문제**: 줄글 형태의 긴 게임 기획서를 읽고 실무용 테스트 케이스 표를 손으로 한땀 한땀 작성하는 작업이 너무 오래 걸리고, 기획서의 누락·모호성 검증을 수동으로 추적하기 어려움
- **한 문장 정의**:

> "우리 팀은 **게임 QA 담당자**를 위해 **기획서 기반 테스트 케이스 작성 및 기획 모호성 검증의 번거로움**을 **멀티모달 PDF 분석 및 실존 근거(Evidence) 추적 인공지능**으로 풀어 준다."
> 

---

## 2. 기능 범위 — MoSCoW

| 구분 | 기능 |
| --- | --- |
| **Must** (3~5, 절대 사수) | ① PDF 기획서 업로드 및 파일 데이터화 (텍스트/이미지 추출)
||② AI 기반 10컬럼 실무형 QA 테스트 케이스 생성 파이프라인
||③ 테스트 케이스별 원문 근거(Evidence) 매핑 및 백엔드 실존 검증
||④ 기획서 내 모호한 요구사항 발굴 및 마크다운 다운로드
||⑤ 테스트 케이스 개별 승인/반려 처리 및 최종 CSV 내보내기 |
| **Should** (시간 허락 시) | Jira REST API 실연동을 통한 모호 요구사항 티켓 발행, PDF 원문 영역 하이라이팅 |
| **Could** (과감히 포기) | 테스트 케이스 자체의 수정/추가/삭제 기능, Excel(xlsx) 출력, Notion/GitHub 연동 |
- **오늘(Day2) 코드로 증명할 1개**: PDF 텍스트 조각을 입력받아 규칙에 맞는 구조화된 10컬럼 테스트 케이스 데이터(JSON) 1회 생성

---

## 3. 핵심 시나리오 (MVP 한 흐름)

| 구분 | 흐름 |
| --- | --- |
| 입력 (User) | 사용자가 특정 기획서 상세 페이지에서 [AI 분석 가동] 버튼을 클릭 |
| 전송 (System) | React가 백엔드 API로 `documentId`와 분석 옵션을 JSON으로 전송 |
| 처리 (System) | Spring AI가 파싱된 기획서 본문과 프롬프트를 조합해 Gemini에 전달, 구조화된 테스트 케이스 및 원문 근거(Evidence) 리스트를 생성 및 자가 검증 |
| 저장 (System) | 생성된 테스트 케이스, 모호 요구사항 데이터 및 검증 로그(`Lats_Loop_Log`)를 DB에 저장 |
| 출력 (System) | 화면의 가로 스크롤 표에 고정 포맷의 10컬럼 테스트 시트가 렌더링되고 우측에 원문 대조 패널 표시 |

---

## 4. 아키텍처 · 책임 분리 (Separation of Concerns)

```
React (Vite) ──HTTP──> Spring Boot (com.study) ──> Spring AI ──> Gemini
                            │
                            └──> DB (outputs, lats_loop_log 등)
```

| 레이어 | 책임 |
| --- | --- |
| **React** | PDF 및 분석 상태 로딩 관리, 10컬럼 시트 가로 스크롤 및 상세 대조 패널 UI 렌더링, 승인/반려 액션 처리 |
| **Controller**  | 기획서 업로드(Multipart) 및 분석 요청 수신, QA 권한 체크, DTO 매핑 및 유효성 검증 |
| **Service** | Actor-Evaluator 루프 제어, 구조화 출력(`.entity`) 호출, 기획서 본문 일치도 백엔드 검증 로직 실행 |
| **Spring AI & Repository** | Gemini 모델 연결, 생성된 테스트 케이스 및 단계별 루프 이력(`Lats_Loop_Log`) DB CRUD 수행 |

---

## 5. API 계약 ⭐ (핵심)

### 5.1 엔드포인트 목록

| Method | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/api/documents/{documentId}/analyses` | 특정 기획서에 대한 AI 테스트 케이스 생성 파이프라인 가동 |
| `GET`  | `/api/projects/{projectId}/test-cases` |  생성된 10컬럼 테스트 케이스 목록 조회 (필터링 및 검색) |
| `POST`  | `/api/test-cases/{testCaseId}/approve` | 개별 테스트 케이스 승인 처리 |
| `POST`  | `/api/test-cases/{testCaseId}/reject` | 개별 테스트 케이스 반려 처리 (반려 사유 필수) |

### 5.2 상세 — `POST /api/documents/{documentId}/analyses`

**요청 (Request Body)**

```json
{
  "options": "FULL_ANALYSIS"
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `options` | `string` | ✅ | 분석 범위 및 옵션 지정 (`FULL_ANALYSIS`, `QUICK_ANALYSIS` 등) |

|

- *응답 — 성공 `200 OK**`

```json
{
  "analysisId": 45,
  "status": "SUCCESS",
  "generatedCount": 15,
  "loopCount": 2
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `analysisId` | `Long` | 생성된 분석 마스터 ID |
| `status`  | `string`  | 최종 파이프라인 상태 (`SUCCESS`, `FAILED`) |
| `generatedCount`  | `int` | 추출 및 생성 완료된 테스트 케이스 총 개수 |
| `loopCount`  | `int`  | Actor-Evaluator 자가 교정 루프가 실행된 횟수 |

**응답 — 오류 (오류 형태도 계약)**

| 상태 코드 | 상황 | 응답 예시 |
| --- | --- | --- |
| `400 Bad Request` | 필수 파라미터가 누락되었거나 없는 문서 ID일 때 | `{ "message": "존재하지 않는 기획서 ID입니다." }`<br> |
| `403 Forbidden` | QA 권한이 없는 일반 USER 계정이 요청했을 때 | `{ "message": "AI 분석 요청 권한이 없습니다. QA 역할을 확인하세요." }`<br> |
| `500 Internal Server Error` | AI 호출 실패 또는 프롬프트 오류 발생 시 | `{ "message": "AI 분석 중 오류가 발생했습니다. (LLM Timeout)" }` (DB 저장은 **롤백**) |

---

## 6. 데이터 모델 (DB 스키마)

**`outputs`** (최종 산출물 관리 전용 테이블)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long (PK, auto) | 식별자 |
| `project_id`  | Long (FK | 연관 프로젝트 ID |
| `output_type` | VARCHAR  | 산출물 유형 (`CSV_EXPORT`, `MARKDOWN_EXPORT`, `JIRA_ISSUE`) |
| `status`  | VARCHAR  | 생성 처리 상태 (`PENDING`, `SUCCESS`, `FAILED`) |
| `final_content`  | TEXT  | 최종 확정된 데이터 내용 또는 JSON 덤프 |
| `createdAt`  | LocalDateTime | 생성 시각 |

**`lats_loop_log`** (AI 자가 검증 및 교정 프로세스 이력 로그 테이블)

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long (PK, auto) | 식별자 |
| `output_id`  | Long (FK) | 연관 산출물 ID |
| `depth_step`  | INT  | 루프 차수 (1회차, 2회차...) |
| `generated_draft`  | TEXT  | 해당 회차에 AI가 생성했던 초안 텍스트 |
| `evaluation_score`  | INT  | 자가 평가 점수 |
| `evaluation_feedback`  | TEXT  | 자가 피드백 내용 |
| `createdAt`  | LocalDateTime  | 기록 시각 |

---

## 7. AI 연동 설계

### 7.1 프롬프트 해부학

`[시스템 역할]` + `[작업 지시]` + `[출력 형식]` + `[사용자 입력]` = 완성된 프롬프트

| 파트 | 내용 |
| --- | --- |
| 역할 | "당신은 게임 전문 QA 엔지니어이자 기획 분석 전문가입니다." |
| 작업  | "입력된 게임 기획서 본문을 분석하여 대/중/소분류를 계층형으로 나누고, 사전조건, 테스트 스텝, 기대결과가 명확한 실무형 테스트 케이스를 추출하세요. 기획서에 직접적인 근거가 없는 경우 비고란에 '기획서 직접 근거 없음' 태그를 명시해야 합니다." |
| 출력 형식 |  "`displayOrder`, `categoryLarge`, `categoryMedium`, `categorySmall`, `testItem`, `preCondition`, `testSteps`, `expectedResult`, `evidenceText`, `evidencePage`, `notes` 를 포함하는 **JSON 배열**로 반환하세요." |
| 사용자 입력 (동적) | `{documentText}` — 파싱되어 넘어온 게임 기획서 원문 텍스트 스트림 |

### 7.2 구조화 출력 (평문 아님)

- AI 반환 형식 (record): `TestCaseDraft(int displayOrder, String categoryLarge, String categoryMedium, String categorySmall, String testItem, String preCondition, String testSteps, String expectedResult, String evidenceText, int evidencePage, String notes)`
- `.content()`(평문) ❌ → `.entity(TestCaseDraftList.class)` ✅
- **필드 매핑**: AI가 채워준 응답 객체의 데이터들을 검증 과정을 거친 후 백엔드의 `test_cases` 테이블 컬럼으로 1:1 보관 및 매핑함

### 7.3 Spring AI 기능 매핑

| 우리 기능 | 쓰는 Spring AI 기능 |
| --- | --- |
| 10컬럼 테스트 시트 데이터 추출 | 챗 (ChatClient) |
| 정형화된 리스트 데이터 파싱 | 구조화 출력 (`.entity`) |
| 자가 검증 루프 구현 | 대화 메모리 및 체이닝 구조를 활용한 Actor-Evaluator 다회차 검증 프롬프팅 |

---

## 8. 예외 설계 (실패를 먼저 설계)

| 입력 | 시스템 반응 |
| --- | --- |
| 분석 요청 시 `documentId` 누락 | `400 Bad Request` + "필수 기획서 식별자가 누락되었습니다." |
|  Gemini API 네트워크 장애 또는 할당량 초과 | `500 Internal Server Error` → 현재 트랜잭션 롤백, `outputs` 테이블의 상태를 `FAILED`로 변경하고 실패 사유(`failure_reason`) 기록 |
| 생성된 원문 근거가 실제 PDF 텍스트 내에 존재하지 않음 (환각 발생) | 백엔드 내 스트링 매칭 검증기에서 일치도를 `NOT_FOUND`로 판단하고, 해당 건에 대해 자가 교정을 지시하거나 프론트에 경고 비고 태그 부착 |

---

## 9. 오늘의 완료 기준 (Execution Checklist)

- [x]  1. 해결할 문제와 사용자가 **한 문장**으로 정의됨 (§1)
- [x]  2. **Must 기능(3~5)** + 핵심 시나리오 확정 (§2·§3)
- [x]  3. **API 계약(URL·Method·JSON·오류)** 문서화 (§5)
- [ ]  4. **(가장 중요) Spring AI 핵심 호출이 코드로 1회 성공** (§7 기반, `curl`로 확인)

```bash
# §5 계약대로 호출 확인
curl -X POST <http://localhost:8080/api/documents/1/analyses> \
  -H "Content-Type: application/json" \
  -d '{"options":"FULL_ANALYSIS"}'
```

> 각자 만든 코드가 아니라 **연결되어 실행되는 하나의 서비스**가 결과물이다.
> 

---

## 10. Use case Diagram, Sequence Diagram