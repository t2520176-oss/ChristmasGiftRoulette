"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Check, Copy, X } from "lucide-react";

type ReturnPosition = {
  element: HTMLElement | null;
  selection: { start: number; end: number; direction: "forward" | "backward" | "none" } | null;
  scroll: { element: HTMLElement; top: number; left: number }[];
};

function rememberPosition(button: HTMLButtonElement): ReturnPosition {
  const element = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  const editable = element instanceof HTMLTextAreaElement || element instanceof HTMLInputElement;
  const selection = editable && element.selectionStart !== null && element.selectionEnd !== null
    ? { start: element.selectionStart, end: element.selectionEnd, direction: element.selectionDirection ?? "none" } : null;
  const scroll: ReturnPosition["scroll"] = [];
  for (let parent = button.parentElement; parent; parent = parent.parentElement) {
    scroll.push({ element: parent, top: parent.scrollTop, left: parent.scrollLeft });
  }
  return { element, selection, scroll };
}

function restorePosition(position: ReturnPosition | null) {
  if (!position) return;
  if (position.element?.isConnected) {
    position.element.focus({ preventScroll: true });
    if (position.selection && (position.element instanceof HTMLTextAreaElement || position.element instanceof HTMLInputElement)) {
      position.element.setSelectionRange(position.selection.start, position.selection.end, position.selection.direction);
    }
  }
  for (const { element, top, left } of position.scroll) {
    if (element.isConnected) {
      element.scrollTop = top;
      element.scrollLeft = left;
    }
  }
}

/** Copy only after a click; clipboard failures keep an explicit manual option. */
export function CopyMessageButton({ body }: { body: string }) {
  const [copied, setCopied] = useState(false);
  const [copying, setCopying] = useState(false);
  const [manual, setManual] = useState(false);
  const titleId = useId();
  const helpId = useId();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const textRef = useRef<HTMLTextAreaElement>(null);
  const returnPositionRef = useRef<ReturnPosition | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);
  const pendingRef = useRef(false);

  useEffect(() => {
    mountedRef.current = true;
    const dialog = dialogRef.current;
    return () => {
      mountedRef.current = false;
      if (timerRef.current) clearTimeout(timerRef.current);
      // Retention can remove a message while its manual-copy dialog is open.
      // Close the modal and return to the connected composer before discarding it.
      const position = returnPositionRef.current;
      returnPositionRef.current = null;
      if (dialog?.open) {
        dialog.close();
        restorePosition(position);
      }
    };
  }, []);

  useEffect(() => {
    if (!manual) return;
    dialogRef.current?.showModal();
    textRef.current?.focus({ preventScroll: true });
    textRef.current?.select();
    textRef.current?.setSelectionRange(0, body.length);
  }, [manual, body]);

  function closeManual() {
    const position = returnPositionRef.current;
    returnPositionRef.current = null;
    if (dialogRef.current?.open) dialogRef.current.close();
    if (mountedRef.current) setManual(false);
    restorePosition(position);
  }

  async function copyMessage() {
    if (pendingRef.current) return;
    pendingRef.current = true;
    if (!returnPositionRef.current && buttonRef.current) returnPositionRef.current = rememberPosition(buttonRef.current);
    if (timerRef.current) clearTimeout(timerRef.current);
    setCopied(false);
    setCopying(true);
    try {
      if (typeof navigator.clipboard?.writeText !== "function") throw new Error("Clipboard unavailable");
      // Keep this directly in the user event so Safari can use its activation.
      await navigator.clipboard.writeText(body);
      if (!mountedRef.current) return;
      returnPositionRef.current = null;
      setCopied(true);
      timerRef.current = setTimeout(() => { timerRef.current = null; setCopied(false); }, 2000);
    } catch {
      if (mountedRef.current) setManual(true);
    } finally {
      pendingRef.current = false;
      if (mountedRef.current) setCopying(false);
    }
  }

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        aria-label={copied ? "Copied" : "Copy message"}
        aria-busy={copying}
        onPointerDown={() => { if (buttonRef.current && !pendingRef.current) returnPositionRef.current = rememberPosition(buttonRef.current); }}
        onMouseDown={(event) => {
          // A mouse click must not discard the composer's focus or selection.
          if (document.activeElement instanceof HTMLTextAreaElement || document.activeElement instanceof HTMLInputElement) event.preventDefault();
        }}
        onClick={() => void copyMessage()}
        className="mt-0.5 inline-flex min-h-11 min-w-11 items-center justify-center gap-1.5 rounded-lg px-2 text-xs font-medium text-[#657b55] transition hover:bg-[#e8eddf] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#456e3c] dark:text-[#bbcea6] dark:hover:bg-white/10"
      >
        {copied ? <Check size={14} aria-hidden="true" /> : <Copy size={14} aria-hidden="true" />}
        <span>{copied ? "Copied" : "Copy message"}</span>
      </button>
      <span role="status" aria-live="polite" className="sr-only">{copied ? "Message copied." : ""}</span>
      <dialog
        ref={dialogRef}
        aria-labelledby={titleId}
        aria-describedby={helpId}
        onCancel={(event) => { event.preventDefault(); closeManual(); }}
        onClose={closeManual}
        className="m-auto max-h-[calc(var(--chat-viewport-height,100dvh)-2rem)] w-[calc(100%-1.5rem)] max-w-xl overflow-y-auto rounded-3xl border border-[#8fa58c]/30 bg-[#fcfbf7] p-5 text-[#28382b] shadow-2xl backdrop:bg-black/40 dark:bg-[#202820] dark:text-[#f4f2eb]"
      >
        {manual && <>
          <div className="flex items-center justify-between gap-3">
            <h2 id={titleId} className="text-lg font-semibold">Copy message manually</h2>
            <button type="button" aria-label="Close manual copy" onClick={closeManual} className="flex min-h-11 min-w-11 shrink-0 items-center justify-center rounded-xl hover:bg-black/5 dark:hover:bg-white/10"><X size={20} aria-hidden="true" /></button>
          </div>
          <p id={helpId} role="status" className="my-3 text-sm">Automatic copy is unavailable. The message below is selected. Press and hold it and choose Copy, or use Ctrl+C / ⌘C.</p>
          <label htmlFor={`${titleId}-text`} className="sr-only">Message text to copy</label>
          <textarea ref={textRef} id={`${titleId}-text`} readOnly value={body} rows={6} className="max-h-[35dvh] min-h-28 w-full resize-none rounded-xl border border-[#8fa58c]/40 bg-transparent p-3 text-base leading-6" />
          <button type="button" onClick={() => { textRef.current?.focus({ preventScroll: true }); textRef.current?.select(); textRef.current?.setSelectionRange(0, body.length); }} className="mt-3 inline-flex min-h-11 items-center justify-center rounded-xl border border-[#8fa58c]/40 px-4 py-2 text-sm font-semibold hover:bg-[#e4eadd] dark:hover:bg-white/10">Select message</button>
        </>}
      </dialog>
    </>
  );
}
