"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { Spinner } from "@/components/ui/spinner";

const MESSAGE_DELAY_MS = 1000;

export function LoadingIndicator({
  message,
  className,
  spinnerClassName,
}: {
  message: string;
  className?: string;
  spinnerClassName?: string;
}) {
  const [showMessage, setShowMessage] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setShowMessage(true), MESSAGE_DELAY_MS);
    return () => clearTimeout(timer);
  }, []);

  return (
    <span className={cn("inline-flex items-center justify-center gap-2", className)}>
      <Spinner className={spinnerClassName} />
      <span aria-live="polite">{showMessage ? message : null}</span>
    </span>
  );
}
