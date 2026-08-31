import { showToast } from "nextjs-toast-notify";

export type ToastType = "success" | "error" | "warning" | "info";

const TOAST_OPTIONS = {
  duration: 4000,
  progress: false,
  position: "top-right",
  transition: "fadeIn",
  icon: "",
  sound: false,
} as const;

export function showAppToast(type: ToastType, message: string) {
  showToast[type](message, TOAST_OPTIONS);
}
