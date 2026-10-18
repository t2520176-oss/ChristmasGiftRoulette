import "server-only";
import { createClient } from "@supabase/supabase-js";
import webPush from "web-push";
import { publicConfig } from "@/lib/config";
import { isAllowedPushEndpoint, subscriptionSchema } from "@/lib/validation";
import type { ChatMessage } from "@/lib/types";
import type { MemberContext } from "./auth";

/** Called only after authenticated RLS insertion; failure never reverses a saved message. */
export async function sendMessagePush(context: MemberContext, message: ChatMessage) {
  if (!context.authorized || context.user.id !== context.member.user_id || message.sender_id !== context.member.user_id) return;
  const config = publicConfig();
  const secret = process.env.SUPABASE_SERVICE_ROLE_KEY;
  const publicKey = process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY;
  const privateKey = process.env.VAPID_PRIVATE_KEY;
  const subject = process.env.VAPID_SUBJECT;
  if (!config || !secret || !publicKey || !privateKey || !subject || !/^(mailto:|https:\/\/)/.test(subject)) return;

  // Discover the peer with the member's RLS client before making any administrator query.
  const peerResult = await context.client.from("members").select("user_id").neq("user_id", context.member.user_id).maybeSingle();
  if (peerResult.error || !peerResult.data?.user_id) return;
  const peerId = String(peerResult.data.user_id);
  if (peerId === context.member.user_id) return;
  const admin = createClient(config.url, secret, { auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false } });
  const { data, error } = await admin.from("push_subscriptions").select("id,endpoint,p256dh,auth").eq("user_id", peerId);
  if (error || !data) return;
  try { webPush.setVapidDetails(subject, publicKey, privateKey); } catch { return; }
  // Batches bound outbound sockets; send to every valid registered peer device.
  for (let start = 0; start < data.length; start += 8) {
    await Promise.allSettled(data.slice(start, start + 8).map(async (row) => {
      const candidate = subscriptionSchema.safeParse({ endpoint: row.endpoint, keys: { p256dh: row.p256dh, auth: row.auth } });
      if (!candidate.success || !isAllowedPushEndpoint(candidate.data.endpoint)) return;
      try {
        await webPush.sendNotification(candidate.data, JSON.stringify({ type: "message" }), { TTL: 3600, urgency: "high", timeout: 4000 });
      } catch (error: unknown) {
        const status = typeof error === "object" && error !== null && "statusCode" in error ? error.statusCode : null;
        if (status === 404 || status === 410) {
          await admin.from("push_subscriptions").delete().eq("id", row.id).eq("user_id", peerId);
        }
      }
    }));
  }
}
