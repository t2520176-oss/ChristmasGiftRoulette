import "server-only";
import { z } from "zod";
import { messageIdSchema, messageRowSchema, receiptRowSchema } from "@/lib/validation";
import type { MemberContext } from "./auth";
import { ApiError, throwDatabaseError } from "./http";

// Text casts keep PostgreSQL bigint IDs exact, including values above Number.MAX_SAFE_INTEGER.
export const MESSAGE_COLUMNS = "id::text,sender_id,body,client_nonce,created_at";
export const RECEIPT_COLUMNS = "user_id,last_read_message_id::text,updated_at";
export async function listMessages(context: MemberContext, before?: string, after?: string) {
  if (before && after) throw new ApiError(400, "Choose one pagination cursor.");
  if (before) messageIdSchema.parse(before);
  if (after) messageIdSchema.parse(after);
  let query = context.client.from("messages").select(MESSAGE_COLUMNS).limit(51);
  if (before) query = query.lt("id", before);
  if (after) query = query.gt("id", after);
  const { data, error } = await query.order("id", { ascending: Boolean(after) });
  throwDatabaseError(error);
  const rows = z.array(messageRowSchema).parse(data ?? []);
  const hasMore = rows.length > 50;
  const page = rows.slice(0, 50);
  return { messages: after ? page : page.reverse(), hasMore };
}
export async function listReceipts(context: MemberContext) {
  const { data, error } = await context.client.from("read_receipts").select(RECEIPT_COLUMNS);
  throwDatabaseError(error);
  return z.array(receiptRowSchema).parse(data ?? []);
}
