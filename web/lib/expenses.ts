import { ApiError, request } from "@/lib/api";
import { EventError } from "@/lib/events";

export const EXPENSE_CURRENCIES = ["ARS", "USD", "EUR", "BRL", "UYU", "CLP"] as const;

export type ExpenseCurrency = (typeof EXPENSE_CURRENCIES)[number];

export type ExpensePayer = {
  id: number;
  name: string;
};

export type Expense = {
  id: number;
  eventId: number;
  name: string;
  originalAmount: number;
  originalCurrency: string;
  baseAmount: number;
  baseCurrency: string;
  paidByMember: ExpensePayer;
  expenseDate: string;
};

export type ExpenseQuote = {
  originalAmount: number;
  originalCurrency: string;
  baseCurrency: string;
  exchangeRate: number;
  baseAmount: number;
};

export type CreateExpenseInput = {
  name: string;
  amount: number;
  currency: string;
  paidByMemberId: number;
};

const LIST_UNAVAILABLE = "No pudimos cargar los gastos. Probá de nuevo.";
const QUOTE_UNAVAILABLE = "No pudimos obtener el tipo de cambio.";
const SAVE_UNAVAILABLE = "No pudimos guardar el gasto. Probá de nuevo.";

function toExpenseError(err: unknown, unavailableMessage: string): never {
  if (err instanceof ApiError) {
    if (err.status === 0) {
      throw new EventError("network", "No pudimos conectar con el servidor.");
    }
    if (err.status === 401) {
      throw new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.");
    }
    if (err.status === 403) {
      throw new EventError("forbidden", "No tenés acceso a este evento.");
    }
    if (err.status === 404) {
      throw new EventError("not-found", "No encontramos este evento.");
    }
    if (err.status === 400 || err.status === 422) {
      throw new EventError("validation", err.message || "Revisá los datos ingresados.");
    }
    if (err.status === 409) {
      throw new EventError(
        "conflict",
        err.message || "No se puede completar la operación porque tiene registros relacionados."
      );
    }
    throw new EventError("server-error", unavailableMessage);
  }
  throw err as Error;
}

export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(amount);
}

export function parseExpenseAmount(raw: string): number | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const normalized =
    trimmed.includes(",") && !trimmed.includes(".") ? trimmed.replace(",", ".") : trimmed;
  if (!/^\d+(\.\d+)?$/.test(normalized)) return null;

  const fraction = normalized.split(".")[1] ?? "";
  if (fraction.length > 4) return null;

  const value = Number(normalized);
  if (!Number.isFinite(value) || value <= 0) return null;
  return value;
}

export function formatExpenseDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("T")[0].split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day)).toLocaleDateString("es-AR", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
}

export function expenseTotal(expenses: Expense[]): number {
  return expenses.reduce((sum, expense) => sum + expense.baseAmount, 0);
}

export async function listExpenses(eventId: number | string): Promise<Expense[]> {
  try {
    return await request<Expense[]>(`/api/events/${eventId}/expenses`);
  } catch (err) {
    toExpenseError(err, LIST_UNAVAILABLE);
  }
}

export async function quoteExpense(
  eventId: number | string,
  amount: number,
  currency: string,
  init?: RequestInit
): Promise<ExpenseQuote> {
  const params = new URLSearchParams({ amount: String(amount), currency });
  try {
    return await request<ExpenseQuote>(`/api/events/${eventId}/expenses/quote?${params}`, init);
  } catch (err) {
    toExpenseError(err, QUOTE_UNAVAILABLE);
  }
}

export async function createExpense(
  eventId: number | string,
  input: CreateExpenseInput
): Promise<Expense> {
  try {
    return await request<Expense>(`/api/events/${eventId}/expenses`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  } catch (err) {
    toExpenseError(err, SAVE_UNAVAILABLE);
  }
}
