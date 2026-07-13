import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AnalysisResult, TestCaseStatus } from '../types';
import { apiGetResult, apiPatchTestCase, apiDeleteTestCase } from '../api';
import { useAuth } from '../contexts/AuthContext';
import TestCaseTable from '../components/testcase/TestCaseTable';
import Navbar from '../components/Navbar';

// ─── 목 데이터 (백엔드 연동 전 UI 확인용) ───────────────────────
const MOCK_RESULT: AnalysisResult = {
  sessionId: 'mock-001',
  fileName: '게임기획서_v2.pdf',
  inputType: 'pdf',
  status: 'DONE',
  createdAt: new Date().toISOString(),
  testCases: [
    {
      id: 'TC-001',
      category: 'NORMAL',
      featureArea: '기본 이동',
      title: '캐릭터 기본 이동 확인',
      precondition: '게임 시작 후 캐릭터 생성 완료',
      steps: ['WASD 키 입력', '캐릭터 이동 확인'],
      expectedResult: '입력 방향으로 캐릭터가 부드럽게 이동한다',
      priority: 'P1',
      evidenceRef: [{ sectionTitle: '3.1 캐릭터 이동', excerpt: '플레이어는 WASD로 이동한다' }],
      status: 'PENDING',
    },
    {
      id: 'TC-002',
      category: 'EXCEPTION',
      featureArea: '전투 시스템',
      title: '공격 대상 없을 때 공격 시도',
      precondition: '주변에 적 없음',
      steps: ['공격 키(Z) 입력'],
      expectedResult: '공격 모션 없이 쿨다운 없이 무반응',
      priority: 'P2',
      evidenceRef: [{ sectionTitle: '4.2 전투', excerpt: '적 없을 시 공격 무효 처리' }],
      status: 'APPROVED',
    },
    {
      id: 'TC-003',
      category: 'BOUNDARY',
      featureArea: '인벤토리',
      title: '인벤토리 최대치 초과 아이템 획득',
      precondition: '인벤토리 슬롯 20/20 상태',
      steps: ['아이템 획득 시도'],
      expectedResult: '"인벤토리가 가득 찼습니다" 메시지 표시, 획득 불가',
      priority: 'P1',
      evidenceRef: [{ sectionTitle: '5.1 인벤토리', excerpt: '최대 슬롯 20개 제한' }],
      status: 'REJECTED',
      comment: '최대치 수치 기획서와 불일치, 재확인 필요',
    },
  ],
};
// ─────────────────────────────────────────────────────────────────

const ResultPage = () => {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate      = useNavigate();
  const { isAdmin }   = useAuth();

  const [result,  setResult]  = useState<AnalysisResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  useEffect(() => {
    if (!sessionId) return;
    setLoading(true);

    // 목 세션이면 목 데이터 사용
    if (sessionId === 'mock-001') {
      setTimeout(() => { setResult(MOCK_RESULT); setLoading(false); }, 600);
      return;
    }

    apiGetResult(sessionId)
      .then(data => setResult(data))
      .catch(() => setError('결과를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [sessionId]);

  const handleStatusChange = useCallback(
    async (tcId: string, status: TestCaseStatus, comment?: string) => {
      if (!sessionId || !result) return;
      try {
        await apiPatchTestCase(sessionId, tcId, status, comment);
        setResult(prev =>
          prev
            ? {
                ...prev,
                testCases: prev.testCases.map(tc =>
                  tc.id === tcId ? { ...tc, status, comment: comment ?? tc.comment } : tc
                ),
              }
            : prev
        );
      } catch {
        alert('상태 변경에 실패했습니다.');
      }
    },
    [sessionId, result]
  );

  const handleDelete = useCallback(
    async (tcId: string) => {
      if (!sessionId || !result) return;
      try {
        await apiDeleteTestCase(sessionId, tcId);
        setResult(prev =>
          prev
            ? { ...prev, testCases: prev.testCases.filter(tc => tc.id !== tcId) }
            : prev
        );
      } catch {
        alert('삭제에 실패했습니다.');
      }
    },
    [sessionId, result]
  );

  // 통계
  const stats = result
    ? {
        total:    result.testCases.length,
        approved: result.testCases.filter(t => t.status === 'APPROVED').length,
        rejected: result.testCases.filter(t => t.status === 'REJECTED').length,
        pending:  result.testCases.filter(t => t.status === 'PENDING').length,
      }
    : null;

  return (
    <>
      <Navbar />
      <main className="result-page">
        {loading && (
          <div className="loading-state">
            <div className="loading-spinner" />
            <p>분석 결과를 불러오는 중...</p>
          </div>
        )}

        {error && (
          <div className="error-state">
            <p>{error}</p>
            <button className="btn-back" onClick={() => navigate('/')}>
              ← 처음으로
            </button>
          </div>
        )}

        {result && !loading && (
          <>
            {/* 페이지 헤더 */}
            <div className="result-header">
              <div>
                <h2 className="page-title">분석 결과</h2>
                <p className="result-meta">
                  {result.fileName && <span>📄 {result.fileName}</span>}
                  <span>세션: {result.sessionId}</span>
                  <span>{new Date(result.createdAt).toLocaleString('ko-KR')}</span>
                </p>
              </div>
              <button className="btn-back" onClick={() => navigate('/')}>
                ← 새 분석
              </button>
            </div>

            {/* 통계 카드 */}
            {stats && (
              <div className="stats-bar">
                <div className="stat-card">
                  <span className="stat-num">{stats.total}</span>
                  <span className="stat-label">전체</span>
                </div>
                <div className="stat-card approved">
                  <span className="stat-num">{stats.approved}</span>
                  <span className="stat-label">승인</span>
                </div>
                <div className="stat-card rejected">
                  <span className="stat-num">{stats.rejected}</span>
                  <span className="stat-label">반려</span>
                </div>
                <div className="stat-card pending">
                  <span className="stat-num">{stats.pending}</span>
                  <span className="stat-label">미검토</span>
                </div>
              </div>
            )}

            {/* 권한 안내 */}
            {!isAdmin && (
              <div className="readonly-banner">
                👁 조회 전용 모드 — 승인/반려/삭제는 QA 관리자만 가능합니다.
              </div>
            )}

            {/* 테스트 케이스 표 */}
            <TestCaseTable
              testCases={result.testCases}
              isAdmin={isAdmin}
              onStatusChange={handleStatusChange}
              onDelete={handleDelete}
            />
          </>
        )}
      </main>
    </>
  );
};

export default ResultPage;
