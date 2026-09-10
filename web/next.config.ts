import type { NextConfig } from "next";
import { config as dotenvConfig } from "dotenv";
import { resolveOutputFileTracingRoot } from "./tracing-root";

dotenvConfig({ path: "../.env" });

const nextConfig: NextConfig = {
  outputFileTracingRoot: resolveOutputFileTracingRoot(__dirname),
  ...(!process.env.VERCEL ? { output: "standalone" as const } : {}),
};

export default nextConfig;
