import { afterEach, describe, expect, it } from "vitest";

import { showAppToast, type ToastType } from "@/lib/toast";

// globals.css styles the toasts through these classes of nextjs-toast-notify.
describe("estilos de los toasts", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it.each<ToastType>(["success", "error", "warning", "info"])(
    "el toast de %s tiene las clases que usa el estilo de SplitIt",
    (type) => {
      showAppToast(type, "Agregamos «Cena»");

      const toast = document.querySelector(`.toast-container .toast-nextjs.${type}`);
      expect(toast?.querySelector(".check")).not.toBeNull();
      expect(toast?.querySelector(".text")).toHaveTextContent("Agregamos «Cena»");
      expect(toast?.querySelector(".close-toast")).not.toBeNull();
    }
  );
});
