import Link from "next/link";
import { redirect } from "next/navigation";
import { requireMember } from "@/lib/server/auth";
import { ApiError, throwDatabaseError } from "@/lib/server/http";
import { listMessages, listReceipts } from "@/lib/server/messages";
import { memberSchema } from "@/lib/validation";
import ChatClient from "@/components/chat/chat-client";

export const dynamic = "force-dynamic";
export default async function ChatPage() {
  let context;
  try { context = await requireMember(); }
  catch (error) {
    if (error instanceof ApiError && [401, 403].includes(error.status)) redirect("/login");
    return <main className="grid min-h-dvh place-items-center bg-canvas p-6 text-white"><section className="max-w-md rounded-3xl border border-white/20 bg-panel p-8 text-white"><h1 className="text-2xl font-semibold">OurChat needs a little setup</h1><p className="mt-4 leading-7 text-white/80">Your administrator needs to finish connecting your private conversation. Please try again when setup is complete.</p><Link className="mt-6 inline-block font-semibold text-white underline" href="/login">Back to sign in</Link></section></main>;
  }
  const [messages, receipts, peerResult] = await Promise.all([
    listMessages(context), listReceipts(context),
    context.client.from("members").select("user_id,display_name,created_at").neq("user_id", context.member.user_id).maybeSingle(),
  ]);
  throwDatabaseError(peerResult.error);
  const peer = peerResult.data ? memberSchema.parse(peerResult.data) : null;
  return <ChatClient currentMember={context.member} peer={peer} initialMessages={messages.messages} initialReceipts={receipts} />;
}
