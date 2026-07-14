# Spec2Test 최종 기획서 v1.0

> 본 문서는 v0.1과 v0.2를 병합한 최종 기획서다.
> **v0.2의 결정이 우선하며, v0.2에 없는 내용은 v0.1을 따른다.**
> 이 문서는 랄프 코딩(Ralph Loop) 방식의 자동화 구현에 직접 입력되는 단일 기준 문서(Single Source of Truth)다.

---

# 0. 병합 결정 사항

버전 간 충돌 및 v0.2 내부 불일치를 아래와 같이 확정한다. 팀 검토 후 이견이 있으면 이 표만 수정하면 된다.

| # | 항목 | v0.1 | v0.2 | 최종 결정 |
| --- | --- | --- | --- | --- |
| 1 | 테스트 케이스 출력 구조 | title/category 중심 | 실무형 10컬럼 테스트 시트 | **10컬럼 시트** (No, 대분류, 중분류, 소분류, 테스트 항목, 사전조건, 테스트 스텝, 기대결과, 기획서 원문 근거, 비고) |
| 2 | 승인된 테스트 케이스 산출물 | Markdown | §1·§3.1은 CSV, §14·§16은 Markdown (내부 불일치) | **CSV 다운로드가 기본.** CSV는 10컬럼 순서를 유지한다. Markdown 표 출력은 v0.2 §14 형식을 따르는 **선택(후순위) 기능**으로 보류 범위에 둔다. |
| 3 | 모호한 요구사항 산출물 | Jira Issue만 | Markdown 다운로드 + Jira Issue | **Markdown 다운로드 + Jira Issue 생성** 둘 다 지원 |
| 4 | 일괄 승인/반려 API | bulk-approve / bulk-reject 포함 | 해성 의견: 승인/반려는 케이스별 개별 처리 | **개별 승인/반려만 구현.** bulk API는 보류 범위로 이동 |
| 5 | 원문 근거 하이라이트 | 우선순위 1 기능 | 해성 의견: 선택사항 | **선택(Optional) 기능.** 페이지 이동 + 원문 텍스트 표시는 필수, boundingBox 하이라이트는 좌표 확보 시에만 |
| 6 | Output 테이블 구조 | 단일 Output 테이블 | Output + Lats_Loop_Log 2테이블 분리 권장 | **2테이블 분리 채택.** 랄프 루프(Actor–Evaluator) 검증 이력을 Lats_Loop_Log에 기록 |
| 7 | Jira 연동 방식 | Tool Calling 또는 MCP로 직접 연동 | ADF 포맷팅 리스크 언급, 목업 폴백 제안 | **1차: Jira 유사 목업 화면으로 Issue 미리보기/게시 → 2차: 잔여 일정에 실제 Jira API 연동.** Output 테이블 구조는 두 경우 모두 동일하게 사용 |
| 8 | 테스트 시트 화면 상세 표시 방식 | 좌측 목록 + 상세 패널 고정 | 해성 의견: 구현 상황 보고 재결정 | 기본 골격(표 + PDF 원문 동시 표시)만 확정, 세부 레이아웃은 **구현 중 재조정 가능** |
| 9 | 분석 파이프라인 | 요구사항 → 테스트 케이스 | 기능 분류(카테고리 트리) 추출 단계 선행 | **분류 추출 → 요구사항 → 테스트 케이스 → 모호성** 순서 확정 |

---

# 1. 프로젝트 개요

## 1.1 프로젝트명

**Spec2Test**

## 1.2 한 줄 소개

이미지와 텍스트가 포함된 PDF 게임 기획서를 AI가 분석하여 실무형 QA 테스트 케이스 표를 생성하고, 각 테스트 케이스가 기획서의 어떤 내용을 근거로 작성되었는지 추적한 뒤 QA 담당자가 승인 또는 반려할 수 있는 시스템

## 1.3 프로젝트 정의

Spec2Test은 PDF 형식의 게임 기획서를 분석하여 다음 결과를 생성한다.

- 기능 요구사항
- QA 테스트 케이스 (정상·경계값·예외 포함)
- 모호하거나 누락된 요구사항
- 기획자에게 확인할 질문
- 테스트 케이스별 기획서 원문 근거(Evidence)

생성된 테스트 케이스는 실제 QA 문서와 유사한 표 형태로 제공한다.

| No | 대분류 | 중분류 | 소분류 | 테스트 항목 | 사전조건 | 테스트 스텝 | 기대결과 | 기획서 원문 근거 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

AI 결과는 자동 확정하지 않는다. QA 사용자가 테스트 케이스와 기획서 원문을 함께 검토한 후 승인 또는 반려하며, **승인된 테스트 케이스만 CSV 문서로 출력**한다. 모호한 요구사항은 Markdown으로 내려받거나 Jira Issue로 생성할 수 있다.

## 1.4 핵심 차별점 (v0.1 계승)

이 프로젝트의 핵심은 단순한 테스트 케이스 생성이 아니다. AI가 생성한 결과와 기획서 원문 사이의 연결 관계를 제공하여 사용자가 다음을 직접 검증할 수 있게 한다.

```text
이 테스트 케이스는 왜 생성되었는가?
        ↓
기획서의 어떤 문장을 근거로 생성되었는가?
        ↓
원문에 명시된 내용인가, AI가 추론한 내용인가?
```

따라서 시스템은 테스트 케이스마다 다음 정보를 제공한다.

- 기획서 페이지 번호
- 근거가 된 원문
- 원문의 위치 또는 영역
- 근거 유형 (EXPLICIT / INFERRED / UNSUPPORTED)
- 원문 검증 상태 (EXACT / PARTIAL / SIMILAR / NOT_FOUND)
- AI 추론 여부
- 신뢰도
- 관련 요구사항

추가로, AI 생성 과정 자체에 **Actor–Evaluator 자기 검증 루프**를 적용하고 그 이력을 저장하여, "AI가 스스로 검증하고 교정한 과정"을 프론트에서 시각화한다. (§16.5 Lats_Loop_Log)

---

# 2. 프로젝트 핵심 목표 (v0.1 계승)

1. PDF 형식의 게임 기획서를 AI가 분석할 수 있도록 처리한다.
2. 기획서에서 게임 기능의 계층 구조(대분류/중분류/소분류)를 추출한다.
3. 기획서에서 테스트 가능한 기능 요구사항을 추출한다.
4. 정상, 경계값, 예외 상황을 포함한 실무형 QA 테스트 케이스를 생성한다.
5. 기획서에서 정의되지 않은 모호하거나 누락된 요구사항을 탐지한다.
6. 각 요구사항과 테스트 케이스가 기획서의 어떤 내용을 근거로 생성되었는지 추적한다.
7. AI가 반환한 근거가 실제 기획서에 존재하는지 백엔드에서 검증한다.
8. QA 사용자가 AI 결과를 검토하고 승인 또는 반려할 수 있게 한다.
9. 승인 및 반려 결과를 데이터베이스에 저장한다.
10. 승인된 테스트 케이스를 CSV 문서로 출력한다.
11. 모호한 요구사항을 Markdown으로 출력하거나 Jira Issue로 생성한다.
12. AI 생성–평가 루프의 이력을 저장하고 시각화한다.

---

# 3. 구현 범위

## 3.1 포함 범위

- 사용자 로그인
- USER와 QA 역할 분리
- 프로젝트 생성 및 조회
- PDF 기획서 업로드
- PDF 텍스트 추출
- PDF 페이지 이미지 처리
- 텍스트 위치(좌표) 정보 추출 (가능한 범위 내)
- AI 기반 기능 분류(카테고리 트리) 추출
- AI 기반 요구사항 추출
- 실무형 테스트 케이스 표(10컬럼) 생성
- 테스트 케이스별 Evidence 연결
- Evidence 원문 실존 여부 백엔드 검증
- 모호하거나 누락된 요구사항 생성
- 테스트 케이스와 PDF 원문 동시 조회
- 테스트 케이스 개별 승인 / 개별 반려
- 승인 및 반려 결과 DB 저장
- 산출물 다운로드
  - 승인된 테스트 케이스 **CSV** 다운로드
  - 모호한 요구사항 **Markdown** 다운로드
- 모호한 요구사항 Jira Issue 생성 (1차: Jira 유사 목업, 2차: 실제 연동)
- AI 생성 루프(Actor–Evaluator) 이력 저장 및 진행 상태 표시

## 3.2 보류 범위

현재 MVP에서 구현하지 않는다.

- AI 생성 테스트 케이스 수정
- 사용자의 테스트 케이스 직접 추가
- 테스트 케이스 삭제
- 테스트 케이스 수정 이력 관리 (`TestCaseRevision` 테이블 생성 금지)
- 테스트 케이스 일괄 승인 / 일괄 반려 (bulk API)
- 승인된 테스트 케이스의 Markdown 표 출력 (CSV가 기본, 여유 시 추가)
- Excel(xlsx) 다운로드
- PDF 다운로드
- Notion 연동
- Google Drive 연동
- GitHub Issue 연동
- DOCX 등 PDF 외 형식의 기획서 업로드
- 이미지 파일 단독 업로드
- 실제 게임 클라이언트 자동 테스트
- 테스트 자동화 코드 생성
- 복잡한 조직 및 팀 권한 관리
- AI 생성 결과의 버전 비교
- 승인/반려 후 상태 되돌리기 (향후 버전에서 재검토 기능 추가 가능)

---

# 4. 사용자 역할

## 4.1 일반 사용자

역할 코드: `USER`

일반 사용자는 조회 기능만 사용할 수 있다.

**가능 기능**

- 프로젝트 목록 조회 / 상세 조회
- 기획서 조회
- AI 분석 결과 조회
- 테스트 케이스 조회
- 테스트 케이스 원문 근거 조회
- 모호한 요구사항 조회
- 승인된 테스트 문서(CSV) 조회 및 다운로드

**불가능 기능**

- 프로젝트 생성
- PDF 기획서 업로드
- AI 분석 요청
- 테스트 케이스 승인 / 반려
- 산출물 생성 요청 (CSV/Markdown 생성 트리거)
- Jira Issue 생성

> 참고: v0.1은 USER의 다운로드를 허용하고 v0.2 §2.1은 "Markdown 생성 및 다운로드"를 불가로 표기한다. **"생성 요청"은 QA 전용, 이미 생성된 산출물의 조회·다운로드는 USER도 가능**한 것으로 확정한다.

## 4.2 QA 사용자

역할 코드: `QA`

**가능 기능**

- 프로젝트 생성 및 조회
- PDF 기획서 업로드
- AI 분석 요청
- 테스트 케이스 조회 및 원문 근거 조회
- 테스트 케이스 개별 승인 / 개별 반려
- 승인된 테스트 케이스 CSV 생성 및 다운로드
- 모호한 요구사항 Markdown 생성 및 다운로드
- 모호한 요구사항 Jira Issue 생성

