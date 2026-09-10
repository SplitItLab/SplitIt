import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from "fs";
import { tmpdir } from "os";
import path from "path";
import { describe, expect, it } from "vitest";
import { resolveOutputFileTracingRoot } from "../tracing-root";

describe("resolveOutputFileTracingRoot", () => {
  it("usa el directorio del proyecto cuando no hay docker-compose en el padre (build Docker)", () => {
    const root = mkdtempSync(path.join(tmpdir(), "splitit-web-"));
    const projectDir = path.join(root, "app");
    mkdirSync(projectDir);

    try {
      expect(resolveOutputFileTracingRoot(projectDir)).toBe(projectDir);
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  });

  it("usa el padre cuando es un monorepo con docker-compose.yml", () => {
    const repoRoot = mkdtempSync(path.join(tmpdir(), "splitit-repo-"));
    const projectDir = path.join(repoRoot, "web");
    mkdirSync(projectDir);
    writeFileSync(path.join(repoRoot, "docker-compose.yml"), "services: {}\n");

    try {
      expect(resolveOutputFileTracingRoot(projectDir)).toBe(repoRoot);
    } finally {
      rmSync(repoRoot, { recursive: true, force: true });
    }
  });
});
