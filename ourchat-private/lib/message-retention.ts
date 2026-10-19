import { isMessageId } from "@/lib/chat-state";
import type { ChatMessage } from "@/lib/types";

/** Seven full days, rather than calendar days (which vary around DST). */
export const MESSAGE_RETENTION_MS = 168 * 60 * 60 * 1000;
const MAX_TIMEOUT_MS = 2_147_483_647;
type RetainedMessage = Pick<ChatMessage, "id" | "created_at"> & { status?: string };

function isUnsent(message: RetainedMessage): boolean {
  return !isMessageId(message.id) && (message.status === "pending" || message.status === "failed");
}

export function isMessageRetained(message: RetainedMessage, now: number): boolean {
  if (isUnsent(message)) return true;
  const created = Date.parse(message.created_at);
  return Number.isFinite(created) && created > now - MESSAGE_RETENTION_MS;
}

export function retainMessages<T extends RetainedMessage>(messages: readonly T[], now: number): T[] {
  return messages.filter((message) => isMessageRetained(message, now));
}

/** A single timer for the next stored message, with the browser's safe delay bound. */
export function nextMessageExpiryDelay(messages: readonly RetainedMessage[], now: number): number | null {
  let next = Infinity;
  for (const message of messages) {
    if (isUnsent(message)) continue;
    const created = Date.parse(message.created_at);
    if (!Number.isFinite(created)) return 0;
    next = Math.min(next, created + MESSAGE_RETENTION_MS);
  }
  return next === Infinity ? null : Math.max(0, Math.min(MAX_TIMEOUT_MS, next - now));
}