현재 MVP에서는 별도의 관리자 역할을 두지 않는다.

---

# 5. 기술 스택 (v0.1 계승)

## 5.1 백엔드

- Java 21, Spring Boot 3.x
- Spring Web MVC, Spring Security, Spring Data JPA, Spring Validation
- Spring AI (`ChatClient`)
- PostgreSQL, Flyway
- Jackson, Lombok
- OpenAPI / Swagger
- Gradle

## 5.2 프론트엔드

- React + TypeScript + Vite
- React Router, TanStack Query, React Hook Form, Zod, Axios
- MUI 또는 shadcn/ui — **하나만 선택하여 사용**

## 5.3 AI

- Spring AI `ChatClient`
- 멀티모달 모델 (PDF 텍스트 + 페이지 이미지 입력)
- JSON Schema 기반 구조화 출력
- 프롬프트 템플릿 (리소스 파일로 분리)
- Actor–Evaluator 자기 검증 루프
- Jira 연동을 위한 Tool Calling 또는 MCP (2차 단계)

## 5.4 데이터베이스

- PostgreSQL
- 주요 도메인 데이터는 관계형 컬럼으로 저장
- AI 원본 응답과 분석 부가 정보는 JSONB 활용
- 테스트 케이스와 원문 근거를 관계형/JSONB 혼합 구조로 연결
- 분류·검색에 사용하는 값은 JSONB 내부에만 저장하지 않는다

## 5.5 파일 처리

- 업로드 형식: PDF만 허용
- PDF 원본 파일 저장
- 페이지 단위 텍스트 추출
- 페이지 단위 이미지(렌더링) 생성
- 텍스트 위치 정보(boundingBox) 추출
- AI 분석 시 텍스트와 이미지 정보 함께 전달
- 산출물: CSV(테스트 케이스), Markdown(모호 요구사항)

---

# 6. 전체 시스템 흐름

```text
[사용자 로그인]
       ↓
[사용자 역할 확인]
       ↓
[QA 프로젝트 생성]
       ↓
[PDF 기획서 업로드]
  ├─ 텍스트 추출
  ├─ 페이지 이미지 생성
  └─ 텍스트 위치 정보 추출
       ↓
[기획서 원문 데이터 저장]
       ↓
[AI 분석 요청]
       ↓
[기능 분류 추출: 대분류 / 중분류 / 소분류]
       ↓
[기능 요구사항 추출]
       ↓
[QA 테스트 케이스 생성 (10컬럼)]
       ↓
[모호한 요구사항 생성]
       ↓
[테스트 케이스와 기획서 원문 근거 연결]
       ↓
[백엔드 Evidence 검증 (원문 실존 / 페이지 범위)]
       ↓
[AI 생성 결과 GENERATED 상태로 저장]
       ↓
[QA 사용자 검토]
  ├─ 승인
  └─ 반려
       ↓
[승인 및 반려 결과 DB 저장]
       ↓
  ├─ 승인된 테스트 케이스 CSV 다운로드
  ├─ 모호한 요구사항 Markdown 다운로드
  └─ 모호한 요구사항 Jira Issue 생성
```

---

# 7. 주요 사용자 시나리오 (v0.1 계승, v0.2 반영 수정)

## 7.1 QA 사용자의 기획서 분석

1. QA 사용자가 로그인한다.
2. 새 프로젝트를 생성한다.
3. 프로젝트 상세 화면에서 PDF 기획서를 업로드한다.
4. 시스템은 PDF의 텍스트, 페이지 이미지, 텍스트 위치를 처리한다.
5. QA 사용자가 AI 분석을 요청한다.
6. AI는 기능 분류 구조(대분류/중분류/소분류)를 먼저 추출한다.
7. AI는 기능 요구사항을 추출한다.
8. AI는 요구사항을 기반으로 10컬럼 테스트 케이스를 생성한다.
9. AI는 누락되거나 모호한 요구사항을 생성한다.
10. 시스템은 각 결과와 원문 근거를 연결하고 백엔드에서 검증한다.
11. 분석 결과를 `GENERATED` 상태로 저장한다.

## 7.2 테스트 케이스 근거 검토

1. QA 사용자가 분석 결과 화면(테스트 시트)을 연다.
2. 테스트 케이스 표에서 특정 행을 선택한다.
3. 화면에 다음이 표시된다: 테스트 케이스 상세, 관련 요구사항, 기획서 원문, 근거 문장, 페이지 번호, 근거 유형, 원문 검증 상태, 신뢰도, 비고 태그.
4. PDF 미리보기가 근거 페이지로 이동하고, 좌표가 있으면 근거 영역을 강조한다(선택 기능).
5. QA 사용자는 원문과 생성 결과를 비교한다.
6. 타당하면 승인, 부정확하거나 불필요하면 사유를 입력하고 반려한다. **승인/반려는 케이스별로 개별 처리한다.**

## 7.3 CSV 다운로드

1. 사용자가 승인된 테스트 케이스 목록을 조회한다.
2. CSV 다운로드 버튼을 선택한다. (생성 요청은 QA, 다운로드는 USER/QA)
3. 시스템은 승인된 테스트 케이스만 포함한 CSV를 10컬럼 순서로 생성한다.
4. 사용자는 CSV 파일을 다운로드한다.

## 7.4 모호한 요구사항 처리

1. QA 사용자가 모호한 요구사항 목록을 조회한다.
2. Markdown 다운로드로 전체 목록을 문서화하거나, 개별 항목을 Jira Issue로 생성한다.
3. Jira Issue 생성 전 시스템은 Issue 제목과 내용을 미리 보여준다.
4. QA 사용자가 생성 요청을 하면 Issue가 생성되고 키와 URL을 저장한다.
5. Jira 실연동 이전 단계에서는 Jira 유사 목업 화면에 게시된다.

---

# 8. 테스트 케이스 출력 형식 (v0.2 확정)

## 8.1 최종 출력 컬럼 (순서 고정)

```text
No / 대분류 / 중분류 / 소분류 / 테스트 항목 / 사전조건 / 테스트 스텝 / 기대결과 / 기획서 원문 근거 / 비고
```

### 1. No

화면 및 문서 출력 순서. 데이터베이스 기본키와 구분한다. DB 내부에서는 `id`를 식별자로 사용하고 화면과 문서에서는 `displayOrder`를 `No`로 표시한다.

### 2. 대분류

테스트 대상이 되는 최상위 기능 또는 화면 영역. **테스트 유형이 아니라 게임 기능의 최상위 분류다.**

```text
게임 진입 - 인덱스 UI / 궁술 훈련 / 상점 / 인벤토리 / 출석 보상
```

### 3. 중분류

대분류 아래의 세부 기능 흐름 또는 기능 그룹. 적절한 중분류가 없으면 `-`로 표시한다.

```text
진행 흐름 / 보상 지급 / UI 표시 / 구매 처리 / 장비 관리
```

### 4. 소분류

실제 테스트 대상이 되는 세부 기능이나 화면 요소.

```text
INDEX 제목 / 훈련장 로비 / 화살 출발 / 시작 카운트다운 / 인벤토리 지급
```

### 5. 테스트 항목

검증하려는 내용을 한 문장으로 표현한다. **하나의 명확한 검증 목적만 가진다.**

```text
좋은 예: INDEX 제목 표시 / 배경 색상 확인 / 무료 뽑기 횟수 초기화 확인
잘못된 예: 제목, 버튼, 배경 색상 및 애니메이션 확인
```

### 6. 사전조건

테스트 수행 전 만족해야 할 조건. 필요 없으면 `-`. 여러 조건은 번호 목록으로 줄바꿈 표현한다.

```text
1. 사용자가 로그인되어 있다.
2. 오늘 무료 뽑기를 사용하지 않았다.
3. 인벤토리에 빈 공간이 있다.
```

### 7. 테스트 스텝

사용자가 실제로 수행하는 동작을 순서대로 작성한다. 각 스텝은 `순서 / 수행 동작 / 단계별 예상 결과`를 가질 수 있으며, 최종 표에서는 하나의 셀 안에 번호 목록으로 표시한다.

### 8. 기대결과

스텝 수행 후 확인해야 하는 **관찰·검증 가능한** 결과. 다음 표현만 단독 사용 금지: "정상적으로 표시된다", "정상적으로 처리된다", "문제없이 작동한다".

### 9. 기획서 원문 근거

기본 표시 형식:

```text
[p.3 / 무료 뽑기 정책 / EXPLICIT]
"모든 유저는 하루에 한 번 무료 뽑기를 할 수 있다."
```

여러 근거가 연결된 경우 번호 목록으로 표시한다. 프론트에서 근거를 클릭하면 해당 PDF 페이지(및 가능하면 위치)로 이동한다.

### 10. 비고

테스트 수행·검토 시 참고 사항. 예:

```text
기획 확인 필요 / 초기화 시간 기준 미정 / AI 추론 포함 / 원문 위치 확인 필요 / 관련 Jira Issue: GAME-124
```

Evidence를 찾지 못한 경우:

```text
기획서 직접 근거 없음
QA 검토 필수
```

## 8.2 테스트 케이스 예시

| No | 대분류 | 중분류 | 소분류 | 테스트 항목 | 사전조건 | 테스트 스텝 | 기대결과 | 기획서 원문 근거 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 게임 진입 - 인덱스 UI | - | INDEX 제목 | INDEX 제목 표시 | 게임 진입 후 인덱스 화면이 표시된 상태 | 1. 게임을 실행한다. 2. 인덱스 화면 상단을 확인한다. | 화면 상단에 `INDEX`라는 제목이 표시된다. | [p.2 / INDEX 화면 / EXPLICIT] "화면 상단에는 INDEX 제목을 표시한다." | - |
| 2 | 게임 진입 - 인덱스 UI | - | 배경 | 배경 색상 확인 | 인덱스 화면이 표시된 상태 | 1. 인덱스 화면으로 이동한다. 2. 화면 배경을 확인한다. | 화면 배경 색상이 흰색으로 표시된다. | [p.2 / INDEX 화면 / EXPLICIT] "배경 색상은 흰색으로 구성한다." | - |
| 3 | 궁술 훈련 | 진행 흐름 | 훈련 시작 | 훈련 시작 화면 표시 | 훈련장 로비에 진입한 상태 | 1. 훈련 시작 버튼을 선택한다. 2. 시작 화면을 확인한다. | 훈련 시작 화면이 표시되고 카운트다운이 시작된다. | [p.5 / 훈련 진행 / EXPLICIT] "훈련 시작 버튼 선택 시 시작 카운트다운을 출력한다." | - |
| 4 | 무료 뽑기 | 보상 지급 | 일일 무료 횟수 | 무료 뽑기 정상 사용 | 로그인되어 있으며 오늘 무료 뽑기를 사용하지 않은 상태 | 1. 무료 뽑기 화면에 진입한다. 2. 무료 뽑기 버튼을 선택한다. | 보상이 한 번 지급되고 무료 뽑기 횟수가 0으로 변경된다. | [p.7 / 무료 뽑기 정책 / EXPLICIT] "모든 유저는 하루에 한 번 무료 뽑기를 할 수 있다." | 보상 지급 위치는 별도 근거 확인 필요 |
| 5 | 무료 뽑기 | 초기화 | 초기화 시간 | 자정 전후 무료 횟수 초기화 확인 | 무료 뽑기를 이미 사용한 상태 | 1. 초기화 시각 직전 횟수를 확인한다. 2. 초기화 시각 이후 다시 확인한다. | 정의된 초기화 시각 이후 무료 횟수가 1로 변경된다. | [p.7 / 무료 뽑기 정책 / INFERRED] "무료 뽑기는 매일 초기화된다." | 초기화 기준 시간 미정, 기획 확인 필요 |

