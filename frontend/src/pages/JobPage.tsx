import { DownloadOutlined, HomeOutlined } from "@ant-design/icons";
import { Button, Card, Col, Progress, Row, Space, Spin, Tag, Typography, theme } from "antd";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { exportUrl, fetchJob } from "@/api/jobs";
import type { JobStatus } from "@/types";

const { Title, Text, Paragraph } = Typography;

function statusLabel(s: JobStatus): string {
  const m: Record<JobStatus, string> = {
    QUEUED: "排队",
    PARSING: "解析（MinerU）",
    CHUNKING: "分段",
    TRANSLATING: "翻译（vLLM）",
    MERGING: "合并",
    DONE: "完成",
    FAILED: "失败",
  };
  return m[s] ?? s;
}

export function JobPage() {
  const { id } = useParams<{ id: string }>();
  const { token } = theme.useToken();

  const { data, error, isLoading } = useQuery({
    queryKey: ["job", id],
    queryFn: () => fetchJob(id!),
    enabled: Boolean(id),
    refetchInterval: (q) => {
      const st = q.state.data?.status;
      if (!st || st === "DONE" || st === "FAILED") {
        return false;
      }
      return 1500;
    },
  });

  if (!id) {
    return <Text type="danger">缺少任务 ID</Text>;
  }

  if (isLoading && !data) {
    return <Spin size="large" />;
  }

  if (error || !data) {
    return <Text type="danger">{(error as Error)?.message ?? "加载失败"}</Text>;
  }

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Space wrap>
        <Link to="/">
          <Button icon={<HomeOutlined />}>上传新文件</Button>
        </Link>
        {data.status === "DONE" && (
          <>
            <Button icon={<DownloadOutlined />} href={exportUrl(id, "md")}>
              下载 Markdown
            </Button>
            <Button icon={<DownloadOutlined />} href={exportUrl(id, "docx")}>
              下载 DOCX
            </Button>
          </>
        )}
      </Space>

      <Card>
        <Title level={4}>任务 {data.id}</Title>
        <Paragraph>
          <Tag color={data.status === "FAILED" ? "red" : data.status === "DONE" ? "green" : "blue"}>
            {statusLabel(data.status)}
          </Tag>
          <Text type="secondary">
            {" "}
            {data.sourceLang} → {data.targetLang}
          </Text>
        </Paragraph>
        <Progress percent={data.progress} status={data.status === "FAILED" ? "exception" : "active"} />
        {data.errorMessage && (
          <Paragraph type="danger">
            {data.errorCode ? `[${data.errorCode}] ` : ""}
            {data.errorMessage}
          </Paragraph>
        )}
      </Card>

      {data.segments.length > 0 && (
        <Title level={5}>对照预览</Title>
      )}
      {data.segments.map((seg) => (
        <Card key={seg.index} size="small" style={{ marginBottom: token.marginSM }}>
          <Text type="secondary">段落 {seg.index}</Text>
          <Row gutter={16} style={{ marginTop: 8 }}>
            <Col xs={24} md={12}>
              <Card size="small" title="原文" type="inner">
                <Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>{seg.source}</Paragraph>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card size="small" title="译文" type="inner">
                <Paragraph style={{ whiteSpace: "pre-wrap", marginBottom: 0 }}>{seg.target}</Paragraph>
              </Card>
            </Col>
          </Row>
        </Card>
      ))}
    </Space>
  );
}
