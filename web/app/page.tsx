import OrderPanel from './order-panel';
import { formatMinor, getStock } from '@/lib/api';

// Always fetched fresh: stock is the one thing on this page that must never be
// served from a cache.
export const dynamic = 'force-dynamic';

export default async function Page() {
  const result = await getStock();

  return (
    <>
      <h1>Stockroom</h1>
      <p className="lede">
        Inventory and orders. Stock cannot go negative, and concurrent orders for
        the last unit resolve to exactly one sale - enforced by a database
        constraint and an optimistic lock, not by hoping.
      </p>

      {result.kind === 'unreachable' && (
        <div className="note no">
          The API is not reachable at the configured address.{' '}
          <code>{result.message}</code>
          <br />
          Start it with <code>docker compose up</code>, or run the API on port 8080.
        </div>
      )}

      {result.kind === 'problem' && (
        <div className="note no">
          {result.problem.title ?? 'The API returned an error'}
          {result.problem.detail ? ` - ${result.problem.detail}` : ''}
        </div>
      )}

      {result.kind === 'ok' && (
        <>
          <h2>Stock on hand</h2>
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Product</th>
                <th className="num">Unit price</th>
                <th className="num">On hand</th>
              </tr>
            </thead>
            <tbody>
              {result.value.map((row) => (
                <tr key={row.sku}>
                  <td>{row.sku}</td>
                  <td>{row.name}</td>
                  <td className="num">{formatMinor(row.unitPriceMinor)}</td>
                  <td
                    className={
                      'num ' + (row.quantityOnHand === 0 ? 'out' : row.quantityOnHand < 5 ? 'low' : '')
                    }
                  >
                    {row.quantityOnHand}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <h2>Place an order</h2>
          <OrderPanel stock={result.value} />
        </>
      )}
    </>
  );
}
