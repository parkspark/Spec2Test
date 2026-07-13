import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { InputMode } from '../types';
import { apiAnalyzeText, apiAnalyzePdf } from '../api';
import TextInput from '../components/upload/TextInput';
import PdfDropZone from '../components/upload/PdfDropZone';
import Navbar from '../components/Navbar';

const InputPage = () => {
  const navigate = useNavigate();

  const [mode,      setMode]      = useState<InputMode>('text');
  const [text,      setText]      = useState('');
  const [pdfFile,   setPdfFile]   = useState<File | null>(null);
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState<string | null>(null);

  const canSubmit = mode === 'text' ? text.trim().length > 0 : pdfFile !== null;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setLoading(true);
    setError(null);
    try {
      let sessionId: string;
      if (mode === 'text') {
        const res = await apiAnalyzeText(text);
        sessionId = res.sessionId;
      } else {
        const res = await apiAnalyzePdf(pdfFile!);
        sessionId = res.sessionId;
      }
      navigate(`/result/${sessionId}`);
    } catch (e: any) {
      setError(e.message || '분석 요청 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="input-page">
        <div className="page-header">
          <h2 className="page-title">기획서 분석</h2>
          <p className="page-desc">
            게임 기획서를 텍스트로 입력하거나 PDF 파일로 업로드하면
            AI가 QA 테스트 케이스를 자동으로 생성합니다.
          </p>
        </div>

        {/* 탭 */}
        <div className="tab-bar">
          <button
            className={`tab-btn ${mode === 'text' ? 'active' : ''}`}
            onClick={() => { setMode('text'); setError(null); }}
          >
            ✏️ 텍스트 입력
          </button>
          <button
            className={`tab-btn ${mode === 'pdf' ? 'active' : ''}`}
            onClick={() => { setMode('pdf'); setError(null); }}
          >
            📄 PDF 업로드
          </button>
        </div>

        {/* 입력 영역 */}
        <div className="input-card">
          {mode === 'text' ? (
            <TextInput value={text} onChange={setText} disabled={loading} />
          ) : (
            <PdfDropZone file={pdfFile} onFileChange={setPdfFile} disabled={loading} />
          )}
        </div>

        {error && (
          <div className="submit-error">⚠ {error}</div>
        )}

        <div className="submit-area">
          <button
            className="btn-submit"
            onClick={handleSubmit}
            disabled={!canSubmit || loading}
          >
            {loading ? (
              <><span className="spinner" /> 분석 요청 중...</>
            ) : (
              '분석 시작하기 →'
            )}
          </button>
        </div>
      </main>
    </>
  );
};

export default InputPage;