---

# 9. 테스트 분류 체계

## 9.1 기능 분류 (화면 노출)

대분류/중분류/소분류는 게임 기능의 계층 구조다.

```text
대분류
 └─ 중분류
      └─ 소분류
           └─ 테스트 항목
```

AI는 동일한 기능에 속하는 테스트 케이스가 같은 분류값을 사용하도록 해야 한다. 아래처럼 의미가 같은 분류가 제각각 생성되지 않게, **분석 과정에서 대표 분류명을 먼저 생성한 뒤 테스트 케이스에서 재사용한다.**

```text
잘못된 예: 궁술훈련 / 궁술 훈련 / 활쏘기 훈련 / 훈련 - 궁술
```

## 9.2 내부 테스트 유형 (화면 비노출)

정상·경계값·예외 등의 유형은 내부 필드 `testType`으로 별도 관리한다.

```text
HAPPY_PATH / BOUNDARY / EXCEPTION / VALIDATION / STATE_TRANSITION / TIME / DUPLICATION / DATA_PERSISTENCE
```

`testType`은 기본 컬럼에 출력하지 않고 다음 용도로 사용한다.

- 테스트 생성 품질 평가
- 정상·예외 케이스 비율 확인
- AI 테스트 커버리지 검증
- 향후 필터 기능
- 비고 자동 생성 (예: `INFERRED` 근거의 `TIME` 테스트 → "시간 경계 테스트 / AI 추론 포함 / 기획 확인 필요")

초기 MVP에서는 모든 요구사항에 모든 유형을 강제로 생성하지 않는다. 해당 기능에 적합한 유형만 생성하되, 유형 선택 이유가 요구사항 및 기획서 근거와 연결되어야 한다. (v0.1 계승)

---

# 10. AI 분석 단계

## 10.1 PDF 원문 처리

```text
PDF 업로드
→ 파일 유효성 검사 (형식 / 크기 / 암호화 / 손상 / 페이지 수)
→ 페이지별 텍스트 추출
→ 페이지별 이미지 생성
→ 문단 및 표 영역 식별
→ 텍스트 좌표 저장
→ 원본 및 추출 결과 저장
```

페이지별 문서 요소 예시:

```json
{
  "pageNumber": 3,
  "elements": [
    {
      "elementId": "PAGE-3-TEXT-07",
      "elementType": "TEXT",
      "text": "모든 유저는 하루에 한 번 무료 뽑기를 할 수 있다.",
      "boundingBox": { "x": 0.12, "y": 0.31, "width": 0.67, "height": 0.05 }
    }
  ]
}
```

좌표는 페이지 크기에 대한 상대 비율(0~1)로 저장한다. 좌표 추출이 어려운 경우에도 **페이지 번호 / 원문 텍스트 / 섹션명**은 반드시 저장한다.

## 10.2 기능 분류 추출 (선행 단계)

AI는 테스트 케이스를 생성하기 전에 기획서의 기능 구조를 추출한다.

```json
{
  "majorCategory": "궁술 훈련",
  "middleCategories": [
    {
      "name": "진행 흐름",
      "minorCategories": ["훈련장 로비", "훈련 방법 팝업", "훈련 시작", "시작 카운트다운", "화살 출발", "화살 타깃"]
    }
  ]
}
```

이 단계에서 결정된 분류명을 요구사항·테스트 케이스 생성 단계에서 재사용한다.

## 10.3 요구사항 추출

각 요구사항은 다음 정보를 가진다: 요구사항 ID, 대분류, 중분류, 소분류, 요구사항 설명, 조건, 동작, 기대 동작, Evidence. 각 요구사항에는 반드시 원문 근거가 연결되어야 한다.

## 10.4 테스트 케이스 생성

AI는 요구사항 기반으로 다음 필드를 생성한다.

```text
displayOrder / majorCategory / middleCategory / minorCategory / testItem / testType
preconditions / testSteps / expectedResults / priority / confidence / evidences / notes
```

## 10.5 모호한 요구사항 생성

AI는 기대결과를 확정할 수 없는 내용을 별도로 생성한다. 모호한 내용을 임의로 확정하지 않고 질문으로 생성한다.

```text
무료 뽑기의 초기화 시간은 서버 시간 기준인가?
인벤토리가 가득 찬 경우 보상은 우편함으로 지급되는가?
네트워크 오류 발생 시 뽑기 횟수는 차감되는가?
```

관련 테스트 케이스가 생성된 경우 비고에 `관련 모호성: AMB-001`, `기획 확인 필요`를 표시한다.

## 10.6 분석 결과 검증 (v0.1 계승)

저장 전 다음 항목을 검증한다.

- JSON 스키마 일치 여부
- 요구사항 / 테스트 항목 / 테스트 스텝 / 기대결과 존재 여부
- Evidence 필드 존재 여부
- 페이지 번호 유효성 (`1 <= pageNumber <= document.pageCount`)
- 원문이 실제 추출 텍스트에 존재하는지
- Enum 값 유효성
- 분류명 일관성 (카테고리 트리에 존재하는 분류명인지)
- 중복 테스트 케이스 여부

## 10.7 Actor–Evaluator 자기 검증 루프

산출물 생성(CSV/Markdown/Jira 본문) 및 분석 품질 확보를 위해 내부 생성 루프를 적용한다.

```text
[Actor: 초안 생성]
       ↓
[Evaluator: 점수 및 피드백 산출]
       ↓
점수 >= 통과 기준 → 최종 결과 확정 (Output.status = SUCCESS)
점수 <  통과 기준 → 피드백을 힌트로 재생성 (다음 회차)
       ↓
최대 회차 도달(서킷 브레이커) → Output.status = FAILED, 실패 사유 저장
```

- 각 회차의 초안, 점수, 피드백을 `Lats_Loop_Log`에 기록한다.
- 프론트엔드는 이 테이블을 조회하여 프로그레스 바 또는 실시간 로그를 표시한다.

---

# 11. AI 응답 JSON 구조

## 11.1 전체 응답

```json
{
  "documentSummary": {
    "title": "무료 뽑기 시스템",
    "featureSummary": "사용자가 매일 한 번 무료 뽑기를 사용할 수 있는 기능",
    "actors": ["PLAYER"]
  },
  "categoryTree": [],
  "requirements": [],
  "testCases": [],
  "ambiguities": [],
  "qualityWarnings": []
}
```

## 11.2 요구사항 응답 (v0.1 구조 + v0.2 분류 필드)

```json
{
  "requirementId": "REQ-001",
  "majorCategory": "무료 뽑기",
  "middleCategory": "보상 지급",
  "minorCategory": "일일 무료 횟수",
  "title": "일일 무료 뽑기 제공",
  "description": "사용자는 하루에 한 번 무료 뽑기를 사용할 수 있다.",
  "actor": "PLAYER",
  "preconditions": ["사용자가 로그인한 상태이다."],
  "trigger": "사용자가 무료 뽑기 버튼을 선택한다.",
  "expectedBehaviors": [
    "무료 뽑기 결과가 지급된다.",
    "남은 무료 횟수가 차감된다."
  ],
  "evidences": [
    {
      "evidenceType": "EXPLICIT",
      "pageNumber": 3,
      "sectionTitle": "무료 뽑기 정책",
      "sourceText": "모든 유저는 하루에 한 번 무료 뽑기를 할 수 있다.",
      "sourceElementType": "TEXT",
      "reason": "일일 무료 뽑기 가능 횟수가 직접 명시되어 있다."
    }
  ]
}
```

## 11.3 테스트 케이스 응답

```json
{
  "testCaseId": "TC-001",
  "requirementId": "REQ-001",
  "displayOrder": 1,
  "majorCategory": "무료 뽑기",
  "middleCategory": "보상 지급",
  "minorCategory": "일일 무료 횟수",
  "testItem": "무료 뽑기 정상 사용",
  "testType": "HAPPY_PATH",
  "priority": "HIGH",
  "preconditions": [
    "사용자가 로그인되어 있다.",
    "오늘 무료 뽑기를 사용하지 않았다.",
    "인벤토리에 빈 공간이 있다."
  ],
  "testSteps": [
    { "stepNumber": 1, "action": "무료 뽑기 화면에 진입한다.", "expectedResult": "무료 뽑기 버튼이 활성화되어 표시된다." },
    { "stepNumber": 2, "action": "무료 뽑기 버튼을 선택한다.", "expectedResult": "뽑기 결과 화면이 표시된다." }
  ],
  "expectedResults": [
    "보상 아이템이 한 번 지급된다.",
    "남은 무료 뽑기 횟수가 0으로 변경된다."
  ],
  "confidence": "HIGH",
  "requiresHumanReview": false,
  "evidences": [
    {
      "evidenceType": "EXPLICIT",
      "verificationStatus": "EXACT",
      "pageNumber": 7,
      "sectionTitle": "무료 뽑기 정책",
      "sourceElementId": "PAGE-7-TEXT-04",
      "sourceText": "모든 유저는 하루에 한 번 무료 뽑기를 할 수 있다.",
      "sourceElementType": "TEXT",
      "boundingBox": { "x": 0.14, "y": 0.28, "width": 0.63, "height": 0.06 },
      "reason": "무료 뽑기의 일일 사용 가능 횟수가 직접 명시되어 있다."
    }
  ],
  "notes": []
}
```

## 11.4 추론이 포함된 테스트 케이스

