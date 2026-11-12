import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { ImageResponse } from "next/og";

export const alt = "SplitIt: dividí los gastos del grupo sin vueltas";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const PRIMARY = "#28c49e";
// Medidas de components/splitit-logo.tsx (en px) multiplicadas por este factor.
const SCALE = 6;

// Vista previa al compartir un link (WhatsApp, etc.). WhatsApp recorta la tarjeta chica a un
// cuadrado centrado, así que todo va dentro del cuadrado central de 630x630.
export default async function OpengraphImage() {
  const geistExtraBold = await readFile(join(process.cwd(), "assets/Geist-ExtraBold.ttf"));

  return new ImageResponse(
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#fcfcfe",
      }}
    >
      <div
        style={{
          width: size.height,
          height: size.height,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <div
          style={{ display: "flex", position: "relative", width: 74 * SCALE, height: 34 * SCALE }}
        >
          <div
            style={{
              position: "absolute",
              top: 0,
              left: 0,
              width: 57 * SCALE,
              height: 34 * SCALE,
              borderRadius: 8 * SCALE,
              background: PRIMARY,
            }}
          />
          <div
            style={{
              position: "absolute",
              top: 3 * SCALE,
              left: 1 * SCALE,
              width: 74 * SCALE,
              display: "flex",
              justifyContent: "center",
              fontFamily: "Geist",
              fontSize: 24 * SCALE,
              lineHeight: 1.15,
              fontWeight: 800,
              whiteSpace: "nowrap",
            }}
          >
            <span style={{ color: "#ffffff" }}>Split</span>
            <span style={{ color: "#000000" }}>It</span>
          </div>
        </div>
        <div
          style={{
            display: "flex",
            marginTop: 48,
            maxWidth: 520,
            textAlign: "center",
            fontSize: 40,
            color: "#3f4a55",
          }}
        >
          Dividí los gastos del grupo, sin vueltas
        </div>
      </div>
    </div>,
    { ...size, fonts: [{ name: "Geist", data: geistExtraBold, weight: 800, style: "normal" }] }
  );
}
