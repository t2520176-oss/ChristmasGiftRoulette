import { after } from "next/server";
import { requireMember } from "@/lib/server/auth";
import { ApiError, assertSameOrigin, fail, json, readJson, throwDatabaseError } from "@/lib/server/http";
import { listMessages, MESSAGE_COLUMNS } from "@/lib/server/messages";
import { sendMessagePush } from "@/lib/server/push";
import { messageInputSchema, messageRowSchema } from "@/lib/validation";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 30;
export async function GET(request: Request) {
  try {
    const context = await requireMember();
    const params = new URL(request.url).searchParams;
    return json(await listMessages(context, params.get("before") ?? undefined, params.get("after") ?? undefined));
  } catch (error) { return fail(error); }
}
export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const context = await requireMember();
    const input = await readJson(request, messageInputSchema);
    const existing = await context.client.from("messages").select(MESSAGE_COLUMNS)
      .eq("sender_id", context.member.user_id).eq("client_nonce", input.client_nonce).maybeSingle();
    throwDatabaseError(existing.error);
    if (existing.data) return json({ message: messageRowSchema.parse(existing.data) });
    const saved = await context.client.from("messages").insert({ ...input, sender_id: context.member.user_id })
      .select(MESSAGE_COLUMNS).single();
    if (saved.error?.code === "23505") {
      // Two simultaneous retries may race; the database nonce constraint decides the winner.
      const duplicate = await context.client.from("messages").select(MESSAGE_COLUMNS)
        .eq("sender_id", context.member.user_id).eq("client_nonce", input.client_nonce).single();
      throwDatabaseError(duplicate.error);
      return json({ message: messageRowSchema.parse(duplicate.data) });
    }
    throwDatabaseError(saved.error);
    if (!saved.data) throw new ApiError(500, "Your message could not be saved. Please try again.");
    const message = messageRowSchema.parse(saved.data);
    after(async () => {
      try { await sendMessagePush(context, message); }
      catch { /* Push is best-effort. Do not expose subscriptions or lose a saved message. */ }
    });
    return json({ message }, 201);
  } catch (error) { return fail(error); }
}
