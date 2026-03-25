import type { JobStatusResponse } from "@/types";

export async function createJob(
  file: File,
  sourceLang: string,
  targetLang: string,
): Promise<{ jobId: string }> {
  const body = new FormData();
  body.append("file", file);
  body.append("sourceLang", sourceLang);
  body.append("targetLang", targetLang);
  const res = await fetch("/api/jobs", { method: "POST", body });
  if (!res.ok) {
    throw new Error(`上传失败: ${res.status}`);
  }
  return res.json() as Promise<{ jobId: string }>;
}

export async function fetchJob(id: string): Promise<JobStatusResponse> {
  const res = await fetch(`/api/jobs/${id}`);
  if (!res.ok) {
    throw new Error(`查询失败: ${res.status}`);
  }
  return res.json() as Promise<JobStatusResponse>;
}

export function exportUrl(id: string, format: "md" | "docx"): string {
  return `/api/jobs/${id}/export?format=${format}`;
}
