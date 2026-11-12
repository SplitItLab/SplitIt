import type { Expense, ExpensePage } from "@/lib/expenses";

// Página de gastos como la devuelve la API; el total por defecto suma los gastos dados.
export function expensePage(
  items: Expense[],
  {
    hasNext = false,
    totalCount,
    totalAmount,
  }: { hasNext?: boolean; totalCount?: number; totalAmount?: number } = {}
): ExpensePage {
  return {
    items,
    hasNext,
    totalCount: totalCount ?? items.length,
    totalAmount: totalAmount ?? items.reduce((sum, expense) => sum + expense.baseAmount, 0),
  };
}
