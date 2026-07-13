import React from 'react';

interface TextInputProps {
  value: string;
  onChange: (val: string) => void;
  disabled?: boolean;
}

const TextInput = ({ value, onChange, disabled }: TextInputProps) => (
  <div className="text-input-wrap">
    <label className="input-label">기획서 내용을 직접 입력하세요</label>
    <textarea
      className="text-area"
      placeholder="게임 기획서 내용을 여기에 붙여넣거나 직접 입력하세요..."
      value={value}
      onChange={e => onChange(e.target.value)}
      disabled={disabled}
      rows={16}
    />
    <div className="char-count">{value.length.toLocaleString()}자</div>
  </div>
);

export default TextInput;
