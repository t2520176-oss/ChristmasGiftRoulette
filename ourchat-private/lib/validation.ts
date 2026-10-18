import { z } from "zod";

export const MAX_MESSAGE_CHARS = 2000;
export function messageLength(value: string) { return Array.from(value).length; }
export const messageBodySchema = z.string().transform((text) => text.trim())
  .refine((text) => messageLength(text) >= 1 && messageLength(text) <= MAX_MESSAGE_CHARS,
    "Messages must contain between 1 and 2,000 characters.");
export const messageInputSchema = z.object({ body: messageBodySchema, client_nonce: z.uuid() }).strict();
export const messageIdSchema = z.string().regex(/^[1-9]\d{0,18}$/)
  .refine((id) => { try { return BigInt(id) <= 9223372036854775807n; } catch { return false; } }, "Invalid message ID.");
export const readInputSchema = z.object({ message_id: messageIdSchema }).strict();
export const signInSchema = z.object({
  email: z.string().trim().email().max(254),
  password: z.string().min(1).max(1024),
}).strict();
export const memberSchema = z.object({ user_id: z.uuid(), display_name: z.string(), created_at: z.string().optional() });
export const messageRowSchema = z.object({ id: messageIdSchema, sender_id: z.uuid(), body: z.string(), client_nonce: z.uuid(), created_at: z.string() });
export const receiptRowSchema = z.object({ user_id: z.uuid(), last_read_message_id: messageIdSchema.nullable(), updated_at: z.string() });

const PUSH_HOSTS = ["fcm.googleapis.com", "updates.push.services.mozilla.com", "web.push.apple.com"];
const PUSH_SUFFIXES = [".push.services.mozilla.com", ".push.apple.com", ".notify.windows.com", ".wns.windows.com"];
export function isAllowedPushEndpoint(value: string): boolean {
  try {
    const url = new URL(value);
    if (value.length > 2048 || url.protocol !== "https:" || url.username || url.password || url.hash || (url.port && url.port !== "443")) return false;
    return PUSH_HOSTS.includes(url.hostname) || PUSH_SUFFIXES.some((suffix) => url.hostname.endsWith(suffix));
  } catch { return false; }
}
function base64UrlBytes(value: string, length: number, publicKey = false) {
  if (!/^[A-Za-z0-9_-]+$/.test(value)) return false;
  try {
    const bytes = Uint8Array.from(atob(value.replace(/-/g, "+").replace(/_/g, "/")), (c) => c.charCodeAt(0));
    return bytes.length === length && (!publicKey || bytes[0] === 4);
  } catch { return false; }
}
export const endpointSchema = z.string().max(2048).refine(isAllowedPushEndpoint, "This push service is not supported.");
export const subscriptionSchema = z.object({
  endpoint: endpointSchema,
  expirationTime: z.number().nullable().optional(),
  keys: z.object({
    p256dh: z.string().max(100).refine((s) => base64UrlBytes(s, 65, true), "Invalid push key."),
    auth: z.string().max(32).refine((s) => base64UrlBytes(s, 16), "Invalid push key."),
  }).strict(),
}).strict();
export const deleteSubscriptionSchema = z.object({ endpoint: endpointSchema }).strict();