```json
{
  "testCaseId": "TC-002",
  "requirementId": "REQ-002",
  "displayOrder": 2,
  "majorCategory": "무료 뽑기",
  "middleCategory": "초기화",
  "minorCategory": "초기화 시간",
  "testItem": "무료 뽑기 초기화 경계 확인",
  "testType": "TIME",
  "priority": "HIGH",
  "preconditions": ["오늘 무료 뽑기를 이미 사용한 상태이다."],
  "testSteps": [
    { "stepNumber": 1, "action": "초기화 기준 시각 직전 무료 뽑기 횟수를 확인한다.", "expectedResult": "무료 뽑기 횟수가 0으로 표시된다." },
    { "stepNumber": 2, "action": "초기화 기준 시각 이후 무료 뽑기 횟수를 다시 확인한다.", "expectedResult": "무료 뽑기 횟수가 초기화된다." }
  ],
  "expectedResults": ["정의된 초기화 기준 시각 이후 무료 뽑기 횟수가 1로 변경된다."],
  "confidence": "LOW",
  "requiresHumanReview": true,
  "evidences": [
    {
      "evidenceType": "INFERRED",
      "verificationStatus": "EXACT",
      "pageNumber": 7,
      "sectionTitle": "무료 뽑기 정책",
      "sourceText": "무료 뽑기는 매일 초기화된다.",
      "sourceElementType": "TEXT",
      "reason": "매일 초기화된다는 내용은 있으나 초기화 기준 시각은 정의되어 있지 않다."
    }
  ],
  "notes": ["AI 추론 포함", "초기화 기준 시간 기획 확인 필요", "관련 모호성: AMB-001"]
}
```

## 11.5 모호한 요구사항 응답 (v0.1 계승 + 분류 필드)

```json
{
  "ambiguityId": "AMB-001",
  "relatedRequirementIds": ["REQ-001"],
  "majorCategory": "무료 뽑기",
  "middleCategory": "초기화",
  "minorCategory": "초기화 시간",
  "title": "무료 뽑기 초기화 시간 기준 불명확",
  "description": "기획서에는 매일 초기화된다고 작성되어 있으나 정확한 시간 기준이 없다.",
  "question": "무료 뽑기 횟수는 서버 시간 기준으로 초기화되나요?",
  "impact": "초기화 경계 테스트의 예상 결과를 확정할 수 없다.",
  "severity": "HIGH",
  "evidences": [
    {
      "evidenceType": "INFERRED",
      "pageNumber": 3,
      "sectionTitle": "무료 뽑기 정책",
      "sourceText": "무료 뽑기는 매일 초기화된다.",
      "sourceElementType": "TEXT",
      "reason": "초기화 주기는 명시되었지만 기준 시간이 정의되지 않았다."
    }
  ]
}
```

---

# 12. Evidence 추적 설계

## 12.1 Evidence의 목적 (v0.1 계승)

Evidence는 AI가 생성한 요구사항과 테스트 케이스가 기획서의 어떤 내용을 기반으로 작성되었는지 보여주는 정보다. 사용자는 Evidence를 통해 다음을 판단할 수 있어야 한다.

- AI가 기획서를 올바르게 해석했는가
- 테스트 조건이 원문에 실제로 존재하는가
- 예상 결과가 원문에서 직접 확인되는가
- AI가 임의로 내용을 추가하지 않았는가
- 추가 기획 확인이 필요한가

## 12.2 Evidence 필드

```text
evidenceType / verificationStatus / pageNumber / sectionTitle / sourceElementId / sourceText / sourceElementType / boundingBox / reason
```

| 필드 | 설명 |
| --- | --- |
| evidenceType | EXPLICIT, INFERRED, UNSUPPORTED |
| verificationStatus | EXACT, PARTIAL, SIMILAR, NOT_FOUND (백엔드 검증 결과) |
| pageNumber | 근거가 존재하는 PDF 페이지 |
| sectionTitle | 근거가 포함된 기획서 섹션 |
| sourceElementId | 페이지 요소 식별자 (예: PAGE-7-TEXT-04) |
| sourceText | 근거가 된 원문 |
| sourceElementType | TEXT, TABLE, IMAGE, CAPTION |
| boundingBox | PDF 페이지 내 근거 위치 좌표 (상대 비율) |
| reason | 해당 원문이 근거로 선택된 이유 |

## 12.3 Evidence 유형 (v0.1 계승)

**EXPLICIT** — 원문에 조건과 결과가 명확하게 작성되어 있다.

**INFERRED** — 원문 일부를 바탕으로 조건이나 예외 상황을 AI가 추론했다. 비고에 `AI 추론 포함`을 자동 표시한다.

**UNSUPPORTED** — 근거를 찾을 수 없거나 일반적인 서비스 관행만으로 생성한 내용이다. 승인 화면에서 강한 경고를 표시하고 비고에 `기획서 직접 근거 없음`, `QA 검토 필수`를 표시한다.

## 12.4 Evidence 연결 관계 (v0.1 계승)

```text
PlanningDocument → Requirement → TestCase → Evidence
```

하나의 테스트 케이스는 여러 개의 근거를 가질 수 있다.

```text
TC-001 무료 뽑기 사용
 ├─ 페이지 3: 하루 한 번 무료 사용
 └─ 페이지 4: 보상은 인벤토리에 지급
```

## 12.5 프론트 표시 형태 (v0.2 확정)

테스트 케이스의 `기획서 원문 근거` 셀에는 요약 정보만 표시한다: `p.7 / 무료 뽑기 정책 / EXPLICIT`

셀 또는 근거 보기 버튼 선택 시 상세 패널에 표시:

```text
페이지: 7
섹션: 무료 뽑기 정책
근거 유형: EXPLICIT
원문 검증: EXACT

원문:
"모든 유저는 하루에 한 번 무료 뽑기를 할 수 있다."

선정 이유:
무료 뽑기의 일일 사용 가능 횟수가 직접 명시되어 있다.
```

동시에 PDF 미리보기는 해당 페이지로 이동하고, 좌표가 있으면 영역을 강조한다(선택 기능). 근거가 여러 개인 경우 `[근거 1] [근거 2]` 탭/번호 버튼으로 제공하며, 선택한 근거에 따라 PDF 페이지와 하이라이트 위치가 변경된다. **좌표가 없어도 페이지 번호와 원문은 반드시 표시한다.**

---

# 13. Evidence 검증 로직 (v0.1 계승)

AI가 반환한 원문을 그대로 신뢰하지 않는다. 백엔드에서 다음 검증을 수행한다.

## 13.1 원문 일치 검증

AI가 반환한 `sourceText`가 추출된 기획서 텍스트 안에 실제 존재하는지 확인하고, 결과를 `verificationStatus`로 저장한다.

| 상태 | 의미 | 처리 |
| --- | --- | --- |
| EXACT | 원문이 정확히 확인됨 | 정상 저장 |
| PARTIAL | 일부 생략되었으나 핵심 문구 확인됨 | 정상 저장 |
| SIMILAR | 표현은 다르나 의미가 유사함 | 사용자 검토 필요 표시, 비고 `원문 유사 일치, QA 확인 필요` |
| NOT_FOUND | 원문을 기획서에서 확인 불가 | Evidence를 `UNSUPPORTED`로 처리, `requiresHumanReview=true`, 화면 경고, 비고 `원문 확인 불가` |

## 13.2 페이지 검증

```text
1 <= pageNumber <= document.pageCount
```

유효하지 않은 페이지 번호는 저장하지 않거나 검토 경고를 표시한다.

---

# 14. 테스트 케이스 상태 (v0.1 계승)

```text
GENERATED  : AI 생성 완료, QA 검토 전
APPROVED   : QA가 원문과 비교 후 타당하다고 판단
REJECTED   : QA가 부정확하거나 불필요하다고 판단 (사유 필수)
```

상태 변경 규칙:

```text
GENERATED → APPROVED
GENERATED → REJECTED
```

현재 MVP에서는 승인/반려 후 상태를 되돌리는 기능을 제공하지 않는다. 필요 시 향후 버전에서 재검토 기능을 추가한다.

---

# 15. 데이터베이스 설계

사용 테이블:

```text
User / Project / PlanningDocument / AnalysisJob / Requirement / TestCase / Ambiguity / Output / Lats_Loop_Log
```

- `TestCaseRevision` 테이블은 생성하지 않는다.
- `ExternalIntegration`, `ExportHistory`는 `Output`으로 통합한다.
- 스키마는 Flyway로 관리한다.

## 15.1 User (v0.1 계승)

```text
id / email / password / name / role(USER|QA) / createdAt / updatedAt
```

## 15.2 Project (v0.1 계승)

```text
id / ownerId / name / description / gameGenre / platform / status / createdAt / updatedAt
```

`ownerId`는 프로젝트를 생성한 QA 사용자를 참조한다.

## 15.3 PlanningDocument (v0.1 계승)

```text
id / projectId / title / originalFileName / storedFilePath / mimeType / fileSize / pageCount
extractedText / pageContents(JSONB) / processingStatus / createdBy / createdAt / updatedAt
```

`processingStatus`: `UPLOADED / PROCESSING / READY / FAILED`

`pageContents` JSONB 예시는 §10.1 참조 (elementId, elementType, text, boundingBox 포함).

## 15.4 AnalysisJob (v0.1 계승)

```text
id / planningDocumentId / status / modelName / promptVersion / requestedBy
requestedAt / startedAt / completedAt / failureReason / tokenUsage / rawResponse / createdAt
```

`status`: `PENDING / PROCESSING / COMPLETED / FAILED`

## 15.5 Requirement (v0.2 확정)

```text
id / analysisJobId / externalRequirementId
majorCategory / middleCategory / minorCategory
title / description / actor / preconditions / trigger / expectedBehaviors / constraints / evidences(JSONB)
createdAt
```

요구사항과 테스트 케이스의 분류 체계가 일치하도록 한다.

## 15.6 TestCase (v0.2 확정)

```text
id / projectId / planningDocumentId / analysisJobId / requirementId / externalTestCaseId
displayOrder
majorCategory / middleCategory / minorCategory / testItem / testType
priority / confidence / status
preconditions(JSONB) / testSteps(JSONB) / expectedResults(JSONB) / evidences(JSONB) / notes(JSONB)
requiresHumanReview
reviewedBy / reviewedAt / rejectionReason
createdAt / updatedAt
```

### 필드 매핑

| DB 필드 | 화면 및 출력 컬럼 |
| --- | --- |
| displayOrder | No |
| majorCategory | 대분류 |
| middleCategory | 중분류 |
| minorCategory | 소분류 |
| testItem | 테스트 항목 |
| preconditions | 사전조건 |
| testSteps | 테스트 스텝 |
| expectedResults | 기대결과 |
| evidences | 기획서 원문 근거 |
| notes | 비고 |

### 저장 방식 원칙

- **JSONB**: preconditions, testSteps, expectedResults, evidences, notes
- **관계형 컬럼**: displayOrder, majorCategory, middleCategory, minorCategory, testItem, testType, priority, confidence, status, reviewedBy, reviewedAt
- 분류 및 검색에 사용하는 값은 JSONB 내부에만 저장하지 않는다.

Enum: `testType`은 §9.2, `status`는 `GENERATED/APPROVED/REJECTED`, `confidence`는 `HIGH/MEDIUM/LOW`.

## 15.7 Ambiguity (v0.2 확정)

```text
id / planningDocumentId / analysisJobId / externalAmbiguityId
majorCategory / middleCategory / minorCategory
title / description / question / impact / severity / status / evidences(JSONB)
jiraIssueKey / jiraIssueUrl / publishedAt / createdAt
```

