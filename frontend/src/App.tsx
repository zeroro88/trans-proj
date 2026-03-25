import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider, Layout, Typography, theme } from "antd";
import zhCN from "antd/locale/zh_CN";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { JobPage } from "@/pages/JobPage";
import { UploadPage } from "@/pages/UploadPage";

const queryClient = new QueryClient();

const { Header, Content } = Layout;
const { Title } = Typography;

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={{ algorithm: theme.defaultAlgorithm }}>
        <BrowserRouter>
          <Layout style={{ minHeight: "100vh" }}>
            <Header style={{ display: "flex", alignItems: "center", paddingInline: 24 }}>
              <Title level={4} style={{ color: "#fff", margin: 0 }}>
                本地 PDF 翻译（预览）
              </Title>
            </Header>
            <Content style={{ padding: 24, maxWidth: 1100, margin: "0 auto", width: "100%" }}>
              <Routes>
                <Route path="/" element={<UploadPage />} />
                <Route path="/job/:id" element={<JobPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Content>
          </Layout>
        </BrowserRouter>
      </ConfigProvider>
    </QueryClientProvider>
  );
}
