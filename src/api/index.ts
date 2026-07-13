import { TestCaseStatus, AnalysisResult } from '../types';

// =============================================
// ⚠️ 백엔드 endpoint 미정 — 확정 시 아래 상수만 수정
// =============================================
const BASE_URL = process.env.REACT_APP_API_URL || '';

const ENDPOINTS = {
  LOGIN:          `${BASE_URL}/api/auth/login`,
  ANALYZE_TEXT:   `${BASE_URL}/api/analyze/text`,
  ANALYZE_PDF:    `${BASE_URL}/api/analyze/pdf`,
  RESULT:         (id: string) => `${BASE_URL}/api/analyze/${id}/result`,
  STATUS:         (id: string) => `${BASE_URL}/api/analyze/${id}/status`,
  TESTCASE_PATCH: (sessionId: string, tcId: string) =>
                    `${BASE_URL}/api/analyze/${sessionId}/testcases/${tcId}`,
  TESTCASE_DELETE:(sessionId: string, tcId: string) =>
                    `${BASE_URL}/api/analyze/${sessionId}/testcases/${tcId}`,
};

// 공통 헤더
const authHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('qa_token');
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

// 로그인
export const apiLogin = async (id: string, pw: string) => {
  const res = await fetch(ENDPOINTS.LOGIN, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id, pw }),
  });
  if (!res.ok) throw new Error('로그인 실패');
  return res.json() as Promise<{ token: string; role: 'user' | 'admin' }>;
};

// 텍스트 분석
export const apiAnalyzeText = async (content: string) => {
  const res = await fetch(ENDPOINTS.ANALYZE_TEXT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ content }),
  });
  if (!res.ok) throw new Error('분석 요청 실패');
  return res.json() as Promise<{ sessionId: string }>;
};

// PDF 분석
export const apiAnalyzePdf = async (file: File) => {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch(ENDPOINTS.ANALYZE_PDF, {
    method: 'POST',
    headers: authHeaders(),
    body: form,
  });
  if (!res.ok) throw new Error('PDF 업로드 실패');
  return res.json() as Promise<{ sessionId: string }>;
};

// 결과 조회
export const apiGetResult = async (sessionId: string): Promise<AnalysisResult> => {
  const res = await fetch(ENDPOINTS.RESULT(sessionId), {
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error('결과 조회 실패');
  return res.json();
};

// 테스트 케이스 승인/반려
export const apiPatchTestCase = async (
  sessionId: string,
  tcId: string,
  status: TestCaseStatus,
  comment?: string
) => {
  const res = await fetch(ENDPOINTS.TESTCASE_PATCH(sessionId, tcId), {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ status, comment }),
  });
  if (!res.ok) throw new Error('상태 변경 실패');
  return res.json();
};

// 테스트 케이스 삭제
export const apiDeleteTestCase = async (sessionId: string, tcId: string) => {
  const res = await fetch(ENDPOINTS.TESTCASE_DELETE(sessionId, tcId), {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error('삭제 실패');
  return res.json();
};
