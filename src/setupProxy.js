// 백엔드 연동 전, 로그인 화면 힌트에 표시된 테스트 계정으로 프론트만 단독 구동/테스트하기 위한 mock API.
// 실제 백엔드가 붙으면 이 파일을 삭제하면 된다 (react-scripts가 존재 시 자동으로 로드함).
const express = require('express');

const ACCOUNTS = {
  admin: { pw: '1234', role: 'admin' },
  user1: { pw: '1234', role: 'user' },
};

module.exports = function (app) {
  app.use(express.json());

  app.post('/api/auth/login', (req, res) => {
    const { id, pw } = req.body || {};
    const account = ACCOUNTS[id];
    if (!account || account.pw !== pw) {
      return res.status(401).json({ message: '아이디 또는 비밀번호가 올바르지 않습니다.' });
    }
    res.json({ token: `mock-token-${id}`, role: account.role });
  });

  // ResultPage가 sessionId 'mock-001'을 내장 목 데이터로 렌더링하므로 분석 요청은 항상 이 세션으로 응답한다.
  app.post('/api/analyze/text', (req, res) => {
    res.json({ sessionId: 'mock-001' });
  });

  app.post('/api/analyze/pdf', (req, res) => {
    res.json({ sessionId: 'mock-001' });
  });

  app.patch('/api/analyze/:sessionId/testcases/:tcId', (req, res) => {
    res.json({ success: true });
  });

  app.delete('/api/analyze/:sessionId/testcases/:tcId', (req, res) => {
    res.json({ success: true });
  });
};