`status`: `OPEN / ISSUE_CREATED / DISMISSED`

## 15.8 Output (v0.2 분리안 확정 — 최종 결과 및 상태 관리)

사용자가 화면에서 확인하는 '최종 상태'만 가볍게 보관한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | BIGINT (PK) | 아웃풋 고유 ID |
| project_id | BIGINT (FK) | 프로젝트 ID |
| planning_document_id | BIGINT (FK) | 원본 게임 기획서 ID |
| ambiguity_id | BIGINT (FK, nullable) | Jira Issue 대상 모호성 ID |
| output_type | VARCHAR | `CSV_EXPORT` / `MARKDOWN_EXPORT` / `JIRA_ISSUE` |
| status | VARCHAR | `PENDING` (루프 진행 중) / `SUCCESS` / `FAILED` |
| final_content | TEXT | 최종 통과된 CSV·Markdown 텍스트 또는 Jira ADF JSON |
| file_name | VARCHAR | 다운로드 파일명 |
| file_path | VARCHAR | 저장 경로 |
| external_service | VARCHAR | `NONE` / `JIRA` |
| external_resource_id | VARCHAR | Jira Issue Key (성공 시) |
| external_url | VARCHAR | Jira Issue 바로가기 링크 (성공 시) |
| request_data | JSONB | 요청 데이터 |
| response_data | JSONB | 응답 데이터 |
| created_by | BIGINT (FK) | 생성 사용자 |
| created_at | TIMESTAMP | 생성 시간 |
| completed_at | TIMESTAMP | 완료 시간 |
| failure_reason | VARCHAR | 서킷 브레이커 작동 시 실패 사유 |

## 15.9 Lats_Loop_Log (★ 프로젝트의 핵심 증거 자산)

Actor–Evaluator 루프가 스스로 검증하고 교정한 흔적을 전부 기록한다. 프론트엔드는 프로그레스 바나 실시간 로그를 띄울 때 이 테이블을 조회한다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| id | BIGINT (PK) | 로그 고유 ID |
| output_id | BIGINT (FK) | 부모 아웃풋 ID |
| depth_step | INT | 루프 차수 (1회차, 2회차, 3회차...) |
| generated_draft | TEXT | AI(Actor)가 해당 회차에 생성한 초안 |
| evaluation_score | INT | AI(Evaluator)가 내린 점수 (예: 65점) |
| evaluation_feedback | TEXT | AI가 스스로 내린 피드백 (다음 회차의 힌트) |
| created_at | TIMESTAMP | 기록 시간 |

## 15.10 테이블 관계 (v0.1 계승 + 갱신)

```text
User
 ├─ Project
 ├─ AnalysisJob
 └─ TestCase.reviewedBy

Project
 ├─ PlanningDocument
 ├─ TestCase
 └─ Output

PlanningDocument
 ├─ AnalysisJob
 ├─ Requirement
 ├─ TestCase
 ├─ Ambiguity
 └─ Output

AnalysisJob
 ├─ Requirement
 ├─ TestCase
 └─ Ambiguity

Requirement
 └─ TestCase

Ambiguity
 └─ Output (JIRA_ISSUE)

Output
 └─ Lats_Loop_Log
```

---

# 16. API 설계

## 16.1 인증 API (v0.1 계승)

```http
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

## 16.2 프로젝트 API (v0.1 계승)

```http
POST /api/projects            (QA)
GET  /api/projects            (USER, QA)
GET  /api/projects/{projectId} (USER, QA)
```

현재 MVP에서는 프로젝트 수정 및 삭제 기능을 보류한다.

## 16.3 기획서 API (v0.1 계승)

```http
POST /api/projects/{projectId}/documents   (QA, multipart/form-data: file=PDF, title)
GET  /api/projects/{projectId}/documents   (USER, QA)
GET  /api/documents/{documentId}           (USER, QA)
GET  /api/documents/{documentId}/pages/{pageNumber} (USER, QA)
```

## 16.4 AI 분석 API (v0.1 계승)

```http
POST /api/documents/{documentId}/analyses          (QA)
GET  /api/analyses/{analysisId}                    (USER, QA)
GET  /api/documents/{documentId}/analyses/latest   (USER, QA)
```

## 16.5 요구사항 API (v0.1 계승)

```http
GET /api/analyses/{analysisId}/requirements
GET /api/requirements/{requirementId}
```

## 16.6 테스트 케이스 API (v0.2 확정)

```http
GET  /api/projects/{projectId}/test-cases    (USER, QA)
GET  /api/test-cases/{testCaseId}            (USER, QA)
POST /api/test-cases/{testCaseId}/approve    (QA)
POST /api/test-cases/{testCaseId}/reject     (QA)
```

- **bulk-approve / bulk-reject는 구현하지 않는다.** (해성 결정: 케이스별 개별 처리)
- 테스트 케이스 내용을 수정하는 `PATCH` API를 제공하지 않는다.

목록 조회 조건:

```text
analysisId / status / majorCategory / middleCategory / minorCategory / testType / confidence / keyword
```

목록 응답 예시:

```json
{
  "items": [
    {
      "id": 101,
      "displayOrder": 1,
      "majorCategory": "게임 진입 - 인덱스 UI",
      "middleCategory": "-",
      "minorCategory": "INDEX 제목",
      "testItem": "INDEX 제목 표시",
      "preconditions": ["게임 진입 후 인덱스 화면이 표시된 상태"],
      "testSteps": [{ "stepNumber": 1, "action": "화면 상단을 확인한다." }],
      "expectedResults": ["화면 상단에 INDEX라는 제목이 표시된다."],
      "evidenceSummary": {
        "pageNumber": 2,
        "evidenceType": "EXPLICIT",
        "sourceText": "화면 상단에 INDEX 제목을 표시한다."
      },
      "notes": [],
      "status": "GENERATED"
    }
  ]
}
```

상세 응답에는 전체 테스트 스텝과 Evidence 배열을 포함한다.

반려 요청:

```json
{ "reason": "기획서 원문에서 기대결과에 대한 근거를 확인할 수 없습니다." }
```

## 16.7 Evidence API (v0.1 계승)

```http
GET /api/test-cases/{testCaseId}/evidences
GET /api/requirements/{requirementId}/evidences
GET /api/ambiguities/{ambiguityId}/evidences
```

## 16.8 모호한 요구사항 API (v0.1 계승 + Markdown 추가)

```http
GET  /api/projects/{projectId}/ambiguities
GET  /api/ambiguities/{ambiguityId}
POST /api/ambiguities/{ambiguityId}/jira-issue   (QA)
```

## 16.9 산출물(Output) API

```http
POST /api/projects/{projectId}/outputs/csv        (QA)  승인된 테스트 케이스 CSV 생성
POST /api/projects/{projectId}/outputs/markdown   (QA)  모호한 요구사항 Markdown 생성
GET  /api/outputs/{outputId}                      (USER, QA)
GET  /api/outputs/{outputId}/download             (USER, QA)
GET  /api/outputs/{outputId}/loop-logs            (USER, QA)  Lats_Loop_Log 조회 (프로그레스/이력)
```

CSV에는 기본적으로 승인된 테스트 케이스만 포함한다.

---

# 17. 프론트엔드 화면 구성

## 17.1 로그인 화면 (v0.1 계승)

이메일 / 비밀번호 입력. 로그인 성공 후 역할에 따라 사용할 수 있는 기능을 구분한다.

## 17.2 프로젝트 목록 화면 (v0.1 계승)

표시 항목: 프로젝트명, 게임 장르, 플랫폼, 기획서 개수, 생성/승인/반려 테스트 케이스 수, 미확인 모호성 수, 최근 분석일. QA에게만 프로젝트 생성 버튼을 표시한다.

## 17.3 프로젝트 생성 화면 (QA 전용, v0.1 계승)

입력: 프로젝트명, 설명, 게임 장르, 플랫폼

## 17.4 기획서 업로드 화면 (QA 전용, v0.1 계승)

입력: 기획서 제목, PDF 파일, 분석 시작 여부

검증: PDF만 업로드 가능, 최대 파일 크기 제한, 암호화된 PDF 제한, 페이지 수 제한, 파일 손상 확인

## 17.5 기획서 조회 화면 (v0.1 계승)

```text
왼쪽: PDF 페이지 목록 / 가운데: PDF 원문 미리보기 / 오른쪽: 문서 정보 및 분석 상태
```

표시 항목: 파일명, 페이지 수, 업로드 사용자, 업로드 시각, 처리 상태, 최근 분석 상태

## 17.6 AI 분석 결과 화면 (v0.1 계승)

탭 구성: `기능 요약 / 요구사항 / 테스트 케이스 / 모호한 요구사항`

분석 진행 중에는 Lats_Loop_Log 기반 프로그레스 바 또는 실시간 로그를 표시한다.

## 17.7 테스트 케이스 검토 화면 (핵심 화면, v0.2 확정)

```text
┌──────────────────────────────────────────────────────────────┐
│ 필터: 대분류 / 중분류 / 소분류 / 상태 / 신뢰도                 │
├───────────────────────────────────┬──────────────────────────┤
│ 테스트 케이스 표 (10컬럼)          │ PDF 기획서 원문           │
│                                   │                          │
│ No / 대분류 / 중분류 / 소분류      │ 선택된 Evidence 페이지    │
│ 테스트 항목 / 사전조건             │                          │
│ 테스트 스텝 / 기대결과             │ 원문 근거 하이라이트       │
│ 기획서 원문 근거 / 비고            │ (선택 기능)               │
├───────────────────────────────────┤                          │
│ 선택 케이스 상세 + 승인 / 반려      │                          │
└───────────────────────────────────┴──────────────────────────┘
```

- 승인/반려는 각 케이스별로 개별 수행한다.
- 세부 레이아웃은 구현 상황을 보고 재조정할 수 있다. (해성 결정)

### 표 표시 방식

컬럼이 많으므로 모든 내용을 한 줄에 강제로 표시하지 않는다.

- `No`, 대분류, 중분류, 소분류, 테스트 항목은 기본 노출
- 사전조건, 테스트 스텝, 기대결과는 줄바꿈 허용
- 기획서 원문 근거는 요약 표시 (`p.7 / 무료 뽑기 정책 / EXPLICIT`)
- 비고는 태그 형태로 표시: `[AI 추론 포함] [기획 확인 필요] [근거 없음] [QA 검토 필수]`
- 행 선택 시 하단 또는 사이드 패널에서 전체 내용 표시
- 가로 스크롤 지원, 컬럼 너비 조절 가능

### Evidence 연동 동작

1. 테스트 케이스 행 선택 → 상세 조회
2. 연결된 Evidence 조회
3. 첫 번째 Evidence 페이지로 PDF 이동
4. 좌표가 있으면 근거 영역 하이라이트 (선택 기능)
5. Evidence 원문과 선정 이유 표시
6. Evidence가 여러 개인 경우 근거 선택 버튼 표시
7. **좌표가 없는 경우에도 페이지 번호와 원문은 반드시 표시**

## 17.8 승인 및 반려 UI (v0.1 계승)

- 승인 시: 확인 모달 → 상태 `APPROVED` → 검토자·검토 시각 저장
- 반려 시: 반려 사유 입력 모달(필수) → 상태 `REJECTED` → 사유 저장
- 테스트 케이스 수정 버튼은 표시하지 않는다.

## 17.9 모호한 요구사항 화면 (v0.1 계승)

표시 항목: 제목, 관련 요구사항, 질문, 영향, 심각도, 기획서 근거, 상태, Markdown 다운로드 버튼, Jira Issue 생성 버튼

## 17.10 산출물 다운로드 화면

표시 항목: 승인된 테스트 케이스 수, 포함될 요구사항 수, 문서 미리보기, CSV 다운로드 버튼(테스트 케이스), Markdown 다운로드 버튼(모호 요구사항), 생성 진행 상태(Lats_Loop_Log 연동)

---

# 18. 산출물 출력 형식

## 18.1 승인 테스트 케이스 CSV

- `APPROVED` 테스트 케이스만 포함
- `displayOrder` 순서로 정렬
- 컬럼 순서 고정: `No, 대분류, 중분류, 소분류, 테스트 항목, 사전조건, 테스트 스텝, 기대결과, 기획서 원문 근거, 비고`
- 빈 중분류와 사전조건은 `-`로 표시
- 테스트 스텝은 번호 목록을 셀 내 줄바꿈으로 변환
- 여러 Evidence·비고는 줄바꿈하여 표시
- 셀 내 줄바꿈·쉼표·따옴표는 RFC 4180 규칙에 따라 이스케이프 처리
- UTF-8 BOM 포함 (Excel 한글 호환)
- 반려된 테스트 케이스는 포함하지 않음

CSV 예시 (개념):

```csv
No,대분류,중분류,소분류,테스트 항목,사전조건,테스트 스텝,기대결과,기획서 원문 근거,비고
1,게임 진입 - 인덱스 UI,-,INDEX 제목,INDEX 제목 표시,게임 진입 후 인덱스 화면 표시,"1. 게임 실행
2. 화면 상단 확인",화면 상단에 INDEX 제목이 표시된다.,"[p.2 / INDEX 화면 / EXPLICIT]
화면 상단에 INDEX 제목을 표시한다.",-
```

## 18.2 모호한 요구사항 Markdown

```markdown
# 모호한 요구사항 목록

