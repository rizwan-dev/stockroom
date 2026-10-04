/**
 * The API contract, by hand.
 *
 * These mirror the Java records on the server. In a larger project this file
 * would be generated from the OpenAPI document the API already publishes at
 * /v3/api-docs, so drift is impossible. At this size, hand-written and
 * reviewed is honest and readable - but the generated route is the one to take
 * the moment a second consumer appears.
 */

export type StockRow = {
  sku: string;
  name: string;
  quantityOnHand: number;
  unitPriceMinor: number;
};

export type OrderLine = {
  sku: string;
  name: string;
  quantity: number;
  unitPriceMinor: number;
  lineTotalMinor: number;
};

export type Order = {
  reference: string;
  status: 'PLACED' | 'CANCELLED';
  totalMinor: number;
  createdAt: string;
  lines: OrderLine[];
};

/** RFC 7807 problem document, plus the extensions this API adds. */
export type Problem = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  sku?: string;
  requested?: number;
  available?: number;
  retryable?: boolean;
  fields?: Record<string, string>;
};
