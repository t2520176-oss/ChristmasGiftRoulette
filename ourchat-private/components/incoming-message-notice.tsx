"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { MessageCircle, X } from "lucide-react";

type Notice = { revision: number; count: number };
type PauseReason = "hover" | "focus";
const lifetime = 6000;

/** Foreground notices contain counts only; message text stays in the conversation. */
export function useIncomingMessageNotice() {
  const [notice, setNotice] = useState<Notice | null>(null);
  const currentRef = useRef<Notice | null>(null);
  const revisionRef = useRef(0);
  const mountedRef = useRef(true);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const remainingRef = useRef(lifetime);
  const deadlineRef = useRef(0);
  const pausedRef = useRef({ hover: false, focus: false });

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) clearTimeout(timerRef.current);
    timerRef.current = null;
  }, []);

  const dismiss = useCallback(() => {
    clearTimer();
    currentRef.current = null;
    pausedRef.current = { hover: false, focus: false };
    remainingRef.current = lifetime;
    if (mountedRef.current) setNotice(null);
  }, [clearTimer]);

  const schedule = useCallback(() => {
    clearTimer();
    if (!currentRef.current || pausedRef.current.hover || pausedRef.current.focus) return;
    deadlineRef.current = Date.now() + remainingRef.current;
    timerRef.current = setTimeout(dismiss, remainingRef.current);
  }, [clearTimer, dismiss]);

  const show = useCallback((amount: number) => {
    if (!mountedRef.current || !Number.isFinite(amount) || amount < 1 ||
      document.visibilityState !== "visible" || !document.hasFocus()) return;
    const next = {
      revision: ++revisionRef.current,
      count: (currentRef.current?.count ?? 0) + Math.floor(amount),
    };
    currentRef.current = next;
    remainingRef.current = lifetime;
    setNotice(next);
    schedule();
  }, [schedule]);

  const pause = useCallback((reason: PauseReason) => {
    if (!currentRef.current || pausedRef.current[reason]) return;
    if (timerRef.current !== null) remainingRef.current = Math.max(0, deadlineRef.current - Date.now());
    pausedRef.current[reason] = true;
    clearTimer();
  }, [clearTimer]);

  const resume = useCallback((reason: PauseReason) => {
    pausedRef.current[reason] = false;
    schedule();
  }, [schedule]);

  useEffect(() => {
    mountedRef.current = true;
    const onVisibility = () => { if (document.visibilityState !== "visible") dismiss(); };
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("blur", dismiss);
    return () => {
      mountedRef.current = false;
      clearTimer();
      currentRef.current = null;
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("blur", dismiss);
    };
  }, [clearTimer, dismiss]);

  return { notice, show, dismiss, pause, resume };
}

export function IncomingMessageNotice({ notifications, senderName, onView }: {
  notifications: ReturnType<typeof useIncomingMessageNotice>;
  senderName: string;
  onView: () => void;
}) {
  if (!notifications.notice) return null;
  const count = notifications.notice.count;
  return <div className="pointer-events-none absolute inset-x-3 top-[calc(env(safe-area-inset-top)+4.5rem)] z-40 flex justify-center sm:top-[calc(env(safe-area-inset-top)+5.5rem)]">
    <div role="status" aria-label="Incoming message notification" aria-live="polite" aria-atomic="true"
      onMouseEnter={() => notifications.pause("hover")} onMouseLeave={() => notifications.resume("hover")}
      onFocusCapture={() => notifications.pause("focus")}
      onBlurCapture={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) notifications.resume("focus"); }}
      className="pointer-events-auto flex w-full max-w-md items-center gap-2 rounded-2xl border border-[#bdcfae] bg-[#f8fbee] p-2 text-[#344e35] shadow-lg sm:gap-3 sm:p-3 dark:border-[#647953] dark:bg-[#30462b] dark:text-[#edf5e5]">
      <MessageCircle size={20} aria-hidden="true" className="ml-1 hidden shrink-0 sm:block" />
      <div className="min-w-0 flex-1 px-1">
        <p className="truncate text-sm font-semibold">{senderName}</p>
        <p className="text-xs leading-5">{count === 1 ? "New message" : `${count.toLocaleString("en")} new messages`}</p>
      </div>
      <button type="button" aria-label="View new messages" onClick={onView} className="min-h-11 shrink-0 rounded-xl px-3 text-sm font-semibold underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-[#35624d] dark:focus-visible:outline-[#b7cf9f]">View</button>
      <button type="button" aria-label="Dismiss message notification" onClick={notifications.dismiss} className="flex size-11 shrink-0 items-center justify-center rounded-xl hover:bg-[#e3ecd7] focus-visible:outline-2 focus-visible:outline-[#35624d] dark:hover:bg-[#425b39] dark:focus-visible:outline-[#b7cf9f]"><X size={19} aria-hidden="true" /></button>
    </div>
  </div>;
}
