import { describe, expect, it } from "vitest";
import type { ChatMessage, ReadReceipt } from "@/lib/types";
import {
  canSendDraft, codePointLength, compareMessageIds, highestVisibleMessageId,
  isRead, mergeMessages, mergeReceipts, newestMessageId, oldestMessageId,
} from "@/lib/chat-state";

function message(id: string, nonce = `nonce-${id}`, sender = "husband"): ChatMessage {
  return { id, client_nonce: nonce, sender_id: sender, body: "Hello 👋 안녕", created_at: "2026-10-18T01:00:00.000Z" };
}

function receipt(id: string | null, user = "wife"): ReadReceipt {
  return { user_id: user, last_read_message_id: id, updated_at: "2026-10-18T01:00:00.000Z" };
}

describe("message reconciliation", () => {
  it("replaces the optimistic message using the sender and retry nonce", () => {
    const pending = { ...message("pending:1", "nonce-1"), status: "pending" as const };
    const result = mergeMessages([pending], [message("51", "nonce-1")]);
    expect(result).toHaveLength(1);
    expect(result[0]).toMatchObject({ id: "51", status: "sent" });
  });

  it("deduplicates an acknowledgement followed by Realtime reconciliation", () => {
    const stored = message("52", "one-nonce");
    expect(mergeMessages([stored], [stored, stored])).toHaveLength(1);
  });

  it("keeps a stored message when a stale failed response arrives after Realtime", () => {
    const result = mergeMessages([message("53", "nonce")], [{ ...message("pending:nonce", "nonce"), status: "failed", error: "Network error" }]);
    expect(result[0]).toMatchObject({ id: "53", status: "sent" });
    expect(result[0].error).toBeUndefined();
  });

  it("retries a failed draft with the original nonce", () => {
    const failed = { ...message("pending:1", "same-nonce"), status: "failed" as const };
    const retried = mergeMessages([failed], [{ ...failed, status: "pending" }]);
    expect(retried).toHaveLength(1);
    expect(retried[0].client_nonce).toBe("same-nonce");
    expect(retried[0].status).toBe("pending");
  });

  it("allows the same nonce from different senders without conflating their messages", () => {
    expect(mergeMessages([message("1", "nonce", "husband")], [message("2", "nonce", "wife")])).toHaveLength(2);
  });

  it("retains older pages while reconnect fetches newer pages", () => {
    const loaded = [message("1"), message("2"), message("101"), message("102")];
    expect(mergeMessages(loaded, [message("102"), message("103")]).map((item) => item.id)).toEqual(["1", "2", "101", "102", "103"]);
  });

  it("sorts unbounded bigint IDs exactly and keeps pending messages last", () => {
    const result = mergeMessages([message("9007199254740993"), { ...message("pending:1"), status: "pending" }], [message("9007199254740992"), message("10")]);
    expect(result.map((item) => item.id)).toEqual(["10", "9007199254740992", "9007199254740993", "pending:1"]);
    expect(newestMessageId(result)).toBe("9007199254740993");
    expect(oldestMessageId(result)).toBe("10");
  });

  it("returns no pagination cursor for unsaved messages", () => {
    const messages = [{ ...message("pending:1"), status: "pending" as const }];
    expect(newestMessageId(messages)).toBeNull();
    expect(oldestMessageId(messages)).toBeNull();
  });
});

describe("read positions", () => {
  it("compares IDs beyond Number.MAX_SAFE_INTEGER exactly", () => {
    expect(compareMessageIds("9007199254740993", "9007199254740992")).toBe(1);
    expect(isRead("9007199254740993", "9007199254740992")).toBe(false);
    expect(isRead("9007199254740993", "9007199254740993")).toBe(true);
  });

  it("never moves a receipt backward, including a delayed null receipt", () => {
    expect(mergeReceipts([receipt("80")], [receipt("40"), receipt(null)])[0].last_read_message_id).toBe("80");
  });

  it("updates each member independently", () => {
    const result = mergeReceipts([receipt("80"), receipt("40", "husband")], [receipt("90", "husband")]);
    expect(result.find((item) => item.user_id === "wife")?.last_read_message_id).toBe("80");
    expect(result.find((item) => item.user_id === "husband")?.last_read_message_id).toBe("90");
  });

  it("uses only actually visible IDs, never the latest unseen message", () => {
    const loaded = [message("10"), message("11"), message("999")];
    expect(newestMessageId(loaded)).toBe("999");
    expect(highestVisibleMessageId(["10", "11", "pending:1"])).toBe("11");
    expect(highestVisibleMessageId([])).toBeNull();
  });

  it("does not mark optimistic messages read", () => {
    expect(isRead("pending:1", "999")).toBe(false);
    expect(isRead("40", null)).toBe(false);
  });
});

describe("Unicode and offline drafts", () => {
  it("counts Unicode code points, including Korean and emoji", () => {
    expect(codePointLength("안녕👋")).toBe(3);
    expect(canSendDraft("👋".repeat(2000), true)).toBe(true);
    expect(canSendDraft("👋".repeat(2001), true)).toBe(false);
  });

  it("does not send blank messages", () => {
    expect(canSendDraft("  \n\t  ", true)).toBe(false);
    expect(canSendDraft("  Hello  ", true)).toBe(true);
  });

  it("disables offline sending without changing the draft", () => {
    const draft = "Keep this for later 👋";
    expect(canSendDraft(draft, false)).toBe(false);
    expect(draft).toBe("Keep this for later 👋");
    expect(canSendDraft(draft, true)).toBe(true);
  });
});
