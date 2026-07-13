import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Navbar from '../components/Navbar';

// User(조회 전용)가 관리자 전용 페이지에 접근했을 때 보여지는 안내 페이지
const NoAccessPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  return (
    <>
      <Navbar />
      <main className="noaccess-page">
        <div className="noaccess-card">
          <span className="noaccess-icon">🔒</span>
          <h2 className="noaccess-title">접근 권한이 없습니다</h2>
          <p className="noaccess-desc">
            기획서 입력은 QA 관리자만 이용할 수 있습니다.
            {user?.role === 'user' && ' 조회 전용 계정으로 로그인되어 있습니다.'}
          </p>
          <p className="noaccess-sub">
            분석 결과 페이지 링크를 전달받으면 조회할 수 있습니다.
          </p>
          <button className="btn-back" onClick={() => navigate(-1)}>
            ← 이전으로
          </button>
        </div>
      </main>
    </>
  );
};

export default NoAccessPage;
