import React, { useState } from 'react';
import { TestCaseStatus } from '../../types';

interface Props {
  tcId: string;
  currentStatus: TestCaseStatus;
  onStatusChange: (tcId: string, status: TestCaseStatus, comment?: string) => void;
  onDelete: (tcId: string) => void;
}

const ApprovalControl = ({ tcId, currentStatus, onStatusChange, onDelete }: Props) => {
  const [showComment, setShowComment] = useState(false);
  const [comment, setComment]         = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);

  const handleApprove = () => {
    onStatusChange(tcId, 'APPROVED');
    setShowComment(false);
  };

  const handleReject = () => {
    if (!showComment) { setShowComment(true); return; }
    onStatusChange(tcId, 'REJECTED', comment);
    setShowComment(false);
    setComment('');
  };

  const handleDelete = () => {
    if (!confirmDelete) { setConfirmDelete(true); return; }
    onDelete(tcId);
    setConfirmDelete(false);
  };

  return (
    <div className="approval-wrap">
      <div className="approval-btns">
        <button
          className={`btn-approve ${currentStatus === 'APPROVED' ? 'active' : ''}`}
          onClick={handleApprove}
          title="승인"
        >
          ✓ 승인
        </button>
        <button
          className={`btn-reject ${currentStatus === 'REJECTED' ? 'active' : ''}`}
          onClick={handleReject}
          title="반려"
        >
          ✕ 반려
        </button>
        <button
          className={`btn-delete ${confirmDelete ? 'confirm' : ''}`}
          onClick={handleDelete}
          title="삭제"
        >
          {confirmDelete ? '확인?' : '🗑'}
        </button>
      </div>

      {showComment && (
        <div className="comment-box">
          <textarea
            className="comment-textarea"
            placeholder="반려 사유를 입력하세요..."
            value={comment}
            onChange={e => setComment(e.target.value)}
            rows={2}
          />
          <div className="comment-actions">
            <button
              className="btn-comment-save"
              onClick={handleReject}
              disabled={!comment.trim()}
            >
              저장
            </button>
            <button
              className="btn-comment-cancel"
              onClick={() => { setShowComment(false); setComment(''); }}
            >
              취소
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApprovalControl;
