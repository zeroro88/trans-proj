export type JobStatus =
  | "QUEUED"
  | "PARSING"
  | "CHUNKING"
  | "TRANSLATING"
  | "MERGING"
  | "DONE"
  | "FAILED";

export interface Segment {
  index: number;
  source: string;
  target: string;
  blockType?: string | null;
}

export interface JobSummary {
  id: string;
  status: JobStatus;
  progress: number;
  sourceLang: string;
  targetLang: string;
  originalFilename: string;
  errorCode: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface JobStatusResponse {
  id: string;
  status: JobStatus;
  progress: number;
  sourceLang: string;
  targetLang: string;
  errorCode: string | null;
  errorMessage: string | null;
  segments: Segment[];
}
