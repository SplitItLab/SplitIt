import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("nextjs-toast-notify", () => ({
  showToast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}));

import { showToast } from "nextjs-toast-notify";

import { showAppToast } from "@/lib/toast";

describe("showAppToast", () => {
  beforeEach(() => {
    vi.mocked(showToast.success).mockClear();
  });

  it("deja el texto común igual y escapa el HTML", () => {
    showAppToast("success", "Agregamos «Cena»");
    showAppToast("success", "Agregamos «<img>»");

    expect(showToast.success).toHaveBeenNthCalledWith(
      1,
      "Agregamos «Cena»",
      expect.objectContaining({ position: "top-right" })
    );
    expect(showToast.success).toHaveBeenNthCalledWith(
      2,
      "Agregamos «&lt;img&gt;»",
      expect.objectContaining({ position: "top-right" })
    );
  });
});
