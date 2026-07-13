export type UserRole = 'user' | 'admin';

export interface AuthUser {
  id: string;
  role: UserRole;
}

export type TestCaseCategory = 'NORMAL' | 'BOUNDARY' | 'EXCEPTION';
export type TestCasePriority = 'P1' | 'P2' | 'P3';
export type TestCaseStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type AnalysisStatus = 'QUEUED' | 'PARSING' | 'ANALYZING' | 'GENERATING' | 'DONE' | 'ERROR';
export type InputMode = 'text' | 'pdf';

export interface EvidenceRef {
  sectionTitle: string;
  pageNumber?: number;
  excerpt: string;
}

export interface TestCase {
  id: string;
  category: TestCaseCategory;
  featureArea: string;
  title: string;
  precondition: string;
  steps: string[];
  expectedResult: string;
  priority: TestCasePriority;
  evidenceRef: EvidenceRef[];
  status: TestCaseStatus;
  comment?: string;
}

export interface AnalysisResult {
  sessionId: string;
  fileName?: string;
  inputType: InputMode;
  status: AnalysisStatus;
  testCases: TestCase[];
  createdAt: string;
}
