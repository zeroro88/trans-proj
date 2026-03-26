import { HomeOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, Card, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { fetchJobs } from "@/api/jobs";
import type { JobStatus, JobSummary } from "@/types";

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

function isTerminal(st: JobStatus): boolean {
  return st === "DONE" || st === "FAILED";
}

function fmtTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export function JobsListPage() {
  const { data, error, isLoading, refetch, isFetching } = useQuery({
    queryKey: ["jobs"],
    queryFn: fetchJobs,
    refetchInterval: (q) => {
      const list = q.state.data;
      if (!list?.length) {
        return 2000;
      }
      const anyActive = list.some((j) => !isTerminal(j.status));
      return anyActive ? 2000 : false;
    },
  });

  const columns: ColumnsType<JobSummary> = [
    {
      title: "文件名",
      dataIndex: "originalFilename",
      ellipsis: true,
      render: (name: string, row) => (
        <Link to={`/job/${row.id}`}>{name || row.id}</Link>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 140,
      render: (s: JobStatus) => (
        <Tag color={s === "FAILED" ? "red" : s === "DONE" ? "green" : "blue"}>{statusLabel(s)}</Tag>
      ),
    },
    {
      title: "进度",
      dataIndex: "progress",
      width: 90,
      render: (p: number) => `${p}%`,
    },
    {
      title: "语言",
      key: "lang",
      width: 100,
      render: (_, row) => (
        <Text type="secondary">
          {row.sourceLang} → {row.targetLang}
        </Text>
      ),
    },
    {
      title: "更新于",
      dataIndex: "updatedAt",
      width: 180,
      render: (iso: string) => fmtTime(iso),
    },
    {
      title: "操作",
      key: "act",
      width: 100,
      render: (_, row) => <Link to={`/job/${row.id}`}>详情</Link>,
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Space wrap>
        <Link to="/">
          <Button icon={<HomeOutlined />}>上传新文件</Button>
        </Link>
        <Button icon={<ReloadOutlined />} onClick={() => void refetch()} loading={isFetching}>
          刷新
        </Button>
      </Space>

      <Card>
        <Title level={4} style={{ marginTop: 0 }}>
          任务列表
        </Title>
        <Paragraph type="secondary">
          解析、翻译可能耗时较长，可在此查看所有任务状态；进行中列表会约每 2 秒自动刷新。
        </Paragraph>
        {error && <Paragraph type="danger">{(error as Error).message}</Paragraph>}
        <Table<JobSummary>
          rowKey="id"
          loading={isLoading && !data}
          dataSource={data ?? []}
          columns={columns}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>
    </Space>
  );
}
