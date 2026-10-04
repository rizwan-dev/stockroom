# Stockroom

A small inventory and orders system, built end to end: **Spring Boot** API,
**Next.js** front end, **PostgreSQL**, one command to run the lot.

It exists to answer a question that is easy to ask in an interview and hard to
show in a repository: *what happens when two people order the last unit at the
same moment?*

```bash
docker compose up --build
```

Then open <http://localhost:3000>. The API is on <http://localhost:9192>, with
OpenAPI docs at <http://localhost:9192/docs>.

The API and Postgres bind to 9192 and 55432 on your machine rather than the
conventional 8080 and 5432, which are the two ports most likely to be in use
already. Inside the compose network they still speak 8080 and 5432.

---

## The problem it solves

Stock is the classic contended resource. The naive implementation reads the
quantity, checks it is enough, and writes the new value — and under concurrency
it oversells, because two requests can both read `1` before either writes `0`.

Stockroom refuses to oversell, and it does so in three independent places
because any one of them can be bypassed by a future change:

**1. The database will not hold an invalid value.**

```sql
CONSTRAINT stock_not_negative CHECK (quantity_on_hand >= 0)
```

Application code can be wrong. A new caller can forget the check. A race can
slip past a conditional. This constraint cannot be bypassed by any of those.

**2. Concurrent writers collide instead of overwriting.**

The `stock_items` row carries an `@Version` column. Two transactions that read
the same row both issue `UPDATE ... WHERE version = ?`; the second matches zero
rows and Hibernate raises an optimistic lock failure. One order wins, the other
retries. Neither silently clobbers the other's write.

**3. The invariant lives with the data.**

`StockItem.remove(int)` is a domain method, not a setter. There is no way to
decrement stock without going through the check, because no setter exists.

## The test that matters

[`ConcurrentOrderTest`](api/src/test/java/dev/rizwan/stockroom/service/ConcurrentOrderTest.java)
seeds five units, releases twenty threads at the same instant with a
`CountDownLatch`, and asserts the outcome exactly:

```java
assertThat(placed.get()).isEqualTo(STOCK);   // exactly 5 sold, no more, no less
assertThat(remaining).isZero();              // lands on zero, never below
assertThat(unexpected.get()).isZero();       // no thread failed for a surprise reason
```

It runs against a real PostgreSQL 16 in Docker through Testcontainers, not an
in-memory database. H2 will happily accept SQL that Postgres rejects, and
constraint bugs that reach production usually got there past an H2 test suite.

## Three outcomes, not two

Most of the care in this codebase is about one distinction. These are three
different things and the user needs three different sentences:

| Outcome | HTTP | What the user should do |
| --- | --- | --- |
| Order placed | `201` | Nothing |
| Not enough stock | `409` + `requested`/`available` | Order fewer, or wait for restock |
| Lost the lock race after retries | `503`, `retryable: true` | Try again, nothing was reserved |
| API unreachable | — | Nothing was ordered; the server is down |

Errors are [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem documents
with a stable `type` URI, so clients branch on the type rather than on
human-readable text that will be reworded later.

The front end mirrors this rather than flattening it. `lib/api.ts` returns a
discriminated union:

```ts
export type Result<T> =
  | { kind: 'ok'; value: T }
  | { kind: 'problem'; problem: Problem; retryable: boolean }
  | { kind: 'unreachable'; message: string };
```

A `catch (e)` that treats "the server is down" and "you cannot have that many"
the same way is how users get told the wrong thing. The type makes collapsing
them a compile error rather than an oversight.

## A Spring trap worth knowing

The retry loop lives in `OrderService`; the single attempt lives in a separate
bean, [`OrderPlacer`](api/src/main/java/dev/rizwan/stockroom/service/OrderPlacer.java).
That separation is not stylistic.

`@Transactional` is implemented with a proxy. When a method calls another method
**on the same instance**, the call never passes through that proxy and the
annotation is silently ignored. A retry loop calling a same-class
`@Transactional` method would run every attempt inside the caller's original
transaction — which, after an optimistic lock failure, is already marked
rollback-only. Every retry fails. The code looks right and does nothing.

Putting the attempt in its own bean makes the call a real proxied call, so
`REQUIRES_NEW` genuinely starts a fresh transaction each time.

## Decisions worth defending

- **Money is `BIGINT` in minor units.** Never a float, never a double. One
  rounding edge case in a currency conversion is enough.
- **Prices are copied onto order lines, not referenced.** When a product's price
  changes, a historical order must still show what the customer actually paid.
- **Stock is a separate table from products.** Products are reference data that
  rarely changes; stock is contended and written constantly. Splitting them
  means an order does not take a write lock on product metadata.
- **`ddl-auto: validate`, Flyway owns the schema.** Entity and migration drift
  fails at startup rather than in production.
- **`UNIQUE (order_id, product_id)` on order lines.** Two lines for the same
  product in one order is a client bug that would otherwise double-decrement.
- **Retries are capped at three.** Enough to ride out normal contention; past
  that, failing fast with an honest "try again" beats an unbounded retry storm
  on a hot row.

## Layout

```
api/     Spring Boot 3.5, Java 17, JPA, Flyway, Testcontainers
web/     Next.js 16, React 19, TypeScript (strict)
.github/ CI: API tests, web typecheck and build
```

## Running without Docker

```bash
# Postgres on :55432 with database/user/password all 'stockroom'
cd api && ./mvnw spring-boot:run

cd web && npm install && npm run dev
```

## Tests

```bash
cd api && ./mvnw test     # needs a running Docker daemon for Testcontainers
cd web && npm run typecheck && npm run build
```

12 tests: pure unit tests on the domain invariants, `@WebMvcTest` for the HTTP
contract, a full-context test proving every entity matches the migrations, and
the concurrency test above.

---

Built by [Rizwanul Haque](https://github.com/rizwan-dev). More reference
implementations at [RizTech Academy](https://riztechacademy.com).
