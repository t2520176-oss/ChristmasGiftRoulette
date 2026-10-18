import { describe, expect, it } from "vitest";
import { isAllowedPushEndpoint, messageBodySchema, messageIdSchema, messageInputSchema, subscriptionSchema } from "@/lib/validation";

describe("message validation", () => {
  it("trims Unicode whitespace, preserves Korean and emoji, and counts codepoints", () => {
    expect(messageBodySchema.parse(" \n 안녕 👋 \n ")).toBe("안녕 👋");
    expect(messageBodySchema.parse("😀".repeat(2000))).toHaveLength(4000);
    expect(messageBodySchema.safeParse("😀".repeat(2001)).success).toBe(false);
    expect(messageBodySchema.safeParse(" \n\t ").success).toBe(false);
  });
  it("requires an idempotency nonce and rejects forged sender IDs", () => {
    expect(messageInputSchema.safeParse({ body: "Hello" }).success).toBe(false);
    expect(messageInputSchema.safeParse({ body: "Hello", client_nonce: "10000000-0000-4000-8000-000000000001", sender_id: "someone else" }).success).toBe(false);
  });
  it("preserves large bigint IDs and rejects malformed and overflowing cursors safely", () => {
    expect(messageIdSchema.parse("9007199254740993")).toBe("9007199254740993");
    for (const bad of ["abc", "1.2", "1e3", "0", "-1", "9223372036854775808"]) {
      expect(messageIdSchema.safeParse(bad).success).toBe(false);
    }
  });
});
describe("push subscription validation", () => {
  it("restricts outbound push targets to HTTPS push providers, preventing arbitrary internal requests", () => {
    for (const endpoint of ["https://fcm.googleapis.com/fcm/send/token", "https://web.push.apple.com/token", "https://updates.push.services.mozilla.com/wpush/token", "https://abc.notify.windows.com/token"]) {
      expect(isAllowedPushEndpoint(endpoint)).toBe(true);
    }
    for (const endpoint of ["http://fcm.googleapis.com/x", "https://127.0.0.1/x", "https://localhost/x", "https://example.com/x", "https://fcm.googleapis.com.evil.test/x", "https://fcm.googleapis.com@evil.test/x", "https://fcm.googleapis.com:444/x", "https://fcm.googleapis.com/x#secret"]) {
      expect(isAllowedPushEndpoint(endpoint)).toBe(false);
    }
  });
  it("validates actual uncompressed P-256 key and authentication-secret sizes", () => {
    const p256dh = Buffer.concat([Buffer.from([4]), Buffer.alloc(64, 1)]).toString("base64url");
    const auth = Buffer.alloc(16, 2).toString("base64url");
    const input = { endpoint: "https://fcm.googleapis.com/push/token", expirationTime: null, keys: { p256dh, auth } };
    expect(subscriptionSchema.safeParse(input).success).toBe(true);
    expect(subscriptionSchema.safeParse({ ...input, keys: { ...input.keys, auth: "short" } }).success).toBe(false);
  });
});
