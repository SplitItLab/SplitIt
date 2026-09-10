import type { NextConfig } from "next";
import { config as dotenvConfig } from "dotenv";

dotenvConfig({ path: "../.env" });

const nextConfig: NextConfig = {
  outputFileTracingRoot: process.env.VERCEL ? path.join(__dirname, "..") : __dirname,
  ...(!process.env.VERCEL ? { output: "standalone" as const } : {}),
};

export default nextConfig;
