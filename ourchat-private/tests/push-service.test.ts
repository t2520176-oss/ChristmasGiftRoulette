import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createClient } from "@supabase/supabase-js";
import webPush from "web-push";
import { sendMessagePush } from "@/lib/server/push";
import type { MemberContext } from "@/lib/server/auth";
import type { ChatMessage } from "@/lib/types";

vi.mock("@supabase/supabase-js", () => ({ createClient: vi.fn() }));
vi.mock("web-push", () => ({ default: { setVapidDetails: vi.fn(), sendNotification: vi.fn() } }));
const sender = "40000000-0000-4000-8000-000000000001";
const peer = "40000000-0000-4000-8000-000000000002";
const message: ChatMessage = { id: "1", sender_id: sender, body: "Private message contents", client_nonce: "50000000-0000-4000-8000-000000000001", created_at: "2026-10-18T00:00:00Z" };
const key = Buffer.concat([Buffer.from([4]), Buffer.alloc(64, 1)]).toString("base64url");
const auth = Buffer.alloc(16, 2).toString("base64url");
function context(peerData: { user_id: string } | null = { user_id: peer }) {
  const maybeSingle = vi.fn().mockResolvedValue({ data: peerData, error: null });
  const neq = vi.fn().mockReturnValue({ maybeSingle });
  return { authorized: true, user: { id: sender }, member: { user_id: sender, display_name: "Husband" },
    client: { from: vi.fn().mockReturnValue({ select: vi.fn().mockReturnValue({ neq }) }) },
  } as unknown as MemberContext;
}
beforeEach(() => {
  vi.resetAllMocks();
  // Synthetic test values; no actual push service or database is contacted.
  for (const [name, value] of Object.entries({ NEXT_PUBLIC_SUPABASE_URL: "https://synthetic.supabase.co", NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY: "synthetic-publishable-key", SUPABASE_SERVICE_ROLE_KEY: "synthetic-server-key", NEXT_PUBLIC_VAPID_PUBLIC_KEY: "synthetic-vapid-public", VAPID_PRIVATE_KEY: "synthetic-vapid-private", VAPID_SUBJECT: "mailto:admin@example.test" })) vi.stubEnv(name, value);
  vi.mocked(webPush.sendNotification).mockResolvedValue({ statusCode: 201, body: "", headers: {} });
});
afterEach(() => vi.unstubAllEnvs());
describe("private peer push delivery", () => {
  it("does not construct a privileged client without validated sender membership", async () => {
    await sendMessagePush({ ...context(), authorized: false } as unknown as MemberContext, message);
    await sendMessagePush(context(), { ...message, sender_id: peer });
    expect(createClient).not.toHaveBeenCalled();
  });
  it("does not query administrator endpoints before locating a distinct peer through RLS", async () => {
    await sendMessagePush(context(null), message);
    await sendMessagePush(context({ user_id: sender }), message);
    expect(createClient).not.toHaveBeenCalled();
  });
  it("targets only peer subscriptions, never includes message content and cleans 410 endpoints", async () => {
    const rows = [{ id: "expired", endpoint: "https://fcm.googleapis.com/token/old", p256dh: key, auth }, { id: "active", endpoint: "https://web.push.apple.com/token/new", p256dh: key, auth }, { id: "ssrf", endpoint: "https://127.0.0.1/internal", p256dh: key, auth }];
    const lookup = vi.fn().mockResolvedValue({ data: rows, error: null });
    const deletedUser = vi.fn().mockResolvedValue({ error: null });
    const deletedId = vi.fn().mockReturnValue({ eq: deletedUser });
    const adminFrom = vi.fn().mockReturnValue({ select: vi.fn().mockReturnValue({ eq: lookup }), delete: vi.fn().mockReturnValue({ eq: deletedId }) });
    vi.mocked(createClient).mockReturnValue({ from: adminFrom } as unknown as ReturnType<typeof createClient>);
    vi.mocked(webPush.sendNotification).mockRejectedValueOnce({ statusCode: 410 }).mockResolvedValueOnce({ statusCode: 201, body: "", headers: {} });
    await sendMessagePush(context(), message);
    expect(lookup).toHaveBeenCalledWith("user_id", peer);
    expect(webPush.sendNotification).toHaveBeenCalledTimes(2);
    for (const call of vi.mocked(webPush.sendNotification).mock.calls) expect(String(call[1])).not.toContain(message.body);
    expect(deletedId).toHaveBeenCalledWith("id", "expired");
    expect(deletedUser).toHaveBeenCalledWith("user_id", peer);
  });
  it("isolates ordinary push network errors without deleting subscriptions", async () => {
    const del = vi.fn();
    vi.mocked(createClient).mockReturnValue({ from: vi.fn().mockReturnValue({ select: vi.fn().mockReturnValue({ eq: vi.fn().mockResolvedValue({ data: [{ id: "valid", endpoint: "https://fcm.googleapis.com/token/valid", p256dh: key, auth }], error: null }) }), delete: del }) } as unknown as ReturnType<typeof createClient>);
    vi.mocked(webPush.sendNotification).mockRejectedValue(new Error("offline"));
    await expect(sendMessagePush(context(), message)).resolves.toBeUndefined();
    expect(del).not.toHaveBeenCalled();
  });
});
