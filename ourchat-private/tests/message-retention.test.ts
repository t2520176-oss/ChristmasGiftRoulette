import { describe, expect, it } from "vitest";
import { MESSAGE_RETENTION_MS, isMessageRetained, nextMessageExpiryDelay, retainMessages } from "@/lib/message-retention";

describe("message retention", () => {
  const now = Date.parse("2026-11-02T12:00:00Z");
  const message = (id: string, age: number, status?: string) => ({
    id, created_at: new Date(now - age).toISOString(), status,
    body: "  안녕 👨‍👩‍👧‍👦\n".repeat(1000),
  });

  it("uses exactly 168 hours, with an exclusive boundary and unchanged Unicode content", () => {
    expect(MESSAGE_RETENTION_MS).toBe(604_800_000);
    const oldest = message("1", MESSAGE_RETENTION_MS - 1);
    const expired = message("2", MESSAGE_RETENTION_MS);
    const overdue = message("3", MESSAGE_RETENTION_MS + 1);
    expect(retainMessages([oldest, expired, overdue], now)).toEqual([oldest]);
    expect(retainMessages([oldest], now)[0]).toBe(oldest);
    expect(isMessageRetained(oldest, now + 1)).toBe(false);
  });

  it("preserves local pending and failed items without extending stored messages by changing status", () => {
    const pending = message("pending:a", MESSAGE_RETENTION_MS * 2, "pending");
    const failed = { ...message("pending:b", MESSAGE_RETENTION_MS * 2, "failed"), created_at: "invalid" };
    const stored = message("42", MESSAGE_RETENTION_MS, "pending");
    expect(retainMessages([pending, failed, stored], now)).toEqual([pending, failed]);
    expect(nextMessageExpiryDelay([pending, failed], now)).toBeNull();
  });

  it("hides malformed stored timestamps rather than preserving them indefinitely", () => {
    const invalid = { id: "1", created_at: "invalid" };
    expect(isMessageRetained(invalid, now)).toBe(false);
    expect(nextMessageExpiryDelay([invalid], now)).toBe(0);
  });

  it("chooses the earliest stored expiry, handles empty sets, and clamps long delays safely", () => {
    expect(nextMessageExpiryDelay([], now)).toBeNull();
    expect(nextMessageExpiryDelay([message("1", MESSAGE_RETENTION_MS - 500), message("2", 0)], now)).toBe(500);
    expect(nextMessageExpiryDelay([message("1", MESSAGE_RETENTION_MS)], now)).toBe(0);
    expect(nextMessageExpiryDelay([{ id: "1", created_at: new Date(now + 2_147_483_647).toISOString() }], now)).toBe(2_147_483_647);
  });
});
