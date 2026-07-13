import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

// QA Admin 전용 라우트.
// - 비로그인 → /login
// - 로그인했지만 User(조회 전용) → 접근 불가 안내로 리다이렉트
const AdminRoute = ({ children }: { children: React.ReactElement }) => {
  const { user, isAdmin } = useAuth();

  if (!user) return <Navigate to="/login" replace />;
  if (!isAdmin) return <Navigate to="/no-access" replace />;

  return children;
};

export default AdminRoute;
