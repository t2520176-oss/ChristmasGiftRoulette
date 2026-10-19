import { describe, expect, it, vi } from "vitest";
import { ZodError } from "zod";
import type { ChatMessage } from "@/lib/types";
import type { MemberContext } from "@/lib/server/auth";
import { ApiError } from "@/lib/server/http";
import { listMessages, MESSAGE_COLUMNS, MESSAGE_PAGE_BYTE_LIMIT } from "@/lib/server/messages";

const sender = "20000000-0000-4000-8000-000000000001";
const nonce = "30000000-0000-4000-8000-000000000001";
const message = (id: string, body = `Message ${id}`): ChatMessage => ({
  id, body, sender_id: sender, client_nonce: nonce, created_at: "2026-10-18T00:00:00+00:00",
});

function database(rows: ChatMessage[] | null, error: { code: string } | null = null) {
  let before: string | undefined;
  let after: string | undefined;
  let limit = Infinity;
  const query = {
    select: vi.fn(), limit: vi.fn(), lt: vi.fn(), gt: vi.fn(), order: vi.fn(),
  };
  query.select.mockReturnValue(query);
  query.limit.mockImplementation((value: number) => { limit = value; return query; });
  query.lt.mockImplementation((_column: string, cursor: string) => { before = cursor; return query; });
  query.gt.mockImplementation((_column: string, cursor: string) => { after = cursor; return query; });
  query.order.mockImplementation(async (_column: string, { ascending }: { ascending: boolean }) => ({
    data: rows?.filter((row) => (!before || BigInt(row.id) < BigInt(before)) && (!after || BigInt(row.id) > BigInt(after)))
      .sort((a, b) => (BigInt(a.id) < BigInt(b.id) ? -1 : BigInt(a.id) > BigInt(b.id) ? 1 : 0) * (ascending ? 1 : -1))
      .slice(0, limit) ?? null,
    error,
  }));
  const from = vi.fn().mockReturnValue(query);
  return { context: { client: { from } } as unknown as MemberContext, from, query };
}

const pageBytes = (page: Awaited<ReturnType<typeof listMessages>>) => Buffer.byteLength(JSON.stringify(page), "utf8");

