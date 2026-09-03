export class ApiError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function apiUrl(path: string) {
  const base = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/$/, "");
  return `${base}${path}`;
}

async function readErrorMessage(response: Response) {
  const text = await response.text();
  if (!text) return response.statusText || `Error ${response.status}`;

  try {
    const body = JSON.parse(text) as { message?: string };
    if (body.message && !body.message.includes("\n")) {
      return body.message;
    }
  } catch {
    if (text.length < 180) return text;
  }

  return `Error ${response.status}`;
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;

  try {
    response = await fetch(apiUrl(path), {
      ...init,
      credentials: init?.credentials ?? "include",
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...init?.headers,
      },
    });
  } catch {
    throw new ApiError(0, "No pudimos conectar con la API.");
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