## 프로젝트 정보
- 프로젝트명: Game Sample
- 기획서명: 무료 뽑기 시스템
- 생성일: 2026-07-13

## AMB-001 무료 뽑기 초기화 시간 기준 불명확

- 관련 요구사항: REQ-001 일일 무료 뽑기 제공
- 심각도: HIGH
- 상태: OPEN

### 질문
무료 뽑기 횟수는 서버 시간 기준으로 초기화되나요?

### 테스트 영향
초기화 직전 및 직후 테스트의 예상 결과를 확정할 수 없습니다.

### 기획서 근거
- 페이지: 3 / 근거 유형: INFERRED
- 원문: 무료 뽑기는 매일 초기화된다.
```

## 18.3 (보류) 승인 테스트 케이스 Markdown 표

여유 일정 확보 시에만 구현한다. 구현 시 v0.2 §14 형식(10컬럼 Markdown 표, `<br>` 줄바꿈)을 그대로 따른다.

---

# 19. Jira Issue 생성 (v0.1 계승 + v0.2 리스크 반영)

## 19.1 생성 대상

- 상태가 `OPEN`인 모호한 요구사항
- QA 사용자가 명시적으로 생성 요청한 항목
- 아직 Jira Issue가 생성되지 않은 항목 (중복 생성 차단 또는 경고)

## 19.2 단계적 구현 전략 (v0.2 결정)

Jira는 자체 문서 포맷(ADF, Atlassian Document Format)을 사용하므로 포맷팅이 제대로 되지 않을 리스크가 있다.

- **1차 (필수)**: Jira와 유사한 형태의 목업 화면에 Issue를 게시한다. Output 테이블에 `JIRA_ISSUE` 타입으로 요청/응답 데이터를 동일하게 저장한다.
- **2차 (잔여 일정)**: 실제 Jira REST API와 연동한다. 외부 Jira 호출은 인터페이스 뒤에 숨겨 목업 구현과 실제 구현을 교체 가능하게 한다.

## 19.3 Jira Issue 형식

```text
제목:
[QA 확인 필요] 무료 뽑기 초기화 시간 기준 확인

본문:

## 관련 기능
무료 뽑기 시스템

## 확인이 필요한 내용
무료 뽑기의 초기화 시간이 서버 시간 기준인지 정의되어 있지 않습니다.

## 질문
무료 뽑기 횟수는 서버 시간 기준으로 초기화되나요?

## 테스트 영향
초기화 직전 및 직후 테스트의 예상 결과를 확정할 수 없습니다.

## 기획서 근거
- 페이지: 3
- 원문: 무료 뽑기는 매일 초기화된다.

## 관련 요구사항
REQ-001 일일 무료 뽑기 제공

## 생성 출처
Spec2Test
```

## 19.4 생성 결과 저장

성공 시: Jira Issue Key, URL, 생성 사용자, 생성 시간, 요청/응답 데이터를 Output에 저장하고 Ambiguity의 `jiraIssueKey/jiraIssueUrl/status`를 갱신한다.

---

# 20. 프롬프트 설계 원칙

프롬프트는 리소스 파일로 분리하고 `promptVersion`으로 버전을 관리한다.

## 20.1 시스템 프롬프트 (v0.1 계승)

```text
당신은 게임 QA 테스트 설계 전문가입니다.

PDF 게임 기획서의 텍스트, 표, 이미지 및 화면 설명을 분석하여
테스트 가능한 기능 요구사항과 QA 테스트 케이스를 생성해야 합니다.

모든 요구사항과 테스트 케이스에는 기획서 원문 근거를 연결해야 합니다.
기획서에 직접 작성된 사실과 AI가 추론한 내용을 구분해야 합니다.
기획서에 정의되지 않은 정책은 임의로 결정하지 말고
모호한 요구사항 또는 질문으로 생성해야 합니다.
원문 근거가 없는 테스트 케이스는 UNSUPPORTED로 표시해야 합니다.
모든 결과는 지정된 JSON 스키마에 맞춰 반환해야 합니다.
```

## 20.2 테스트 케이스 출력 지침 (v0.2 확정)

```text
각 테스트 케이스는 반드시 다음 필드를 포함해야 합니다.

1. 대분류  2. 중분류  3. 소분류  4. 테스트 항목  5. 사전조건
6. 테스트 스텝  7. 기대결과  8. 기획서 원문 근거  9. 비고

대분류, 중분류, 소분류는 테스트 유형이 아니라
게임 기능과 화면의 계층 구조를 나타냅니다.

동일한 기능에 속하는 테스트 케이스는 동일한 분류명을 사용하십시오.
적절한 중분류 또는 사전조건이 없으면 빈 문자열이 아니라 "-"를 반환하십시오.
```

## 20.3 테스트 항목 작성 지침 (v0.2)

```text
테스트 항목은 하나의 검증 목적만 포함해야 합니다.

좋은 예: INDEX 제목 표시 / 배경 색상 확인 / 무료 횟수 차감 확인
나쁜 예: 제목, 버튼, 배경, 글자 크기 및 애니메이션 전체 확인
```

## 20.4 테스트 스텝 작성 지침 (v0.2)

```text
테스트 스텝은 실제 사용자가 수행할 수 있는 동작으로 작성하십시오.
각 스텝에는 순서와 수행 동작을 포함하십시오.
"확인한다"만 반복하지 말고 어떤 화면 또는 데이터를 확인하는지 명시하십시오.
```

## 20.5 기대결과 작성 지침 (v0.2)

```text
기대결과는 관찰하거나 검증할 수 있는 상태로 작성하십시오.

다음 표현만 단독으로 사용하지 마십시오.
- 정상적으로 처리된다. / 정상적으로 표시된다. / 문제없이 동작한다.

화면 문구, 버튼 상태, 데이터 변화, 횟수 변화, 저장 결과 등을 명시하십시오.
```

## 20.6 테스트 유형 검토 지침 (v0.1 계승)

```text
다음 유형을 검토하십시오.

- 정상 시나리오
- 최소 및 최대 경계값
- 기준값과 정확히 같은 경우 / 1 작은 경우 / 1 큰 경우
- 잘못된 입력
- 중복 요청
- 잘못된 상태
- 시간 초기화 직전과 직후
- 데이터 저장 여부
- 서버 처리 후 클라이언트 응답 실패

기획서와 관련 없는 유형은 억지로 생성하지 마십시오.
```

## 20.7 Evidence 생성 규칙 (v0.1 계승)

```text
각 요구사항과 테스트 케이스에 대해 다음 정보를 반환하십시오.

1. 근거가 존재하는 페이지 번호
2. 근거가 된 원문
3. 원문이 속한 섹션
4. 원문의 요소 유형
5. EXPLICIT, INFERRED, UNSUPPORTED 중 하나
6. 해당 원문이 근거가 되는 이유

