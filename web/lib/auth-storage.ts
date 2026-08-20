export type AuthUser = {
  id: number;
  username: string;
  roles: string[];
};

export const AUTH_TOKEN_COOKIE = "auth_token";
export const AUTH_CHANGED_EVENT = "auth-changed";
const USER_STORAGE_KEY = "auth_user";
const TOKEN_MAX_AGE_SECONDS = 60 * 60 * 8;

let snapshot: AuthUser | null = null;
let snapshotKey = "";

export function decodeUserFromToken(token: string): AuthUser | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    const json = JSON.parse(atob(padded)) as {
      sub?: string;
      username?: string;
      roles?: string[];
    };
    if (!json.sub || !json.username || !Array.isArray(json.roles)) return null;
    return { id: Number(json.sub), username: json.username, roles: json.roles };
  } catch {
    return null;
  }
}

function notifyAuthChanged() {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}

export function getAuthToken(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${AUTH_TOKEN_COOKIE}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  const token = getAuthToken() ?? "";
  const raw = window.localStorage.getItem(USER_STORAGE_KEY) ?? "";
  const key = `${token}|${raw}`;
  if (key === snapshotKey) return snapshot;

  let user: AuthUser | null = null;
  if (raw) {
    try {
      user = JSON.parse(raw) as AuthUser;
    } catch {
      user = null;
    }
  }
  if (!user && token) user = decodeUserFromToken(token);

  snapshotKey = key;
  snapshot = user;
  return snapshot;
}

export function setSession(token: string, user: AuthUser) {
  if (typeof document === "undefined") return;
  document.cookie = `${AUTH_TOKEN_COOKIE}=${encodeURIComponent(token)}; Path=/; Max-Age=${TOKEN_MAX_AGE_SECONDS}; SameSite=Lax`;
  window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  snapshotKey = "";
  notifyAuthChanged();
}

export function clearSession() {
  if (typeof document === "undefined") return;
  document.cookie = `${AUTH_TOKEN_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
  window.localStorage.removeItem(USER_STORAGE_KEY);
  snapshotKey = "";
  snapshot = null;
  notifyAuthChanged();
}

export function subscribeToAuth(onStoreChange: () => void) {
  window.addEventListener(AUTH_CHANGED_EVENT, onStoreChange);
  window.addEventListener("storage", onStoreChange);
  return () => {
    window.removeEventListener(AUTH_CHANGED_EVENT, onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}
