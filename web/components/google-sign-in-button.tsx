"use client";

import { useEffect, useEffectEvent, useRef, useState } from "react";

import { LoadingIndicator } from "@/components/loading-indicator";
import { Separator } from "@/components/ui/separator";
import { loginWithGoogle } from "@/lib/auth";

const GOOGLE_SCRIPT_ID = "google-identity-services";
const GOOGLE_SCRIPT_SRC = "https://accounts.google.com/gsi/client";
// Google only renders its button between 200 and 400 px wide.
const MIN_BUTTON_WIDTH = 200;
const MAX_BUTTON_WIDTH = 400;

type GoogleCredentialResponse = { credential?: string };

type GoogleIdentity = {
  initialize: (config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
    ux_mode: "popup";
  }) => void;
  renderButton: (parent: HTMLElement, options: Record<string, string | number>) => void;
};

declare global {
  interface Window {
    google?: { accounts?: { id?: GoogleIdentity } };
  }
}

function loadGoogleIdentity(): Promise<GoogleIdentity> {
  const loaded = window.google?.accounts?.id;
  if (loaded) return Promise.resolve(loaded);

  return new Promise((resolve, reject) => {
    let script = document.getElementById(GOOGLE_SCRIPT_ID) as HTMLScriptElement | null;
    if (!script) {
      script = document.createElement("script");
      script.id = GOOGLE_SCRIPT_ID;
      script.src = GOOGLE_SCRIPT_SRC;
      script.async = true;
      document.body.appendChild(script);
    }
    script.addEventListener("load", () => {
      const identity = window.google?.accounts?.id;
      if (identity) resolve(identity);
      else reject(new Error("Google Identity Services is unavailable."));
    });
    script.addEventListener("error", () => reject(new Error("Google script failed to load.")));
  });
}

type GoogleSignInButtonProps = {
  onSuccess: () => void;
  onError: () => void;
};

// Without a client ID (QA and PR previews) the option is not offered.
export function GoogleSignInButton(props: GoogleSignInButtonProps) {
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  if (!clientId) return null;
  return <GoogleButton clientId={clientId} {...props} />;
}

function GoogleButton({
  clientId,
  onSuccess,
  onError,
}: GoogleSignInButtonProps & { clientId: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);

  const handleCredential = useEffectEvent(async ({ credential }: GoogleCredentialResponse) => {
    if (!credential) {
      onError();
      return;
    }
    setIsSigningIn(true);
    try {
      await loginWithGoogle(credential);
      onSuccess();
    } catch {
      setIsSigningIn(false);
      onError();
    }
  });

  const handleLoadError = useEffectEvent(() => onError());

  useEffect(() => {
    let cancelled = false;
    loadGoogleIdentity()
      .then((identity) => {
        const container = containerRef.current;
        if (cancelled || !container) return;
        identity.initialize({
          client_id: clientId,
          callback: (response) => void handleCredential(response),
          ux_mode: "popup",
        });
        identity.renderButton(container, {
          type: "standard",
          theme: "outline",
          size: "large",
          shape: "rectangular",
          text: "continue_with",
          locale: "es",
          width: Math.max(MIN_BUTTON_WIDTH, Math.min(MAX_BUTTON_WIDTH, container.offsetWidth)),
        });
      })
      .catch(() => {
        if (!cancelled) handleLoadError();
      });
    return () => {
      cancelled = true;
    };
  }, [clientId]);

  return (
    <div className="mt-5 flex flex-col gap-5">
      <div className="flex items-center gap-3" aria-hidden="true">
        <Separator className="flex-1" />
        <span className="text-text-secondary text-sm font-medium">o</span>
        <Separator className="flex-1" />
      </div>
      <div ref={containerRef} className="flex min-h-11 justify-center" hidden={isSigningIn} />
      {isSigningIn && (
        <p className="text-text-secondary flex justify-center text-sm">
          <LoadingIndicator message="Ingresando con Google…" />
        </p>
      )}
    </div>
  );
}
