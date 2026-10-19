import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { after } from "next/server";
import { POST } from "@/app/api/messages/route";
import { requireMember, type MemberContext } from "@/lib/server/auth";
import { sendMessagePush } from "@/lib/server/push";
import { ApiError } from "@/lib/server/http";

vi.mock("@/lib/server/auth", () => ({ requireMember: vi.fn() }));
vi.mock("@/lib/server/push", () => ({ sendMessagePush: vi.fn() }));
vi.mock("next/server", async () => ({ ...await vi.importActual<typeof import("next/server")>("next/server"), after: vi.fn() }));
const userId = "20000000-0000-4000-8000-000000000001";
const nonce = "30000000-0000-4000-8000-000000000001";
const stored = { id: "9007199254740993", sender_id: userId, body: "Hello", client_nonce: nonce, created_at: "2026-10-18T00:00:00+00:00" };
function query(data: unknown, error: { code: string; message?: string } | null = null) {
  const result = { data, error };
  const chain = {
    select: vi.fn(), eq: vi.fn(), insert: vi.fn(), single: vi.fn().mockResolvedValue(result), maybeSingle: vi.fn().mockResolvedValue(result),
  };
  chain.select.mockReturnValue(chain); chain.eq.mockReturnValue(chain); chain.insert.mockReturnValue(chain);
  return chain;
}
let from = vi.fn();
function request(body: unknown = { body: " Hello ", client_nonce: nonce }, origin = "https://ourchat.example") {
  return new Request("https://ourchat.example/api/messages", { method: "POST", headers: { origin, "content-type": "application/json" }, body: JSON.stringify(body) });
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://ourchat.example");
  from = vi.fn();
  const context = { authorized: true, user: { id: userId }, member: { user_id: userId, display_name: "Husband" }, client: { from } } as unknown as MemberContext;
  vi.mocked(requireMember).mockResolvedValue(context);
  vi.mocked(sendMessagePush).mockResolvedValue(undefined);
});
afterEach(() => vi.unstubAllEnvs());
describe("message insertion", () => {
  it("rejects unauthenticated or non-member requests before any message/admin query", async () => {
    for (const status of [401, 403]) {
      vi.mocked(requireMember).mockRejectedValueOnce(new ApiError(status, "Access denied."));
      expect((await POST(request())).status).toBe(status);
    }
    expect(from).not.toHaveBeenCalled(); expect(sendMessagePush).not.toHaveBeenCalled();
  });
  it("checks Origin before authentication or database access", async () => {
    expect((await POST(request(undefined, "https://evil.test"))).status).toBe(403);
    expect(requireMember).not.toHaveBeenCalled();
  });
  it("derives sender from membership, trims text and preserves the returned bigint ID", async () => {
    const save = query(stored);
    from.mockReturnValueOnce(query(null)).mockReturnValueOnce(save);
    const result = await POST(request());
    expect(result.status).toBe(201);
    expect(save.insert).toHaveBeenCalledWith({ body: "Hello", client_nonce: nonce, sender_id: userId });
    expect((await result.json()).message.id).toBe("9007199254740993");
    expect(after).toHaveBeenCalledTimes(1);
  });
  it("returns an existing nonce without inserting or sending a second push", async () => {
    from.mockReturnValueOnce(query(stored));
    const result = await POST(request());
    expect(result.status).toBe(200); expect(from).toHaveBeenCalledTimes(1); expect(after).not.toHaveBeenCalled();
  });
  it.each(["a".repeat(2001), "안녕 👋\n".repeat(500).trim(), "한글 👋\n".repeat(10000).trim()])("saves and returns long text unchanged (%#)", async (body) => {
    const save = query({ ...stored, body });
    from.mockReturnValueOnce(query(null)).mockReturnValueOnce(save);
    const result = await POST(request({ body: ` \n${body}\n `, client_nonce: nonce }));
    expect(result.status).toBe(201);
    expect(save.insert).toHaveBeenCalledWith({ body, client_nonce: nonce, sender_id: userId });
    expect((await result.json()).message.body).toBe(body);
  });
  it("retains a transfer-size boundary without querying messages for an oversized request", async () => {
    const result = await POST(request({ body: "a".repeat(4 * 1024 * 1024), client_nonce: nonce }));
    expect(result.status).toBe(413);
    expect(from).not.toHaveBeenCalled();
    expect(after).not.toHaveBeenCalled();
  });
  it("recovers the stored message after a concurrent unique-constraint race", async () => {
    from.mockReturnValueOnce(query(null)).mockReturnValueOnce(query(null, { code: "23505" })).mockReturnValueOnce(query(stored));
    expect((await POST(request())).status).toBe(200);
    expect(after).not.toHaveBeenCalled();
  });
  it("rejects invalid payloads and maps the database rate limit to 429", async () => {
    expect((await POST(request({ body: " ", client_nonce: nonce }))).status).toBe(400);
    expect(from).not.toHaveBeenCalled();
    from.mockReturnValueOnce(query(null)).mockReturnValueOnce(query(null, { code: "P0001", message: "Request rate limit exceeded." }));
    expect((await POST(request())).status).toBe(429); expect(after).not.toHaveBeenCalled();
  });
  it("keeps saved messages successful even when background push fails", async () => {
    from.mockReturnValueOnce(query(null)).mockReturnValueOnce(query(stored));
    vi.mocked(sendMessagePush).mockRejectedValueOnce(new Error("network failed"));
    const result = await POST(request());
    expect(result.status).toBe(201);
    const task = vi.mocked(after).mock.calls[0][0];
    if (typeof task !== "function") throw new Error("Expected a deferred push task");
    await expect(task()).resolves.toBeUndefined();
  });
});
