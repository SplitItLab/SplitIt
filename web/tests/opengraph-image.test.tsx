// @vitest-environment node
import { describe, expect, it, vi } from "vitest";

vi.mock("next/font/google", () => ({
  Geist: () => ({ variable: "" }),
  Geist_Mono: () => ({ variable: "" }),
}));

import OpengraphImage, { alt, contentType, size } from "../app/opengraph-image";
import { metadata } from "../app/layout";

describe("vista previa del link", () => {
  it("genera una imagen PNG de 1200x630 con alt", async () => {
    expect(size).toEqual({ width: 1200, height: 630 });
    expect(contentType).toBe("image/png");
    expect(alt).toContain("SplitIt");

    const response = await OpengraphImage();
    expect(response.headers.get("content-type")).toBe("image/png");
    const bytes = new Uint8Array(await response.arrayBuffer());
    expect(Array.from(bytes.slice(1, 4))).toEqual([0x50, 0x4e, 0x47]);
  });

  it("indica título y descripción de la app, sin tocar los íconos", () => {
    expect(metadata.title).toBe("SplitIt");
    expect(String(metadata.description).length).toBeGreaterThan(20);
    expect(metadata.openGraph).toMatchObject({ title: "SplitIt", siteName: "SplitIt" });
    expect(metadata.icons).toBeDefined();
  });
});
