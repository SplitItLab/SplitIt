export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public field?: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function apiUrl(path: string) {
  const base = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/$/, "");
  return `${base}${path}`;
}

async function readError(response: Response): Promise<{ message: string; field?: string }> {
  const text = await response.text();
  if (!text) return { message: response.statusText || `Error ${response.status}` };

  try {
    const body = JSON.parse(text) as {
      message?: string;
      field?: string;
      errors?: { field?: string; message?: string }[];
    };
    const fromErrors = body.errors?.find((e) => e.field);
    const message = fromErrors?.message ?? body.message;
    const field = body.field ?? fromErrors?.field;
    if (message && !message.includes("\n")) {
      return { message, field };
    }
  } catch {
    if (text.length < 180) return { message: text };
  }

  return { message: `Error ${response.status}` };
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
    const { message, field } = await readError(response);
    throw new ApiError(response.status, message, field);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
