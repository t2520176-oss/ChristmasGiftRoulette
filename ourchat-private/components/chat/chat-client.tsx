"use client";

import { useCallback, useEffect, useLayoutEffect, useRef, useState, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import type { RealtimeChannel } from "@supabase/supabase-js";
import {
  ArrowDown, ArrowUp, Check, CheckCheck, ChevronDown, Heart, LoaderCircle,
  LockKeyhole, LogOut, MessageCircle, Moon, Send, Sun, WifiOff,
} from "lucide-react";
import { cleanupCurrentPushSubscription, PwaControls } from "@/components/pwa-controls";
import { createClient } from "@/lib/supabase/browser";
import type { ChatMessage, Member, ReadReceipt } from "@/lib/types";
import {
  canSendDraft, codePointLength, compareMessageIds, highestVisibleMessageId,
  isMessageId, isRead, mergeMessages, mergeReceipts, messageIdentity,
  newestMessageId, oldestMessageId, type VisibleMessage,
} from "@/lib/chat-state";

type Props = {
  currentMember: Member;
  peer: Member | null;
  initialMessages: ChatMessage[];
  initialReceipts: ReadReceipt[];
};
type MessagePage = { messages: ChatMessage[]; hasMore: boolean };
type Connection = "connecting" | "connected" | "reconnecting";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20000);
  try {
    const response = await fetch(url, { ...init, signal: controller.signal, cache: "no-store", credentials: "same-origin" });
    const data: unknown = await response.json().catch(() => null);
    if (!response.ok) {
      const message = data && typeof data === "object" && "error" in data &&
        typeof data.error === "string" ? data.error : "Something went wrong. Please try again.";
      throw new Error(message);
    }
    return data as T;
  } catch (cause) {
    if (controller.signal.aborted) throw new Error("The connection took too long. Please try again.");
    throw cause;
  } finally {
    clearTimeout(timeout);
  }
}

function initials(name: string): string {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => Array.from(part)[0]).join("").toUpperCase();
}

function dateLabel(timestamp: string): string {
  const date = new Date(timestamp);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return "Today";
  if (date.toDateString() === yesterday.toDateString()) return "Yesterday";
  return new Intl.DateTimeFormat("en", { month: "short", day: "numeric", year: "numeric" }).format(date);
}

const timeFormatter = new Intl.DateTimeFormat("en", { hour: "numeric", minute: "2-digit" });
const subscribeHydration = () => () => {};
const localDateKey = (timestamp: string, local: boolean) => local
  ? new Date(timestamp).toDateString() : timestamp.slice(0, 10);
let transientTheme: boolean | null = null;

function subscribeOnline(callback: () => void) {
  window.addEventListener("online", callback);
  window.addEventListener("offline", callback);
  return () => { window.removeEventListener("online", callback); window.removeEventListener("offline", callback); };
}

function readTheme() {
  if (transientTheme !== null) return transientTheme;
  try {
    const saved = localStorage.getItem("ourchat-theme");
    if (saved) return saved === "dark";
  } catch { /* The OS preference also works when storage is unavailable. */ }
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

function subscribeTheme(callback: () => void) {
  const preference = window.matchMedia("(prefers-color-scheme: dark)");
  window.addEventListener("storage", callback);
  window.addEventListener("ourchat:theme", callback);
  preference.addEventListener("change", callback);
  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener("ourchat:theme", callback);
    preference.removeEventListener("change", callback);
  };
}

