import React, { useRef, useState, DragEvent } from 'react';

interface PdfDropZoneProps {
  file: File | null;
  onFileChange: (file: File | null) => void;
  disabled?: boolean;
}

const MAX_SIZE_MB = 50;

const PdfDropZone = ({ file, onFileChange, disabled }: PdfDropZoneProps) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validate = (f: File): boolean => {
    if (f.type !== 'application/pdf') {
      setError('PDF 파일만 업로드할 수 있습니다.');
      return false;
    }
    if (f.size > MAX_SIZE_MB * 1024 * 1024) {
      setError(`파일 크기는 ${MAX_SIZE_MB}MB 이하여야 합니다.`);
      return false;
    }
    setError(null);
    return true;
  };

  const handleFile = (f: File) => {
    if (validate(f)) onFileChange(f);
  };

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    if (disabled) return;
    const f = e.dataTransfer.files[0];
    if (f) handleFile(f);
  };

  const onDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!disabled) setIsDragging(true);
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  };

  return (
    <div className="dropzone-wrap">
      <div
        className={`dropzone ${isDragging ? 'dragging' : ''} ${disabled ? 'disabled' : ''}`}
        onClick={() => !disabled && inputRef.current?.click()}
        onDrop={onDrop}
        onDragOver={onDragOver}
        onDragLeave={() => setIsDragging(false)}
      >
        <span className="dropzone-icon">📄</span>
        <p className="dropzone-text">PDF를 드래그하거나 클릭하여 업로드</p>
        <p className="dropzone-sub">최대 {MAX_SIZE_MB}MB · PDF 파일만 허용</p>
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf"
          style={{ display: 'none' }}
          onChange={e => {
            const f = e.target.files?.[0];
            if (f) handleFile(f);
            e.target.value = '';
          }}
        />
      </div>

      {error && <p className="dropzone-error">⚠ {error}</p>}

      {file && (
        <div className="file-preview">
          <span className="file-icon">📎</span>
          <div className="file-info">
            <span className="file-name">{file.name}</span>
            <span className="file-size">{formatSize(file.size)}</span>
          </div>
          {!disabled && (
            <button
              className="file-remove"
              onClick={() => { onFileChange(null); setError(null); }}
              title="파일 제거"
            >
              ✕
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default PdfDropZone;
