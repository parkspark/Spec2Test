import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Navbar = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) return null;

  // User는 입력 페이지(/) 접근 불가하므로 로고 클릭 시 안내 페이지로
  const handleBrandClick = () => navigate(isAdmin ? '/' : '/no-access');

  return (
    <nav className="navbar">
      <div className="navbar-brand" onClick={handleBrandClick}>
        <span className="navbar-icon">🎮</span>
        <span className="navbar-title">Game QA Copilot</span>
      </div>
      <div className="navbar-right">
        <span className="navbar-user">
          <span className={`role-badge ${isAdmin ? 'role-admin' : 'role-user'}`}>
            {isAdmin ? 'QA Admin' : 'User'}
          </span>
          {user.id}
        </span>
        <button className="btn-logout" onClick={handleLogout}>로그아웃</button>
      </div>
    </nav>
  );
};

export default Navbar;
