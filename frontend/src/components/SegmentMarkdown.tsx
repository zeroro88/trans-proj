import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import "./SegmentMarkdown.css";

/** Renders segment text as Markdown (MinerU / LLM often emit MD). */
export function SegmentMarkdown({ children }: { children: string }) {
  return (
    <div className="segment-md">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
    </div>
  );
}
