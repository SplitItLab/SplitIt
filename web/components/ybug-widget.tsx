"use client";

import { useEffect } from "react";

declare global {
  interface Window {
    ybug_settings?: { id: string };
  }
}

export function YbugWidget() {
  useEffect(() => {
    const id = process.env.NEXT_PUBLIC_YBUG_ID;
    if (!id || window.ybug_settings) return;

    window.ybug_settings = { id };
    const script = document.createElement("script");
    script.async = true;
    script.src = `https://widget.ybug.io/button/${id}.js`;
    document.body.appendChild(script);
  }, []);

  return null;
}