describe("message response pages", () => {
  it("keeps small initial and cursor pages at 50 messages in ascending display order", async () => {
    const rows = Array.from({ length: 51 }, (_, index) => message(String(index + 1)));
    const { context, query, from } = database(rows);
    const initial = await listMessages(context);
    expect(initial.messages.map((row) => row.id)).toEqual(rows.slice(1).map((row) => row.id));
    expect(initial.hasMore).toBe(true);
    expect(from).toHaveBeenCalledWith("messages");
    expect(query.select).toHaveBeenCalledWith(MESSAGE_COLUMNS);
    expect(query.limit).toHaveBeenCalledWith(51);
    expect(query.order).toHaveBeenLastCalledWith("id", { ascending: false });
    expect(await listMessages(context, "2")).toEqual({ messages: [rows[0]], hasMore: false });
    const forward = await listMessages(database(rows).context, undefined, "1");
    expect(forward).toEqual({ messages: rows.slice(1), hasMore: false });
    const laterRows = rows.map((row) => ({ ...row, id: String(BigInt(row.id) + 1n) }));
    expect(await listMessages(database(laterRows).context, undefined, "1"))
      .toEqual({ messages: laterRows.slice(0, 50), hasMore: true });
  });

  it("uses serialized UTF-8 bytes and preserves every initial/before message across short pages", async () => {
    const body = "안녕 👋\n\"\\".repeat(100_000);
    const rows = Array.from({ length: 5 }, (_, index) => message(String(90071992547409930n + BigInt(index)), body));
    const initial = await listMessages(database(rows).context);
    expect(initial.messages.map((row) => row.id)).toEqual(rows.slice(3).map((row) => row.id));
    expect(initial.hasMore).toBe(true);
    expect(pageBytes(initial)).toBeLessThanOrEqual(MESSAGE_PAGE_BYTE_LIMIT);
    // Three bodies fit by JS character count, but exceed the serialized UTF-8 transfer budget.
    expect(JSON.stringify({ messages: rows.slice(2), hasMore: true }).length).toBeLessThan(MESSAGE_PAGE_BYTE_LIMIT);
    expect(Buffer.byteLength(JSON.stringify({ messages: rows.slice(2), hasMore: true }), "utf8")).toBeGreaterThan(MESSAGE_PAGE_BYTE_LIMIT);

    const middleDb = database(rows);
    const middle = await listMessages(middleDb.context, initial.messages[0].id);
    expect(middleDb.query.lt).toHaveBeenCalledWith("id", "90071992547409933");
    expect(middle.messages.map((row) => row.id)).toEqual(rows.slice(1, 3).map((row) => row.id));
    expect(middle.hasMore).toBe(true);
    expect(pageBytes(middle)).toBeLessThanOrEqual(MESSAGE_PAGE_BYTE_LIMIT);
    const last = await listMessages(database(rows).context, middle.messages[0].id);
    expect(last.hasMore).toBe(false);
    expect([...last.messages, ...middle.messages, ...initial.messages]).toEqual(rows);
  });

  it("continues after byte-truncated forward pages with exact bigint cursors and no lost messages", async () => {
    const rows = Array.from({ length: 5 }, (_, index) => message(String(90071992547409930n + BigInt(index)), "👋\n\"\\".repeat(170_000)));
    const received: ChatMessage[] = [];
    let cursor = "90071992547409929";
    let hasMore = true;
    const lengths: number[] = [];
    while (hasMore) {
      const { context, query } = database(rows);
      const page = await listMessages(context, undefined, cursor);
      expect(query.gt).toHaveBeenCalledWith("id", cursor);
      expect(query.order).toHaveBeenCalledWith("id", { ascending: true });
      expect(page.messages.length).toBeGreaterThan(0);
      expect(pageBytes(page)).toBeLessThanOrEqual(MESSAGE_PAGE_BYTE_LIMIT);
      received.push(...page.messages);
      lengths.push(page.messages.length);
      cursor = page.messages.at(-1)!.id;
      hasMore = page.hasMore;
      if (lengths.length > rows.length) throw new Error("Pagination did not make progress");
    }
    expect(lengths).toEqual([2, 2, 1]);
    expect(received).toEqual(rows);
  });

  it("allows a legitimate 4 MiB JSON request's message plus response metadata", async () => {
    const emptyRequest = JSON.stringify({ body: "", client_nonce: nonce });
    const body = "a".repeat(4 * 1024 * 1024 - Buffer.byteLength(emptyRequest));
    expect(Buffer.byteLength(JSON.stringify({ body, client_nonce: nonce }))).toBe(4 * 1024 * 1024);
    const stored = message("1", body);
    const page = await listMessages(database([stored]).context);
    expect(page).toEqual({ messages: [stored], hasMore: false });
    expect(pageBytes(page)).toBeLessThanOrEqual(MESSAGE_PAGE_BYTE_LIMIT);
  });

  it.each([false, true])("rejects an individually oversized row explicitly instead of returning an empty page (after=%s)", async (forward) => {
    const context = database([message("2", "a".repeat(MESSAGE_PAGE_BYTE_LIMIT))]).context;
    await expect(listMessages(context, forward ? undefined : "3", forward ? "1" : undefined))
      .rejects.toMatchObject({ name: "ApiError", status: 413, message: expect.stringContaining("too large to load") });
  });

  it("never skips an oversized row to return a later message", async () => {
    const rows = [message("2"), message("3", "a".repeat(MESSAGE_PAGE_BYTE_LIMIT)), message("4")];
    const page = await listMessages(database(rows).context, undefined, "1");
    expect(page).toEqual({ messages: [rows[0]], hasMore: true });
    await expect(listMessages(database(rows).context, undefined, page.messages[0].id)).rejects.toMatchObject({ status: 413 });
  });

  it("handles empty results and still validates cursors, records, and database failures", async () => {
    expect(await listMessages(database(null).context)).toEqual({ messages: [], hasMore: false });
    const { context, from } = database([]);
    await expect(listMessages(context, "1", "2")).rejects.toMatchObject({ status: 400 });
    await expect(listMessages(context, "9223372036854775808")).rejects.toBeInstanceOf(ZodError);
    expect(from).not.toHaveBeenCalled();
    await expect(listMessages(database([{ ...message("1"), body: 3 } as unknown as ChatMessage]).context)).rejects.toBeInstanceOf(ZodError);
    await expect(listMessages(database([], { code: "42501" }).context)).rejects.toMatchObject({ status: 403 });
    await expect(listMessages(database([], { code: "42P01" }).context)).rejects.toBeInstanceOf(ApiError);
  });
});