원문을 찾을 수 없는 경우 존재하지 않는 문장을 만들어서는 안 됩니다.
근거를 찾을 수 없으면 evidenceType을 UNSUPPORTED로 반환하십시오.
```

## 20.8 비고 자동 생성 규칙 (v0.2)

| 조건 | 비고 |
| --- | --- |
| Evidence가 INFERRED | AI 추론 포함 |
| Evidence가 UNSUPPORTED | 기획서 직접 근거 없음 |
| 원문 검증이 SIMILAR | 원문 유사 일치, QA 확인 필요 |
| 원문 검증이 NOT_FOUND | 원문 확인 불가 |
| 관련 모호성이 존재 | 기획 확인 필요 |
| 기대결과를 확정할 수 없음 | 기대결과 정책 미정 |

## 20.9 금지 규칙 (v0.1 계승)

```text
- 기획서에 없는 정책을 사실처럼 작성하지 않는다.
- 존재하지 않는 원문을 Evidence로 생성하지 않는다.
- 페이지 번호를 추측하지 않는다.
- 모든 테스트에 높은 신뢰도를 부여하지 않는다.
- 모호한 내용을 정상 요구사항으로 확정하지 않는다.
- 동일한 목적의 테스트 케이스를 반복 생성하지 않는다.
- 예상 결과에 "정상 처리된다"와 같은 모호한 표현만 사용하지 않는다.
```

---

# 21. 보안 및 권한 요구사항 (v0.1 계승)

- PDF 업로드 / AI 분석 요청 / 승인·반려 / 산출물 생성 / Jira Issue 생성은 QA 역할만 가능하다.
- 일반 사용자는 조회와 생성된 산출물 다운로드만 가능하다.
- API 키와 Jira 토큰은 환경 변수로 관리하고 소스 코드에 저장하지 않는다.
- 기획서 원문 전체를 로그에 출력하지 않는다.
- AI 요청 및 응답 로그에는 민감 내용을 마스킹한다.
- 파일 경로 조작을 방지한다.
- PDF 외 파일 업로드를 차단하고, 악성 PDF 또는 과도한 용량의 파일을 제한한다.
- HTML 렌더링 시 XSS를 방지한다.
- 프로젝트 접근 권한을 API 수준에서 검증한다. 권한 없는 요청은 403을 반환한다.
- Jira Issue는 사용자의 명시적인 요청이 있을 때만 생성한다.
- **AI가 자동으로 Jira 도구를 실행하지 못하게 한다.**

---

# 22. 오류 처리 (v0.1 계승 + 서킷 브레이커)

## 22.1 PDF 처리 오류

오류 유형: PDF가 아닌 파일, 손상된 PDF, 암호화된 PDF, 텍스트 추출 실패, 페이지 이미지 생성 실패, 파일 크기 초과, 페이지 수 초과

처리: `PlanningDocument.processingStatus = FAILED`, 실패 사유 저장, 재업로드 안내, 분석 요청 차단

## 22.2 AI 분석 오류

오류 유형: 응답 시간 초과, JSON 파싱 실패, 필수 필드 누락, 잘못된 Enum 값, 빈 테스트 케이스, Evidence 원문 누락, 페이지 번호 오류

처리:

1. 응답 스키마를 검증한다.
2. 실패 원인을 저장한다.
3. 최대 1회 자동 재시도한다.
4. 재시도 실패 시 분석 상태를 `FAILED`로 변경한다.
5. 원본 PDF와 추출 데이터는 유지한다.

## 22.3 산출물 생성 루프 오류 (Actor–Evaluator)

- Evaluator 점수가 통과 기준 미달이면 피드백과 함께 재생성한다.
- 최대 루프 회차(권장: 3회)를 초과하면 서킷 브레이커가 작동하여 `Output.status = FAILED`, `failure_reason`을 저장한다.
- 모든 회차는 Lats_Loop_Log에 기록한다.

## 22.4 Jira 오류

오류 유형: 인증 실패, 프로젝트 키 오류, 권한 부족, API 호출 제한, 네트워크 오류, ADF 포맷팅 실패

처리:

- 테스트 케이스와 승인 상태에는 영향을 주지 않는다.
- `Output.status = FAILED`로 저장하고 실패 사유를 사용자에게 표시한다.
- 재시도 가능하게 한다.
- ADF 포맷팅이 불안정한 경우 목업 게시 모드로 폴백한다.

---

# 23. 테스트 전략 (v0.1 계승 + v0.2 반영)

## 23.1 백엔드 단위 테스트

- 사용자 역할 권한 검사
- PDF 파일 형식 / 페이지 수 검증
- AI JSON 역직렬화 (10컬럼 스키마)
- 기능 분류명 일관성 검증
- Evidence 페이지 번호 / 원문 일치 검증 (EXACT/PARTIAL/SIMILAR/NOT_FOUND)
- 테스트 케이스 승인 / 반려 상태 변경
- 비고 자동 생성 규칙
- CSV 생성 (이스케이프, BOM, 컬럼 순서)
- 모호 요구사항 Markdown 생성
- Jira 요청 데이터 생성 / 중복 생성 방지
- Actor–Evaluator 루프 서킷 브레이커

## 23.2 통합 테스트

- PDF 업로드 → 기획서 처리 완료
- 분석 요청 → 테스트 케이스 저장
- 테스트 케이스 조회 → Evidence 페이지 이동
- 테스트 케이스 승인 → CSV 출력
- 모호 요구사항 조회 → Jira Issue 생성(목업 포함)
- 산출물 생성 → Lats_Loop_Log 기록

## 23.3 프론트엔드 테스트

- 역할별 버튼 표시 여부
- 10컬럼 표 렌더링 / 행 선택 / 상세 패널
- Evidence 선택 시 PDF 페이지 이동
- 근거 문장 강조 표시 (좌표 존재 시)
- 승인 확인 모달 / 반려 사유 입력 검증
- CSV / Markdown 다운로드
- Jira Issue 생성 결과 표시
- 루프 진행 상태 표시

## 23.4 AI 품질 평가 (v0.1 계승)

- **요구사항 추출률**: 사람이 정의한 기준 요구사항 중 AI가 추출한 비율
- **테스트 케이스 적합률**: 생성된 테스트 케이스 중 실제 QA에 활용 가능한 비율
- **Evidence 정확도**: AI가 연결한 원문이 실제 근거로 적합한 비율
- **Evidence 탐지 성공률**: 유효한 원문 근거가 연결된 테스트 케이스 비율
- **환각 비율**: 기획서에 없는 내용을 사실처럼 작성한 비율
- **승인률 / 반려률**: QA가 승인/반려한 비율
- **분류 일관성**: 동일 기능에 대해 동일 분류명이 사용된 비율 (v0.2 추가)

---

# 24. 구현 우선순위

## Phase 1. 프로젝트 기반

Spring Boot / React / PostgreSQL / Flyway 구성, 로그인, USER·QA 역할 분리, 프로젝트 생성·조회

## Phase 2. PDF 기획서 처리

PDF 업로드, 파일 검증, 원본 저장, 페이지별 텍스트 추출, 페이지 이미지 생성, 텍스트 위치 추출, 기획서 조회 화면

## Phase 3. AI 분석

Spring AI 설정, 멀티모달 요청, 기능 분류 추출 프롬프트, 요구사항 추출 프롬프트, 10컬럼 테스트 케이스 생성 프롬프트, 모호성 분석 프롬프트, JSON Schema 정의, 구조화 응답 저장

## Phase 4. Evidence 추적

테스트 케이스–Evidence 연결, 원문 일치 검증, 페이지 번호 검증, PDF 페이지 이동, 근거 문장 강조(선택), Evidence 상세 패널

## Phase 5. 검토 (승인/반려)

테스트 시트 화면(10컬럼), 상세 조회, 개별 승인, 개별 반려(사유 필수), 검토자·검토 시각 저장

## Phase 6. 산출물 출력

승인 테스트 케이스 CSV 생성·다운로드, 모호 요구사항 Markdown 생성·다운로드, Output 기록, Actor–Evaluator 루프 + Lats_Loop_Log + 진행 상태 표시

## Phase 7. Jira 연동

Jira 유사 목업 게시(1차) → Jira 인증 설정, Issue 생성 요청, 결과 저장, 중복 생성 방지, 실패·재시도 처리(2차)

---

# 25. 작업 원칙 및 코드 구조 (v0.1 계승 + v0.2 반영)

## 25.1 공통 원칙

- 구현 범위를 임의로 확장하지 않는다.
- 테스트 케이스 수정 / 추가 / 삭제 / 일괄 승인·반려 기능을 구현하지 않는다.
- Excel, PDF 출력 기능을 구현하지 않는다.
- Notion, Google Drive, GitHub 연동을 구현하지 않는다.
- AI 결과에는 반드시 Evidence를 포함한다.
- Evidence 원문은 실제 기획서에서 검증한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- Controller에 비즈니스 로직을 작성하지 않는다.
- 프롬프트는 리소스 파일로 분리한다.
- 데이터베이스 스키마는 Flyway로 관리한다.
- 외부 Jira 호출은 인터페이스 뒤에 숨긴다 (목업/실연동 교체 가능).
- AI 호출은 데이터베이스 트랜잭션 내부에서 실행하지 않는다.
- 승인되지 않은 테스트 케이스는 CSV에 포함하지 않는다.
- AI가 자동으로 Jira Issue를 생성하지 않게 한다. Jira Issue는 QA 사용자의 명시적인 요청으로만 생성한다.
- 구현되지 않은 기능을 동작하는 것처럼 작성하지 않는다.

## 25.2 백엔드 패키지 구조

```text
com.example.gameqacopilot
├── common
│   ├── config
│   ├── exception
│   ├── response
│   ├── security
│   └── file
├── user
├── project
├── document
│   ├── controller / service / parser / repository / entity / dto
├── analysis
│   ├── controller / service / prompt / validator / loop / repository / entity / dto
├── requirement
├── testcase
├── ambiguity
├── evidence
├── output
└── integration
    └── jira
