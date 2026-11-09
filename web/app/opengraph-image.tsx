import { ImageResponse } from "next/og";

export const alt = "SplitIt: dividí los gastos del grupo sin vueltas";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const PRIMARY = "#28c49e";

// Vista previa al compartir un link (WhatsApp, etc.): logo completo, nombre y qué es la app.
export default function OpengraphImage() {
  return new ImageResponse(
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        background: "#fcfcfe",
      }}
    >
      <div style={{ display: "flex", alignItems: "center" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            background: PRIMARY,
            borderRadius: 36,
            padding: "0 48px",
            height: 156,
            fontSize: 112,
            fontWeight: 800,
            color: "#ffffff",
          }}
        >
          Split
        </div>
        <div
          style={{
            display: "flex",
            marginLeft: 16,
            fontSize: 112,
            fontWeight: 800,
            color: "#000000",
          }}
        >
          It
        </div>
      </div>
      <div
        style={{ display: "flex", marginTop: 56, fontSize: 44, fontWeight: 600, color: "#3f4a55" }}
      >
        Dividí los gastos del grupo, sin vueltas
      </div>
    </div>,
    size
  );
}
