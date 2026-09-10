import { existsSync } from "fs";
import path from "path";

export function resolveOutputFileTracingRoot(projectDir: string): string {
  const repoRoot = path.join(projectDir, "..");
  return existsSync(path.join(repoRoot, "docker-compose.yml")) ? repoRoot : projectDir;
}