```

Evidence는 별도 테이블을 사용하지 않더라도 독립된 도메인 및 DTO로 관리한다.
`analysis.loop` 패키지에 Actor–Evaluator 루프 및 Lats_Loop_Log 관련 로직을 둔다.

## 25.3 프론트엔드 구조

```text
src
├── app
├── api
├── components
│   ├── pdf / evidence / testCase / loopProgress
├── features
│   ├── auth / projects / documents / analyses / testCases / ambiguities / outputs
├── hooks
├── pages
├── schemas
├── types
└── utils
```

---

# 26. 세부 작업 목록

## 작업 1. 사용자 인증 및 역할 분리

**완료 조건**: 로그인 가능 / 현재 사용자 조회 / QA만 프로젝트 생성·PDF 업로드·분석 요청·승인·반려 가능 / USER는 조회만 가능 / 권한 없는 API 요청 시 403 반환

## 작업 2. 프로젝트 생성 및 조회

**완료 조건**: 생성·목록·상세 조회 / 역할별 UI 표시 / 생성자 User 연결

## 작업 3. PDF 기획서 업로드

**완료 조건**: PDF 업로드 / 형식·크기 검증 / 원본 저장 / 페이지 수 저장 / 페이지별 텍스트 추출 / 페이지 이미지 생성 / 텍스트 좌표 저장 / 처리 상태·실패 사유 저장

## 작업 4. 기획서 원문 조회

**완료 조건**: PDF 미리보기 / 페이지 이동 / 페이지별 추출 텍스트 조회 / 페이지별 요소 위치 정보 조회

## 작업 5. AI 분석 DTO 및 JSON 스키마

**완료 조건**: 문서 요약·CategoryTree·Requirement·TestCase(10컬럼)·Ambiguity·Evidence DTO / Enum 검증 / 잘못된 JSON 처리 / 정상 역직렬화 테스트

## 작업 6. 기능 분류 추출

**완료 조건**: 기능 계층 구조 생성 / 동일 기능 분류명 통일 / 중분류 없는 경우 `-` 처리 / 요구사항·테스트 케이스가 동일 분류 체계 사용

## 작업 7. 요구사항 추출

**완료 조건**: 요구사항 목록 생성 / 분류 필드 포함 / 원문 근거·페이지 번호 포함 / EXPLICIT·INFERRED·UNSUPPORTED 구분 / 결과 저장

## 작업 8. 실무형 테스트 케이스 생성

**완료 조건**: displayOrder / 대·중·소분류 / 테스트 항목 / 사전조건 / 테스트 스텝 / 기대결과 / Evidence / 비고 생성 / testType·우선순위·신뢰도 포함 / 관련 요구사항 연결 / 중복 기본 검증

## 작업 9. 모호한 요구사항 생성

**완료 조건**: 질문 목록 생성 / 관련 요구사항 연결 / 영향도·심각도 포함 / 원문 근거 포함 / 임의 확정 금지 / 관련 테스트 케이스 비고 연동

## 작업 10. Evidence 검증

**완료 조건**: 페이지 범위 검증 / 원문 일치 검증(EXACT·PARTIAL·SIMILAR·NOT_FOUND) / NOT_FOUND 시 UNSUPPORTED 처리 / 검토 필요 상태 표시 / 검증 결과 저장

## 작업 11. 테스트 시트 화면

**완료 조건**: 10컬럼 표시 / 가로 스크롤 / 셀 줄바꿈 / 행 선택 / 선택 행 상세 표시 / Evidence 요약 표시 / 비고 태그 표시 / 승인·반려 버튼 제공

## 작업 12. Evidence 원문 연동

**완료 조건**: 원문 페이지·문장 표시 / Evidence 유형·검증 상태 표시 / PDF 페이지 이동 / 가능한 경우 하이라이트 / 여러 근거 선택 / 좌표 없어도 페이지·원문 표시

## 작업 13. 승인 및 반려

**완료 조건**: 개별 승인 / 개별 반려 / 검토자·시각 저장 / 반려 사유 저장 / USER 승인·반려 불가 / 일괄 처리 미구현 확인

## 작업 14. CSV 다운로드

**완료 조건**: 승인 항목만 포함 / 10컬럼 순서 / displayOrder 정렬 / 목록 데이터 줄바꿈 변환 / Evidence 페이지·원문 포함 / 비고 포함 / RFC 4180 이스케이프 / Output 기록 / USER·QA 다운로드 가능

## 작업 15. 모호 요구사항 Markdown 다운로드

**완료 조건**: 질문·영향·심각도·근거 포함 / Output 기록 / 다운로드 가능

## 작업 16. Actor–Evaluator 루프 및 로그

**완료 조건**: 초안 생성–평가–재생성 루프 / 통과 기준·최대 회차 설정 / Lats_Loop_Log 기록 / Output 상태 전이(PENDING→SUCCESS/FAILED) / 프론트 진행 상태 표시

## 작업 17. Jira Issue 연동

**완료 조건**: QA만 생성 가능 / Issue 미리보기 / 질문·영향도·근거·관련 요구사항 포함 / 1차 목업 게시 / 2차 실연동 시 Key·URL 저장 / Output 기록 / 중복 생성 경고 / 실패 시 재시도

---

# 27. 핵심 인수 조건

다음 조건을 모두 충족할 때 MVP가 완료된 것으로 본다.

1. 사용자가 로그인할 수 있다.
2. USER와 QA 역할이 구분된다.
3. QA만 프로젝트를 생성할 수 있다.
4. QA만 PDF 기획서를 업로드할 수 있다.
5. 시스템이 PDF의 텍스트와 이미지를 처리할 수 있다.
6. QA가 AI 분석을 요청할 수 있다.
7. AI가 테스트 케이스를 10개 출력 항목에 맞춰 생성한다.
8. 각 테스트 케이스에 대분류, 중분류, 소분류가 포함된다.
9. 분류는 테스트 유형이 아니라 게임 기능 계층으로 생성된다.
10. 각 테스트 항목은 하나의 검증 목적만 가진다.
11. 사전조건이 없는 경우 `-`로 표시된다.
12. 테스트 스텝이 번호 순서로 생성된다.
13. 기대결과가 실제 검증 가능한 문장으로 생성된다.
14. AI가 모호하거나 누락된 요구사항을 생성한다.
15. 모든 테스트 케이스에 Evidence 정보가 포함된다.
16. Evidence에는 최소한 페이지 번호와 원문이 포함된다.
17. Evidence가 없는 경우 임의의 원문을 만들지 않는다.
18. Evidence가 없는 테스트는 비고에 QA 검토 필요가 표시된다.
19. 백엔드가 Evidence 원문 존재 여부를 검증한다.
20. 프론트에 10개 컬럼의 테스트 케이스 표가 표시된다.
21. 테스트 케이스 선택 시 전체 상세 내용을 확인할 수 있다.
22. 원문 근거 선택 시 해당 PDF 페이지로 이동한다.
23. 가능한 경우 근거 영역이 하이라이트된다.
24. QA 사용자는 테스트 케이스를 개별 승인/반려할 수 있다.
25. 일반 사용자는 테스트 케이스를 조회만 할 수 있다.
26. 승인 및 반려 결과가 데이터베이스에 저장된다.
27. 승인된 테스트 케이스만 CSV 문서에 포함된다.
28. CSV가 화면과 동일한 10컬럼 순서를 사용한다.
29. 모호한 요구사항을 Markdown으로 다운로드할 수 있다.
30. 모호한 요구사항은 QA 요청으로 Jira Issue(또는 목업)에 등록할 수 있다.
31. Jira Issue 생성 결과가 Output 테이블에 저장된다.
32. AI가 자동으로 Jira Issue를 생성하지 않는다.
33. 산출물 생성 루프의 이력이 Lats_Loop_Log에 저장되고 조회할 수 있다.

---

# 28. 발표 및 포트폴리오 핵심 메시지 (v0.1 계승 + 갱신)

## 문제

게임 기획서는 자연어, 표, 이미지, UI 화면이 혼합되어 있어 QA 담당자가 기능 조건과 예외 상황을 다시 해석해야 한다. 일반적인 AI 테스트 케이스 생성 도구는 결과만 제공하므로, 생성된 테스트가 실제 기획서의 어떤 내용을 근거로 만들어졌는지 확인하기 어렵다.

## 해결

PDF 기획서를 AI가 분석하여 실무형 QA 테스트 시트를 생성하고, 각 결과를 기획서 원문과 연결한다. QA 담당자는 생성된 테스트와 원문 근거를 한 화면에서 비교한 후 승인하거나 반려할 수 있다.

## 신뢰성

```text
AI 생성 결과
→ 기획서 Evidence 연결
→ 원문 존재 여부 백엔드 검증
→ Actor–Evaluator 자기 검증 루프 (이력 시각화)
→ QA 승인 또는 반려
→ 승인된 결과만 문서화
```

## 자동화

모호한 요구사항은 Jira Issue로 전환하여 기획자와 개발자에게 실제 확인 작업을 요청할 수 있다.

## 핵심 메시지

```text
이 프로젝트는 AI가 테스트 케이스를 생성하는 데서 끝나지 않는다.

AI가 생성한 테스트 케이스의 근거를 기획서 원문에서 추적하고,
AI가 스스로 검증·교정한 과정을 기록하며,
QA가 직접 검증한 결과만 업무 문서와 Jira 협업 흐름으로 연결한다.
```

---

# 29. 최종 작업 지시

```text
1.  Spring Boot, React, PostgreSQL 기반 프로젝트를 구성한다.
2.  USER와 QA 역할을 구현한다. QA만 프로젝트 생성, PDF 업로드, 분석 요청,
    승인, 반려, 산출물 생성, Jira 생성을 수행할 수 있게 한다.
3.  PDF 기획서를 페이지 단위 텍스트와 이미지로 처리하고
    페이지별 원문과 위치 정보를 저장한다.
4.  AI 응답은 반드시 구조화된 JSON으로 반환받는다.
5.  AI 테스트 케이스 출력 스키마는 실제 QA 테스트 시트 형태로 한다.
    최종 출력 컬럼 순서 고정:
    No / 대분류 / 중분류 / 소분류 / 테스트 항목 / 사전조건 /
    테스트 스텝 / 기대결과 / 기획서 원문 근거 / 비고
6.  대분류, 중분류, 소분류는 테스트 유형이 아니라
    게임 기능의 계층 구조로 생성한다.
7.  정상, 경계값, 예외 등의 유형은 내부 testType 필드로 별도 관리한다.
8.  분석 파이프라인은 기능 분류 추출 → 요구사항 → 테스트 케이스 →
    모호성 순서로 수행하고 분류명을 재사용한다.
9.  No는 데이터베이스 ID가 아니라 displayOrder를 사용한다.
10. 테스트 스텝, 사전조건, 기대결과, Evidence, 비고는 JSONB 구조를 사용한다.
    분류·검색용 값은 관계형 컬럼으로 둔다.
11. 테스트 케이스마다 최소 하나 이상의 Evidence를 연결한다.
12. Evidence가 존재하지 않을 경우 원문을 임의 생성하지 않고
    UNSUPPORTED 및 QA 검토 필요 상태로 저장한다.
13. AI가 반환한 Evidence가 실제 기획서에 존재하는지 백엔드에서 검증한다.
    (EXACT / PARTIAL / SIMILAR / NOT_FOUND)
14. 프론트는 테스트 케이스 표와 PDF 원문을 동시에 표시한다.
15. 기획서 원문 근거 선택 시 해당 PDF 페이지로 이동한다.
    가능한 경우 boundingBox로 원문 영역을 강조한다(선택 기능).
    좌표가 없더라도 페이지 번호와 원문 문장은 반드시 제공한다.
16. AI 테스트 케이스 수정 및 추가 기능은 구현하지 않는다.
17. 검토 기능은 개별 승인과 개별 반려만 제공한다. 일괄 처리는 구현하지 않는다.
18. 승인 및 반려 결과를 TestCase에 저장한다.
19. TestCaseRevision 테이블은 생성하지 않는다.
20. 승인된 테스트 케이스만 CSV 문서로 출력하며,
    CSV는 화면과 동일한 10개 컬럼 순서를 유지한다.
21. 모호한 요구사항은 Markdown 다운로드와 Jira Issue 생성을 지원한다.
22. 관련 정책이 정의되지 않은 경우 비고에 '기획 확인 필요'를 표시하고
    Ambiguity 데이터와 연결한다.
23. 모호한 요구사항은 QA 사용자의 명시적인 요청에 의해서만
    Jira Issue로 생성한다. AI가 자동으로 생성하지 않는다.
24. Jira 연동은 1차 목업 게시 → 2차 실제 API 연동 순서로 구현하고,
    호출부는 인터페이스로 추상화한다.
25. 문서 출력과 Jira 결과는 Output 테이블에서 통합 관리하고,
    산출물 생성의 Actor–Evaluator 루프 이력은 Lats_Loop_Log에 기록한다.
26. 외부 연동 실패가 테스트 케이스 승인 결과에 영향을 주지 않게 한다.
27. 각 단계마다 단위 테스트와 통합 테스트를 작성한다.
28. 구현되지 않은 기능을 동작하는 것처럼 작성하지 않는다.
29. README에 실행 방법, 환경 변수, 구현 범위, 보류 범위를 명확히 기록한다.
```
