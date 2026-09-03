import type { NextConfig } from "next";
import { config as dotenvConfig } from "dotenv";
import path from "path";

dotenvConfig({ path: "../.env" });

const nextConfig: NextConfig = {
  outputFileTracingRoot: path.join(__dirname, "../"),
  ...(!process.env.VERCEL ? { output: "standalone" as const } : {}),
};

export default nextConfig;
