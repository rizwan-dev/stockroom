'use client';

import { useRouter } from 'next/navigation';
import { useState, useTransition } from 'react';
import { formatMinor, placeOrder } from '@/lib/api';
import type { Order, Problem, StockRow } from '@/lib/types';

type Outcome =
  | { kind: 'idle' }
  | { kind: 'placed'; order: Order }
  | { kind: 'refused'; problem: Problem }
  | { kind: 'retryable'; problem: Problem }
  | { kind: 'unreachable'; message: string };

export default function OrderPanel({ stock }: { stock: StockRow[] }) {
  const [sku, setSku] = useState(stock[0]?.sku ?? '');
  const [quantity, setQuantity] = useState(1);
  const [outcome, setOutcome] = useState<Outcome>({ kind: 'idle' });
  const [pending, startTransition] = useTransition();
  const router = useRouter();

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const result = await placeOrder([{ sku, quantity }]);

    if (result.kind === 'ok') {
      setOutcome({ kind: 'placed', order: result.value });
      // Re-render the server component so the stock table reflects the sale.
      startTransition(() => router.refresh());
      return;
    }

    if (result.kind === 'unreachable') {
      setOutcome({ kind: 'unreachable', message: result.message });
      return;
    }

    // The whole point of the three-way result: a refusal and a retryable
    // failure get different words, because they need different user actions.
    setOutcome(
      result.retryable
        ? { kind: 'retryable', problem: result.problem }
        : { kind: 'refused', problem: result.problem },
    );
  }

  return (
    <form className="panel" onSubmit={submit}>
      <div className="row">
        <div>
          <label htmlFor="sku">Product</label>
          <select id="sku" value={sku} onChange={(e) => setSku(e.target.value)}>
            {stock.map((row) => (
              <option key={row.sku} value={row.sku}>
                {row.name} - {row.quantityOnHand} on hand
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="qty">Quantity</label>
          <input
            id="qty"
            type="number"
            min={1}
            value={quantity}
            onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
          />
        </div>
        <button type="submit" disabled={pending || !sku}>
          {pending ? 'Placing...' : 'Place order'}
        </button>
      </div>

      {outcome.kind === 'placed' && (
        <div className="note ok">
          Order placed for {formatMinor(outcome.order.totalMinor)}.{' '}
          <code>{outcome.order.reference}</code>
        </div>
      )}

      {outcome.kind === 'refused' && (
        <div className="note no">
          {outcome.problem.title ?? 'Refused'}
          {outcome.problem.available !== undefined && (
            <>
              {' '}- you asked for {outcome.problem.requested}, {outcome.problem.available}{' '}
              {outcome.problem.available === 1 ? 'is' : 'are'} on hand.
            </>
          )}
        </div>
      )}

      {outcome.kind === 'retryable' && (
        <div className="note retry">
          Nothing was reserved - the stock was being changed by someone else at the
          same moment. Try again.
        </div>
      )}

      {outcome.kind === 'unreachable' && (
        <div className="note no">
          The API did not answer, so nothing was ordered. <code>{outcome.message}</code>
        </div>
      )}
    </form>
  );
}
