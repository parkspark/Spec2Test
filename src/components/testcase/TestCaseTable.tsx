import React from 'react';
import { TestCase, TestCaseStatus } from '../../types';
import ApprovalControl from './ApprovalControl';

interface Props {
  testCases: TestCase[];
  isAdmin: boolean;
  onStatusChange: (tcId: string, status: TestCaseStatus, comment?: string) => void;
  onDelete: (tcId: string) => void;
}

const CATEGORY_LABEL: Record<string, string> = {
  NORMAL: '정상',
  BOUNDARY: '경계값',
  EXCEPTION: '예외',
};

const PRIORITY_LABEL: Record<string, string> = {
  P1: 'P1 Critical',
  P2: 'P2 High',
  P3: 'P3 Normal',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: '미검토',
  APPROVED: '승인',
  REJECTED: '반려',
};

const TestCaseTable = ({ testCases, isAdmin, onStatusChange, onDelete }: Props) => {
  if (testCases.length === 0) {
    return (
      <div className="empty-state">
        <p>생성된 테스트 케이스가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table className="tc-table">
        <thead>
          <tr>
            <th>TC-ID</th>
            <th>카테고리</th>
            <th>기능 영역</th>
            <th>테스트 항목</th>
            <th>전제조건</th>
            <th>기대 결과</th>
            <th>우선순위</th>
            <th>근거</th>
            <th>상태</th>
            {isAdmin && <th>액션</th>}
          </tr>
        </thead>
        <tbody>
          {testCases.map(tc => (
            <tr key={tc.id} className={`tc-row status-${tc.status.toLowerCase()}`}>
              <td>
                <span className="tc-id-badge">{tc.id}</span>
              </td>
              <td>
                <span className={`badge category-${tc.category.toLowerCase()}`}>
                  {CATEGORY_LABEL[tc.category]}
                </span>
              </td>
              <td className="td-area">{tc.featureArea}</td>
              <td className="td-title">{tc.title}</td>
              <td className="td-pre">{tc.precondition}</td>
              <td className="td-result">{tc.expectedResult}</td>
              <td>
                <span className={`badge priority-${tc.priority.toLowerCase()}`}>
                  {PRIORITY_LABEL[tc.priority]}
                </span>
              </td>
              <td className="td-evidence">
                {tc.evidenceRef.map((ref, i) => (
                  <span key={i} className="evidence-chip" title={ref.excerpt}>
                    {ref.sectionTitle}
                  </span>
                ))}
              </td>
              <td>
                <span className={`badge status-badge-${tc.status.toLowerCase()}`}>
                  {STATUS_LABEL[tc.status]}
                </span>
                {tc.comment && (
                  <p className="tc-comment">{tc.comment}</p>
                )}
              </td>
              {isAdmin && (
                <td className="td-action">
                  <ApprovalControl
                    tcId={tc.id}
                    currentStatus={tc.status}
                    onStatusChange={onStatusChange}
                    onDelete={onDelete}
                  />
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default TestCaseTable;
