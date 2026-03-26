import { InboxOutlined } from "@ant-design/icons";
import type { UploadProps } from "antd";
import { Button, Form, Select, Space, Typography, Upload, message } from "antd";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createJob } from "@/api/jobs";

const { Title, Paragraph } = Typography;
const { Dragger } = Upload;

const LANG_PAIRS = [
  { value: "ja-zh", source: "ja", target: "zh", label: "日语 → 中文" },
  { value: "zh-ja", source: "zh", target: "ja", label: "中文 → 日语" },
  { value: "en-zh", source: "en", target: "zh", label: "英语 → 中文" },
  { value: "zh-en", source: "zh", target: "en", label: "中文 → 英语" },
];

export function UploadPage() {
  const nav = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [lang, setLang] = useState("ja-zh");
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async () => {
    if (!file) {
      message.warning("请选择 PDF 文件");
      return;
    }
    const pair = LANG_PAIRS.find((p) => p.value === lang);
    if (!pair) {
      return;
    }
    setSubmitting(true);
    try {
      const { jobId } = await createJob(file, pair.source, pair.target);
      message.success("已提交，可在任务列表查看进度");
      nav(`/jobs`);
    } catch (e) {
      message.error(e instanceof Error ? e.message : "提交失败");
    } finally {
      setSubmitting(false);
    }
  };

  const uploadProps: UploadProps = {
    maxCount: 1,
    accept: "application/pdf,.pdf",
    beforeUpload: (f) => {
      setFile(f);
      return false;
    },
    onRemove: () => setFile(null),
  };

  return (
    <Space direction="vertical" size="large" style={{ width: "100%", maxWidth: 640 }}>
      <Space align="baseline" style={{ width: "100%", justifyContent: "space-between" }}>
        <Title level={3} style={{ margin: 0 }}>
          上传 PDF
        </Title>
        <Link to="/jobs">查看任务列表</Link>
      </Space>
      <Paragraph type="secondary">
        后端默认 <code>app.mock-pipeline=true</code> 时不调用 MinerU / vLLM，仅用于界面联调。接入真实服务请参阅仓库 README。
      </Paragraph>
      <Form layout="vertical">
        <Form.Item label="语言方向">
          <Select
            value={lang}
            onChange={setLang}
            options={LANG_PAIRS.map((p) => ({ value: p.value, label: p.label }))}
          />
        </Form.Item>
        <Form.Item label="PDF 文件">
          <Dragger {...uploadProps}>
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽 PDF 到此</p>
          </Dragger>
        </Form.Item>
        <Button type="primary" onClick={() => void onSubmit()} loading={submitting} block size="large">
          开始翻译
        </Button>
      </Form>
    </Space>
  );
}
