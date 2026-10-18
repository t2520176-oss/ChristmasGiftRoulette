import type { ChatMessage, ReadReceipt } from "@/lib/types";

export type DeliveryState = "pending" | "failed" | "sent";
export type VisibleMessage = ChatMessage & {
  status?: DeliveryState;
  error?: string;
};

export function isMessageId(value: string): boolean {
  return /^[1-9]\d*$/.test(value);
}

/** Compare PostgreSQL bigint identifiers without converting them to JS numbers. */
export function compareMessageIds(left: string, right: string): number {
  const a = BigInt(left);
  const b = BigInt(right);
  return a < b ? -1 : a > b ? 1 : 0;
}

export function messageIdentity(message: Pick<ChatMessage, "sender_id" | "client_nonce">): string {
  return `${message.sender_id}:${message.client_nonce}`;
}

/** Server acknowledgements replace their optimistic copy, including after a retry. */
export function mergeMessages(
  existing: readonly VisibleMessage[],
  incoming: readonly VisibleMessage[],
): VisibleMessage[] {
  const byNonce = new Map<string, VisibleMessage>();
  const nonceById = new Map<string, string>();
  for (const message of [...existing, ...incoming]) {
    const identity = messageIdentity(message);
    const previous = byNonce.get(identity);
    const status = message.status ?? "sent";
    if (previous && (previous.status ?? "sent") === "sent" && status !== "sent") {
      continue;
    }
    const duplicateIdentity = nonceById.get(message.id);
    if (duplicateIdentity && duplicateIdentity !== identity) byNonce.delete(duplicateIdentity);
    const next = { ...previous, ...message, status };
    if (status === "sent") delete next.error;
    byNonce.set(identity, next);
    if (isMessageId(message.id)) nonceById.set(message.id, identity);
  }
  return [...byNonce.values()].sort((a, b) => {
    const aStored = isMessageId(a.id);
    const bStored = isMessageId(b.id);
    if (aStored && bStored) return compareMessageIds(a.id, b.id);
    if (aStored !== bStored) return aStored ? -1 : 1;
    return a.created_at.localeCompare(b.created_at) || a.client_nonce.localeCompare(b.client_nonce);
  });
}

export function newestMessageId(messages: readonly VisibleMessage[]): string | null {
  return messages.reduce<string | null>(
    (latest, message) =>
      isMessageId(message.id) && (!latest || compareMessageIds(message.id, latest) > 0)
        ? message.id
        : latest,
    null,
  );
}

export function oldestMessageId(messages: readonly VisibleMessage[]): string | null {
  return messages.reduce<string | null>(
    (oldest, message) =>
      isMessageId(message.id) && (!oldest || compareMessageIds(message.id, oldest) < 0)
        ? message.id
        : oldest,
    null,
  );
}

export function mergeReceipts(
  existing: readonly ReadReceipt[],
  incoming: readonly ReadReceipt[],
): ReadReceipt[] {
  const result = new Map(existing.map((receipt) => [receipt.user_id, receipt]));
  for (const receipt of incoming) {
    const previous = result.get(receipt.user_id);
    if (
      !previous?.last_read_message_id ||
      (receipt.last_read_message_id &&
        compareMessageIds(receipt.last_read_message_id, previous.last_read_message_id) >= 0)
    ) {
      result.set(receipt.user_id, receipt);
    }
  }
  return [...result.values()];
}

export function isRead(messageId: string, receiptId: string | null | undefined): boolean {
  return Boolean(
    isMessageId(messageId) && receiptId && isMessageId(receiptId) &&
      compareMessageIds(messageId, receiptId) <= 0,
  );
}

export function codePointLength(value: string): number {
  return Array.from(value).length;
}

export function canSendDraft(draft: string, online: boolean): boolean {
  const body = draft.trim();
  return online && body.length > 0 && codePointLength(body) <= 2000;
}

export function highestVisibleMessageId(ids: Iterable<string>): string | null {
  let highest: string | null = null;
  for (const id of ids) {
    if (isMessageId(id) && (!highest || compareMessageIds(id, highest) > 0)) highest = id;
  }
  return highest;
}
