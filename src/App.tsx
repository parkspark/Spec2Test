import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import AdminRoute   from './components/AdminRoute';
import LoginPage    from './pages/LoginPage';
import InputPage    from './pages/InputPage';
import ResultPage   from './pages/ResultPage';
import NoAccessPage from './pages/NoAccessPage';
import './styles.css';

const App = () => (
  <AuthProvider>
    <BrowserRouter>
      <Routes>
        {/* 공개 라우트 */}
        <Route path="/login" element={<LoginPage />} />

        {/* 기획서 입력 — QA Admin 전용 */}
        <Route
          path="/"
          element={
            <AdminRoute><InputPage /></AdminRoute>
          }
        />

        {/* 분석 결과 조회 — 로그인한 User·Admin 모두 가능 */}
        <Route
          path="/result/:sessionId"
          element={
            <PrivateRoute><ResultPage /></PrivateRoute>
          }
        />

        {/* User가 관리자 전용 페이지 접근 시 안내 */}
        <Route
          path="/no-access"
          element={
            <PrivateRoute><NoAccessPage /></PrivateRoute>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </AuthProvider>
);

export default App;
