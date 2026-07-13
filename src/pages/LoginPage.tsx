import React, { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const LoginPage = () => {
  const { login } = useAuth();
  const navigate  = useNavigate();

  const [id,       setId]       = useState('');
  const [pw,       setPw]       = useState('');
  const [error,    setError]    = useState<string | null>(null);
  const [loading,  setLoading]  = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!id.trim() || !pw.trim()) {
      setError('아이디와 비밀번호를 입력하세요.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const role = await login(id.trim(), pw);
      // QA Admin만 입력 페이지로, User는 접근 불가 안내로 이동
      navigate(role === 'admin' ? '/' : '/no-access', { replace: true });
    } catch {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <span className="login-icon">🎮</span>
          <h1 className="login-title">Game QA Copilot</h1>
          <p className="login-sub">QA 테스트 케이스 자동 생성 시스템</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">아이디</label>
            <input
              className="form-input"
              type="text"
              value={id}
              onChange={e => setId(e.target.value)}
              placeholder="아이디를 입력하세요"
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label">비밀번호</label>
            <input
              className="form-input"
              type="password"
              value={pw}
              onChange={e => setPw(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              autoComplete="current-password"
            />
          </div>

          {error && (
            <div className="login-error">
              <span>⚠</span> {error}
            </div>
          )}

          <button
            className="btn-login"
            type="submit"
            disabled={loading}
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className="login-hint">
          <p>테스트 계정</p>
          <p><strong>User</strong> — ID: user1 / PW: 1234</p>
          <p><strong>QA Admin</strong> — ID: admin / PW: 1234</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
