import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { AuthUser, UserRole } from '../types';
import { apiLogin } from '../api';

interface AuthContextType {
  user: AuthUser | null;
  login: (id: string, pw: string) => Promise<UserRole>;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(() => {
    // 최초 마운트 시 딱 한 번 실행 — localStorage에 저장된 로그인 세션 복원
    const token = localStorage.getItem('qa_token');
    const id    = localStorage.getItem('qa_id');
    const role  = localStorage.getItem('qa_role') as UserRole | null;
    // 토큰·아이디·권한이 모두 있어야 유효한 로그인 상태로 간주
    return token && id && role ? { id, role } : null;
  });

  const login = useCallback(async (id: string, pw: string): Promise<UserRole> => {
    const data = await apiLogin(id, pw);
    localStorage.setItem('qa_token', data.token);
    localStorage.setItem('qa_id',    id);
    localStorage.setItem('qa_role',  data.role);
    setUser({ id, role: data.role });
    return data.role;  // 로그인 후 role에 따라 페이지 분기가 가능하도록 반환
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('qa_token');
    localStorage.removeItem('qa_id');
    localStorage.removeItem('qa_role');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin: user?.role === 'admin' }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
