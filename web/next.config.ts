import type { NextConfig } from "next";
import { config as dotenvConfig } from "dotenv";

dotenvConfig({ path: "../.env" });

const nextConfig: NextConfig = {
  output: "standalone",
};

export default nextConfig;