export default function ChatClient({ currentMember, peer, initialMessages, initialReceipts }: Props) {
  const router = useRouter();
  const hydrated = useSyncExternalStore(subscribeHydration, () => true, () => false);
  const [messages, setMessages] = useState<VisibleMessage[]>(() => mergeMessages([], initialMessages));
  const [receipts, setReceipts] = useState(initialReceipts);
  const [draft, setDraft] = useState("");
  const online = useSyncExternalStore(subscribeOnline, () => navigator.onLine, () => true);
  const [connection, setConnection] = useState<Connection>("connecting");
  const [peerOnline, setPeerOnline] = useState(false);
  const [hasMore, setHasMore] = useState(initialMessages.length === 50);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [newCount, setNewCount] = useState(0);
  const [awayFromBottom, setAwayFromBottom] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [accountError, setAccountError] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const dark = useSyncExternalStore(subscribeTheme, readTheme, () => false);
  const [signingOut, setSigningOut] = useState(false);
  const [realtimeEpoch, setRealtimeEpoch] = useState(0);
  const viewportRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const messagesRef = useRef(messages);
  const receiptsRef = useRef(receipts);
  const mountedRef = useRef(true);
  const atBottomRef = useRef(true);
  const forceScrollRef = useRef(true);
  const restoreScrollRef = useRef<{ height: number; top: number } | null>(null);
  const loadingOlderRef = useRef(false);
  const onlineRef = useRef(true);
  const composingRef = useRef(false);
  const visibleIdsRef = useRef(new Set<string>());
  const readTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const readingRef = useRef(false);
  const syncRunningRef = useRef(false);
  const syncAgainRef = useRef(false);
  const realtimeCleanupRef = useRef<(() => void) | null>(null);
  const sendInFlightRef = useRef(new Set<string>());
  const sessionActiveRef = useRef(true);
  const lastReadRef = useRef(initialReceipts.find((receipt) => receipt.user_id === currentMember.user_id)?.last_read_message_id ?? null);

  const acceptMessages = useCallback((incoming: readonly VisibleMessage[], countNew = true) => {
    if (!mountedRef.current || !incoming.length) return;
    const current = messagesRef.current;
    if (countNew && !atBottomRef.current) {
      const known = new Set(current.map(messageIdentity));
      const fresh = incoming.filter((message) => isMessageId(message.id) && message.sender_id !== currentMember.user_id && !known.has(messageIdentity(message)));
      if (fresh.length) setNewCount((count) => count + fresh.length);
    }
    const next = mergeMessages(current, incoming);
    messagesRef.current = next;
    setMessages(next);
  }, [currentMember.user_id]);

  const acceptReceipts = useCallback((incoming: readonly ReadReceipt[]) => {
    if (!mountedRef.current) return;
    const next = mergeReceipts(receiptsRef.current, incoming);
    receiptsRef.current = next;
    setReceipts(next);
    const own = next.find((receipt) => receipt.user_id === currentMember.user_id)?.last_read_message_id;
    if (own && (!lastReadRef.current || compareMessageIds(own, lastReadRef.current) > 0)) lastReadRef.current = own;
  }, [currentMember.user_id]);

  const flushRead = useCallback(async function flushVisibleRead() {
    if (!mountedRef.current || !sessionActiveRef.current || !onlineRef.current || readingRef.current || document.visibilityState !== "visible") return;
    const candidate = highestVisibleMessageId(visibleIdsRef.current);
    if (!candidate || (lastReadRef.current && compareMessageIds(candidate, lastReadRef.current) <= 0)) return;
    readingRef.current = true;
    try {
      const result = await request<{ receipt: ReadReceipt }>("/api/read", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message_id: candidate }),
      });
      acceptReceipts([result.receipt]);
      const next = highestVisibleMessageId(visibleIdsRef.current);
      if (next && compareMessageIds(next, candidate) > 0) {
        readTimerRef.current = setTimeout(() => { void flushVisibleRead(); }, 450);
      }
    } catch {
      // Reading is retried on the next visible message or reconnect, without changing its local position.
    } finally {
      readingRef.current = false;
    }
  }, [acceptReceipts]);

  const queueRead = useCallback(() => {
    if (readTimerRef.current) clearTimeout(readTimerRef.current);
    readTimerRef.current = setTimeout(() => { void flushRead(); }, 450);
  }, [flushRead]);

  const synchronize = useCallback(async () => {
    if (!mountedRef.current || !sessionActiveRef.current || !onlineRef.current) return;
    if (syncRunningRef.current) { syncAgainRef.current = true; return; }
    syncRunningRef.current = true;
    try {
      do {
        syncAgainRef.current = false;
        let cursor = newestMessageId(messagesRef.current);
        let more = true;
        while (more && mountedRef.current && sessionActiveRef.current && onlineRef.current) {
          const query = cursor ? `?after=${encodeURIComponent(cursor)}` : "";
          const page = await request<MessagePage>(`/api/messages${query}`);
          if (!mountedRef.current) break;
          acceptMessages(page.messages);
          const next = newestMessageId(page.messages);
          more = Boolean(cursor && page.hasMore && next && compareMessageIds(next, cursor) > 0);
          if (!cursor) setHasMore(page.hasMore);
          if (next) cursor = next;
        }
        if (!mountedRef.current || !sessionActiveRef.current) break;
        const result = await request<{ receipts: ReadReceipt[] }>("/api/read");
        acceptReceipts(result.receipts);
        queueRead();
        setError(null);
      } while (syncAgainRef.current && mountedRef.current && sessionActiveRef.current && onlineRef.current);
    } catch (cause) {
      if (mountedRef.current) setError(cause instanceof Error ? cause.message : "Could not refresh messages.");
    } finally {
      syncRunningRef.current = false;
    }
  }, [acceptMessages, acceptReceipts, queueRead]);

  useEffect(() => {
    mountedRef.current = true;
    const isOnline = navigator.onLine;
    onlineRef.current = isOnline;
    const onOnline = () => { onlineRef.current = true; void synchronize(); queueRead(); };
    const onOffline = () => { onlineRef.current = false; setPeerOnline(false); };
    const onVisible = () => { if (document.visibilityState === "visible") { void synchronize(); queueRead(); } };
    window.addEventListener("online", onOnline);
    window.addEventListener("offline", onOffline);
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      mountedRef.current = false;
      window.removeEventListener("online", onOnline);
      window.removeEventListener("offline", onOffline);
      document.removeEventListener("visibilitychange", onVisible);
      if (readTimerRef.current) clearTimeout(readTimerRef.current);
    };
  }, [queueRead, synchronize]);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
  }, [dark]);

  useEffect(() => {
    const supabase = createClient();
    let disposed = false;
    let refreshTimer: ReturnType<typeof setTimeout> | null = null;
    const refresh = () => {
      if (refreshTimer) clearTimeout(refreshTimer);
      refreshTimer = setTimeout(() => { if (!disposed) void synchronize(); }, 120);
    };
    const channel: RealtimeChannel = supabase.channel("ourchat:couple", {
      config: { private: true, presence: { key: currentMember.user_id }, postgres_changes_options: { wait: true } },
    });
    channel
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "messages" }, refresh)
      .on("postgres_changes", { event: "*", schema: "public", table: "read_receipts" }, refresh)
      .on("presence", { event: "sync" }, () => {
        if (disposed) return;
        const present = Object.values(channel.presenceState<{ user_id: string }>()).flat();
        setPeerOnline(Boolean(peer && present.some((member) => member.user_id === peer.user_id)));
      })
      .subscribe((status) => {
        if (disposed) return;
        if (status === "SUBSCRIBED") {
          setConnection("connected");
          void channel.track({ user_id: currentMember.user_id });
          void synchronize();
        } else if (status === "CHANNEL_ERROR" || status === "TIMED_OUT" || status === "CLOSED") {
          setConnection("reconnecting");
          setPeerOnline(false);
        }
      });
    const cleanup = () => {
      disposed = true;
      if (refreshTimer) clearTimeout(refreshTimer);
      void channel.untrack();
      void supabase.removeChannel(channel);
    };
    realtimeCleanupRef.current = cleanup;
    // Initial synchronization also works while Realtime is still establishing its connection.
    void synchronize();
    return cleanup;
  }, [currentMember.user_id, peer, realtimeEpoch, synchronize]);

  useLayoutEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    if (restoreScrollRef.current) {
      const previous = restoreScrollRef.current;
      viewport.scrollTop = previous.top + viewport.scrollHeight - previous.height;
      restoreScrollRef.current = null;
    } else if (atBottomRef.current || forceScrollRef.current) {
      viewport.scrollTop = viewport.scrollHeight;
      atBottomRef.current = true;
      forceScrollRef.current = false;
      setAwayFromBottom(false);
      setNewCount(0);
    }
  }, [messages]);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    visibleIdsRef.current.clear();
    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        const id = (entry.target as HTMLElement).dataset.messageId;
        if (!id) continue;
        const readable = entry.isIntersecting && (
          entry.intersectionRatio >= 0.5 ||
          entry.intersectionRect.height >= Math.min(entry.boundingClientRect.height, viewport.clientHeight) * 0.7
        );
        if (readable) visibleIdsRef.current.add(id);
        else visibleIdsRef.current.delete(id);
      }
      queueRead();
    }, { root: viewport, threshold: [0, 0.5, 0.7, 1] });
    viewport.querySelectorAll<HTMLElement>("[data-message-id]").forEach((element) => observer.observe(element));
    return () => observer.disconnect();
  }, [messages, queueRead]);

  useEffect(() => {
    const input = textareaRef.current;
    if (input) { input.style.height = "auto"; input.style.height = `${Math.min(input.scrollHeight, 144)}px`; }
  }, [draft]);

  async function loadOlder() {
    const cursor = oldestMessageId(messagesRef.current);
    if (!cursor || !hasMore || loadingOlderRef.current || !onlineRef.current) return;
    loadingOlderRef.current = true;
    setLoadingOlder(true);
    try {
      const page = await request<MessagePage>(`/api/messages?before=${encodeURIComponent(cursor)}`);
      if (!mountedRef.current) return;
      const viewport = viewportRef.current;
      if (viewport) restoreScrollRef.current = { height: viewport.scrollHeight, top: viewport.scrollTop };
      acceptMessages(page.messages, false);
      setHasMore(page.hasMore);
    } catch (cause) {
      if (mountedRef.current) setError(cause instanceof Error ? cause.message : "Could not load earlier messages.");
    } finally {
      loadingOlderRef.current = false;
      if (mountedRef.current) setLoadingOlder(false);
    }
  }

  async function deliver(message: VisibleMessage) {
    if (!sessionActiveRef.current || !onlineRef.current || sendInFlightRef.current.has(message.client_nonce)) return;
    sendInFlightRef.current.add(message.client_nonce);
    acceptMessages([{ ...message, status: "pending", error: undefined }]);
    try {
      const result = await request<{ message: ChatMessage }>("/api/messages", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ body: message.body, client_nonce: message.client_nonce }),
      });
      acceptMessages([result.message]);
    } catch (cause) {
      acceptMessages([{ ...message, status: "failed", error: cause instanceof Error ? cause.message : "Failed to send." }]);
    } finally {
      sendInFlightRef.current.delete(message.client_nonce);
    }
  }

  function send() {
    if (!canSendDraft(draft, onlineRef.current) || signingOut) return;
    const nonce = crypto.randomUUID();
    const message: VisibleMessage = {
      id: `pending:${nonce}`, sender_id: currentMember.user_id,
      body: draft.trim(), client_nonce: nonce, created_at: new Date().toISOString(), status: "pending",
    };
    forceScrollRef.current = true;
    setDraft("");
    void deliver(message);
    textareaRef.current?.focus();
  }

  function jumpToLatest() {
    const viewport = viewportRef.current;
    if (!viewport) return;
    viewport.scrollTo({ top: viewport.scrollHeight, behavior: "smooth" });
  }

  function toggleTheme() {
    const next = !dark;
    transientTheme = next;
    try { localStorage.setItem("ourchat-theme", next ? "dark" : "light"); } catch { /* The in-memory preference lasts for this session. */ }
    window.dispatchEvent(new Event("ourchat:theme"));
  }

  async function signOut() {
    setSigningOut(true);
    setAccountError(null);
    sessionActiveRef.current = false;
    realtimeCleanupRef.current?.();
    if (readTimerRef.current) clearTimeout(readTimerRef.current);
    try {
      await cleanupCurrentPushSubscription();
      await request<{ ok: boolean }>("/api/auth/signout", { method: "POST" });
      router.replace("/login");
      router.refresh();
    } catch (cause) {
      setAccountError(cause instanceof Error ? cause.message : "Could not sign out. Please try again.");
      setSigningOut(false);
      sessionActiveRef.current = true;
      setConnection("connecting");
      setRealtimeEpoch((epoch) => epoch + 1);
    }
  }

  const bodyLength = codePointLength(draft.trim());
  const peerReceipt = receipts.find((receipt) => receipt.user_id === peer?.user_id)?.last_read_message_id;
  const lastOwn = [...messages].reverse().find((message) => message.sender_id === currentMember.user_id && isMessageId(message.id));
  const connectionLabel = !online ? "Offline" : connection === "connecting" ? "Connecting…" : connection === "reconnecting" ? "Reconnecting…" : peerOnline ? "Online" : "Away";
  const peerName = peer?.display_name ?? "Your person";

  return (
    <div className="flex min-h-dvh flex-col bg-[#f7f5f0] text-[#283c37] dark:bg-[#131c1a] dark:text-[#e7eee9]">
      <header className="shrink-0 border-b border-[#dedfd6] bg-[#fcfbf8]/95 dark:border-[#304039] dark:bg-[#19231f]">
        <div className="mx-auto flex h-20 max-w-6xl items-center justify-between px-5 sm:px-8">
          <a href="/chat" aria-label="OurChat home" className="flex items-center gap-3 rounded-xl outline-offset-4 focus-visible:outline-2 focus-visible:outline-[#35624d]">
            <span className="flex size-10 items-center justify-center rounded-2xl bg-[#244d3d] text-[#fbf8ef] dark:bg-[#b6ccb8] dark:text-[#1e392e]"><MessageCircle size={22} strokeWidth={1.6} /></span>
            <span className="text-[23px] font-semibold tracking-[-0.8px]">OurChat<span className="text-[#b0b9a8]">.</span></span>
          </a>
          <div className="flex items-center gap-3 sm:gap-5">
            <span className="hidden items-center gap-1.5 text-xs text-[#718175] sm:flex dark:text-[#b5c3b7]"><LockKeyhole size={12} /> Just the two of us</span>
            <button type="button" onClick={toggleTheme} className="rounded-full p-2.5 text-[#607469] transition hover:bg-[#e9ede4] focus-visible:outline-2 focus-visible:outline-[#35624d] dark:text-[#bbcebf] dark:hover:bg-[#304039]" aria-label={dark ? "Use light mode" : "Use dark mode"}>{dark ? <Sun size={19} /> : <Moon size={19} />}</button>
            <div className="relative">
              <button type="button" onClick={() => setSettingsOpen((open) => !open)} aria-expanded={settingsOpen} aria-controls="account-menu" className="flex items-center gap-2 rounded-full border border-[#dde2d8] bg-white px-2 py-1.5 text-sm focus-visible:outline-2 focus-visible:outline-[#35624d] dark:border-[#3c4d42] dark:bg-[#223029]">
                <span className="flex size-8 items-center justify-center rounded-full bg-[#e8edde] text-xs font-semibold text-[#516447] dark:bg-[#3b4c3a] dark:text-[#dce9cd]">{initials(currentMember.display_name)}</span>
                <span className="hidden max-w-24 truncate sm:inline">{currentMember.display_name}</span><ChevronDown size={14} className="mr-1" />
              </button>
              <div id="account-menu" hidden={!settingsOpen} className="absolute right-0 top-full z-30 mt-2 w-80 max-w-[calc(100vw-2.5rem)] rounded-2xl border border-[#dee4d9] bg-[#fcfbf8] p-4 shadow-xl dark:border-[#3a4c3f] dark:bg-[#23302a]">
                <p className="mb-1 text-sm font-semibold">Your little corner</p><p className="mb-4 text-xs leading-5 text-[#718175] dark:text-[#b8c7bb]">Install OurChat and stay in touch on your devices.</p>
                <PwaControls />
                <button type="button" disabled={signingOut} onClick={() => void signOut()} className="mt-4 flex w-full items-center gap-2 rounded-xl border-t border-[#e2e6dc] px-2 pt-4 pb-1 text-sm hover:text-[#a04c39] disabled:opacity-50 dark:border-[#405143]"><LogOut size={16} />{signingOut ? "Signing out…" : "Sign out"}</button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-6xl min-h-0 flex-1 flex-col px-0 sm:px-8 sm:py-8 lg:flex-row lg:gap-8">
        <aside className="hidden w-60 shrink-0 flex-col py-8 lg:flex">
          <span className="mb-4 text-[10px] font-semibold tracking-[0.23em] text-[#8a9889] dark:text-[#a5b4a4]">A SPACE FOR US</span>
          <h1 className="font-serif text-4xl leading-[1.18] tracking-[-1px]">Close, even<br />from here.</h1>
          <p className="mt-5 max-w-48 text-sm leading-7 text-[#7b8879] dark:text-[#a8baaa]">The everyday things.<br />The thinking-of-you things.<br />All in one little place.</p>
          <div className="mt-10 flex items-center gap-3 rounded-2xl border border-[#dde3d5] bg-[#eef1e7] p-4 dark:border-[#3c4d3c] dark:bg-[#233126]">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-[#d9e2cb] font-serif text-lg text-[#4f6549] dark:bg-[#3b5138] dark:text-[#dae7d0]">{initials(peerName)}</span>
            <div className="min-w-0"><p className="truncate text-sm font-semibold">{peerName}</p><p className="mt-0.5 flex items-center gap-1.5 text-xs text-[#71816a] dark:text-[#afc2a8]"><span className={`size-1.5 rounded-full ${peerOnline && online ? "bg-[#76995b]" : "bg-[#9da69a]"}`} />{peerOnline && online ? "Online" : "Your conversation"}</p></div>
          </div>
          <div className="mt-auto pt-12"><Heart size={19} strokeWidth={1.2} className="mb-3 text-[#a5b095]" /><p className="max-w-44 text-xs leading-6 text-[#919b8a] dark:text-[#a1b09c]">A private conversation,<br />made for the two of you.</p></div>
        </aside>

        <section aria-label={`Conversation with ${peerName}`} className="relative flex h-[calc(100dvh-5rem)] min-h-[400px] min-w-0 flex-1 flex-col overflow-hidden bg-[#fffefa] sm:h-[calc(100dvh-9rem)] sm:rounded-[28px] sm:border sm:border-[#dfe4d8] sm:shadow-[0_8px_40px_rgba(40,60,45,0.04)] dark:border-[#34463a] dark:bg-[#1c2822]">
          <div className="flex min-h-22 shrink-0 items-center justify-between border-b border-[#e9ece3] px-5 py-4 sm:px-7 dark:border-[#35463a]">
            <div className="flex items-center gap-3.5"><span className="relative flex size-11 items-center justify-center rounded-full bg-[#e7eada] font-serif text-xl text-[#687b54] dark:bg-[#3a4a33] dark:text-[#d3e2c5]">{initials(peerName)}<span className={`absolute right-0 bottom-0 size-3 rounded-full border-2 border-[#fffefa] dark:border-[#1c2822] ${peerOnline && online ? "bg-[#8ba370]" : "bg-[#adb5a6]"}`} /></span><div><h2 className="text-[17px] font-semibold tracking-[-0.3px]">{peerName}</h2><p role="status" aria-live="polite" className="mt-0.5 flex items-center gap-1 text-xs text-[#87917d] dark:text-[#adbea5]">{!online && <WifiOff size={11} />}{connectionLabel}</p></div></div>
            <span className="rounded-full border border-[#e4e8dd] px-3 py-1.5 text-[10px] font-medium tracking-wide text-[#89977e] dark:border-[#3b4c3c] dark:text-[#b1c0a8]">OUR CONVERSATION</span>
          </div>

          {!online && <div role="status" className="flex items-center justify-center gap-2 bg-[#f3e8cd] px-4 py-2 text-xs text-[#7b602d] dark:bg-[#493d23] dark:text-[#efcf8e]"><WifiOff size={13} />Offline. Your draft stays here. Reconnect to send.</div>}
          {(accountError || error) && <div role="alert" className="flex items-center justify-between gap-3 bg-[#fbebe3] px-4 py-2 text-xs text-[#98573e] dark:bg-[#4b3028] dark:text-[#efb99f]"><span>{accountError || error}</span><button type="button" onClick={() => { if (accountError) void signOut(); else void synchronize(); }} className="shrink-0 underline underline-offset-2">Try again</button></div>}

          <div ref={viewportRef} role="region" aria-label="Message history" tabIndex={0} className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 pb-6 [scrollbar-width:thin] focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-[#96aa83] sm:px-7" onScroll={() => {
            const viewport = viewportRef.current;
            if (!viewport) return;
            const atBottom = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight < 80;
            atBottomRef.current = atBottom;
            setAwayFromBottom(!atBottom);
            if (atBottom) setNewCount(0);
            if (viewport.scrollTop < 60 && !atBottom) void loadOlder();
          }}>
            {hasMore && <div className="flex justify-center pt-5"><button type="button" disabled={loadingOlder || !online} onClick={() => void loadOlder()} className="flex items-center gap-1.5 rounded-full px-4 py-2 text-xs text-[#7b8a71] transition hover:bg-[#f0f2e9] disabled:opacity-50 dark:text-[#b3c2a8] dark:hover:bg-[#344132]">{loadingOlder ? <LoaderCircle size={13} className="animate-spin" /> : <ArrowUp size={13} />}{loadingOlder ? "Loading…" : "Earlier messages"}</button></div>}
            {!messages.length && <div className="flex min-h-full flex-col items-center justify-center px-6 py-16 text-center"><span className="mb-5 flex size-16 items-center justify-center rounded-[24px] bg-[#edf1e3] text-[#82956c] dark:bg-[#34452e] dark:text-[#b2c69a]"><Heart size={28} strokeWidth={1.2} /></span><h3 className="font-serif text-2xl">Every hello belongs here.</h3><p className="mt-3 max-w-56 text-sm leading-6 text-[#8a9581] dark:text-[#aeba9f]">Send the first little note to {peerName}.</p></div>}
            {messages.map((message, index) => {
              const own = message.sender_id === currentMember.user_id;
              const previous = messages[index - 1];
              const next = messages[index + 1];
              const showDate = !previous || localDateKey(previous.created_at, hydrated) !== localDateKey(message.created_at, hydrated);
              const grouped = previous?.sender_id === message.sender_id && !showDate;
              const groupEnd = !next || next.sender_id !== message.sender_id || localDateKey(next.created_at, hydrated) !== localDateKey(message.created_at, hydrated);
              const status = message.status ?? "sent";
              const read = own && lastOwn?.id === message.id && isRead(message.id, peerReceipt);
              return <div key={messageIdentity(message)}>
                {showDate && <div className="flex items-center justify-center gap-4 pt-7 pb-6"><span className="h-px w-12 bg-[#e9ecdf] dark:bg-[#394836]" /><span className="text-[10px] font-medium tracking-[0.08em] text-[#99a18c] dark:text-[#a5b394]">{hydrated ? dateLabel(message.created_at) : "Messages"}</span><span className="h-px w-12 bg-[#e9ecdf] dark:bg-[#394836]" /></div>}
                <div data-message-id={isMessageId(message.id) ? message.id : undefined} className={`flex ${own ? "justify-end" : "justify-start"} ${grouped ? "mt-1.5" : "mt-4"}`}>
                  <div className={`max-w-[86%] sm:max-w-[75%] ${own ? "items-end" : "items-start"} flex min-w-0 flex-col`}>
                    {!own && !grouped && <span className="mb-1.5 ml-1 text-[10px] font-medium text-[#8b967f] dark:text-[#abbb9d]">{peerName}</span>}
                    <div className={`whitespace-pre-wrap wrap-anywhere px-4 py-3 text-[14px] leading-[1.7] ${own ? "rounded-[20px] rounded-br-md bg-[#dfe9d3] text-[#344e35] dark:bg-[#425f3b] dark:text-[#f0f6e8]" : "rounded-[20px] rounded-bl-md border border-[#e8eadf] bg-[#f3f3ec] text-[#4b5845] dark:border-[#3b4b38] dark:bg-[#2a3829] dark:text-[#e0e8d7]"} ${status === "pending" ? "opacity-60" : ""}`}>{message.body}</div>
                    {(groupEnd || status !== "sent" || read) && <div className={`mt-1.5 flex flex-wrap items-center gap-1.5 px-1 text-[10px] text-[#9aA18e] dark:text-[#a7b598] ${own ? "justify-end" : "justify-start"}`}><time dateTime={message.created_at}>{hydrated ? timeFormatter.format(new Date(message.created_at)) : "…"}</time>{own && status === "pending" && <span className="inline-flex items-center gap-1"><LoaderCircle size={10} className="animate-spin" />Sending…</span>}{own && status === "failed" && <><span className="text-[#b2664b] dark:text-[#e9ae91]">Failed to send</span><button type="button" disabled={!online || signingOut} title={message.error} onClick={() => void deliver(message)} className="rounded px-1 font-semibold text-[#926043] underline underline-offset-2 disabled:opacity-40 dark:text-[#f0c2a0]" aria-label="Retry failed message">Retry</button></>}{own && status === "sent" && (read ? <span className="inline-flex items-center gap-0.5 text-[#6b8855] dark:text-[#c0d5a2]"><CheckCheck size={13} />Read</span> : <><Check size={11} /><span className="sr-only">Sent</span></>)}</div>}
                  </div>
                </div>
              </div>;
            })}
          </div>

          {awayFromBottom && <div className="pointer-events-none absolute right-0 bottom-36 left-0 z-10 flex justify-center"><button type="button" onClick={jumpToLatest} className="pointer-events-auto flex items-center gap-2 rounded-full border border-[#cfdbbe] bg-[#fcfff5] px-4 py-2.5 text-xs font-medium text-[#5d7549] shadow-md dark:border-[#647953] dark:bg-[#344b2b] dark:text-[#e1edcf]"><ArrowDown size={14} />{newCount ? `New messages${newCount > 1 ? ` (${newCount})` : ""}` : "Back to latest"}</button></div>}

          <form className="shrink-0 border-t border-[#e9ece2] bg-[#fffefa] px-4 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))] sm:px-6 sm:pb-5 dark:border-[#354536] dark:bg-[#1c2822]" onSubmit={(event) => { event.preventDefault(); send(); }}>
            <div className={`flex items-end gap-2 rounded-[20px] border bg-[#f7f8f1] p-2 focus-within:ring-2 focus-within:ring-[#b9caa7]/50 dark:bg-[#253024] ${bodyLength > 2000 ? "border-[#c7785a]" : "border-[#e2e7d8] dark:border-[#45543b]"}`}>
              <label htmlFor="message-input" className="sr-only">Type a message</label><textarea id="message-input" ref={textareaRef} value={draft} onChange={(event) => setDraft(event.target.value)} onCompositionStart={() => { composingRef.current = true; }} onCompositionEnd={() => { composingRef.current = false; }} onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing && !composingRef.current && event.keyCode !== 229) { event.preventDefault(); send(); }
              }} placeholder="Type a message..." rows={1} aria-describedby="composer-help" className="min-h-11 flex-1 resize-none bg-transparent px-2 py-2.5 text-[15px] leading-6 text-[#3b5137] outline-none placeholder:text-[#a3ac96] dark:text-[#e7efdd] dark:placeholder:text-[#93a486]" />
              <button type="submit" disabled={!canSendDraft(draft, online) || signingOut} aria-label="Send message" className="flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#386044] text-[#f7faef] transition hover:bg-[#284e35] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#456e3c] disabled:cursor-not-allowed disabled:bg-[#d8dfcc] disabled:text-[#9da98d] dark:bg-[#a8c08c] dark:text-[#24371c] dark:hover:bg-[#c4d8aa] dark:disabled:bg-[#405037] dark:disabled:text-[#7d8d6d]"><Send size={18} strokeWidth={1.6} /></button>
            </div>
            <div id="composer-help" className="mt-2.5 flex justify-between gap-3 px-1 text-[10px] text-[#a1aa92] dark:text-[#9daf8a]"><span className="hidden sm:inline">Enter to send <span className="px-1.5">·</span> Shift + Enter for a new line</span><span className="sm:hidden">A little note goes a long way.</span><span className={bodyLength > 2000 ? "font-semibold text-[#b05c3b] dark:text-[#efae8c]" : ""} aria-live={bodyLength > 1950 ? "polite" : "off"}>{bodyLength.toLocaleString("en")} / 2,000</span></div>
          </form>
        </section>
      </main>
    </div>
  );
}
