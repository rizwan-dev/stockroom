import type { Order, Problem, StockRow } from './types';

/**
 * Where the API lives depends on who is asking.
 *
 * In the browser it is the port published on the developer's machine. During
 * server-side rendering inside the compose network it is another container, and
 * `localhost` there means the web container itself. One variable cannot be
 * both: using the browser's URL on the server fails at render time with a bare
 * "fetch failed", which is exactly what `docker compose up` used to produce.
 */
const BASE =
  typeof window === 'undefined'
    ? process.env.API_INTERNAL_URL ??
      process.env.NEXT_PUBLIC_API_URL ??
      'http://localhost:9192'
    : process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:9192';

/**
 * Every call returns one of three outcomes, never a thrown string.
 *
 * The server is careful to distinguish "this cannot be done" from "this could
 * not be done right now", and that distinction is worthless if the client
 * flattens both into `catch (e)`. Modelling it in the type forces each caller
 * to decide what to show for each case.
 */
export type Result<T> =
  | { kind: 'ok'; value: T }
  | { kind: 'problem'; problem: Problem; retryable: boolean }
  | { kind: 'unreachable'; message: string };

async function request<T>(path: string, init?: RequestInit): Promise<Result<T>> {
  let response: Response;

  try {
    response = await fetch(`${BASE}${path}`, {
      ...init,
      cache: 'no-store',
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    });
  } catch (cause) {
    // The API did not answer at all. This is deliberately its own outcome:
    // "the server is down" must never be rendered as "out of stock".
    return {
      kind: 'unreachable',
      message: cause instanceof Error ? cause.message : 'The API could not be reached',
    };
  }

  if (response.ok) {
    return { kind: 'ok', value: (await response.json()) as T };
  }

  let problem: Problem;
  try {
    problem = (await response.json()) as Problem;
  } catch {
    problem = { status: response.status, title: response.statusText };
  }

  return {
    kind: 'problem',
    problem,
    // 503 from this API means "contended, try again"; 409 means "no".
    retryable: problem.retryable === true || response.status === 503,
  };
}

export function getStock(): Promise<Result<StockRow[]>> {
  return request<StockRow[]>('/api/stock');
}

export function placeOrder(
  lines: { sku: string; quantity: number }[],
): Promise<Result<Order>> {
  return request<Order>('/api/orders', {
    method: 'POST',
    body: JSON.stringify({ lines }),
  });
}

/** Minor units to a readable amount. Money never goes near a float. */
export function formatMinor(minor: number, currency = 'INR'): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(minor / 100);
}
