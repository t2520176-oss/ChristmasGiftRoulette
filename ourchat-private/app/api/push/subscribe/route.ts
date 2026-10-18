import { checkRateLimit, requireMember } from "@/lib/server/auth";
import { ApiError, assertSameOrigin, fail, json, readJson, throwDatabaseError } from "@/lib/server/http";
import { deleteSubscriptionSchema, subscriptionSchema } from "@/lib/validation";

export const runtime = "nodejs";
export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const context = await requireMember();
    if (!process.env.SUPABASE_SERVICE_ROLE_KEY || !process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY || !process.env.VAPID_PRIVATE_KEY || !process.env.VAPID_SUBJECT) {
      throw new ApiError(503, "Notifications are not configured yet. Please contact the administrator.");
    }
    const subscription = await readJson(request, subscriptionSchema);
    await checkRateLimit(context, "push_subscribe");
    // Endpoint ownership is enforced by RLS and a database owner-immutability trigger.
    const result = await context.client.from("push_subscriptions").upsert({
      user_id: context.member.user_id,
      endpoint: subscription.endpoint,
      p256dh: subscription.keys.p256dh,
      auth: subscription.keys.auth,
    }, { onConflict: "endpoint" });
    if (result.error?.code === "23505") throw new ApiError(409, "This browser subscription is already registered. Please reset its notification permission and try again.");
    throwDatabaseError(result.error);
    return json({ ok: true });
  } catch (error) { return fail(error); }
}
export async function DELETE(request: Request) {
  try {
    assertSameOrigin(request);
    const context = await requireMember();
    const input = await readJson(request, deleteSubscriptionSchema);
    await checkRateLimit(context, "push_delete");
    const result = await context.client.from("push_subscriptions").delete().eq("endpoint", input.endpoint).eq("user_id", context.member.user_id);
    throwDatabaseError(result.error);
    return json({ ok: true });
  } catch (error) { return fail(error); }
}
