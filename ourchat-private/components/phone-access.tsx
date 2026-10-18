"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Copy, Share2, Smartphone, X } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { PwaControls } from "@/components/pwa-controls";

type Device = "iphone" | "android" | "other";
type InstallPrompt = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
};

function publicAddress(): string {
  const configured = process.env.NEXT_PUBLIC_SITE_URL;
  if (configured) {
    try {
      const url = new URL(configured);
      if (["https:", "http:"].includes(url.protocol) && !url.username && !url.password) {
        return url.origin;
      }
    } catch {
      // An invalid setting must never copy a login URL or session parameters.
    }
  }
  return window.location.origin;
}

function currentDevice(): Device {
  if (
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1)
  ) return "iphone";
  return /Android/i.test(navigator.userAgent) ? "android" : "other";
}

function installed(): boolean {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

/** Share only the public app address. This never includes the current path or login parameters. */
export function PhoneAccess({ authenticated = false, compact = false }: {
  authenticated?: boolean;
  compact?: boolean;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const addressRef = useRef<HTMLInputElement>(null);
  const titleId = useId();
  const descriptionId = useId();
  const [address, setAddress] = useState("");
  const [device, setDevice] = useState<Device>("other");
  const [isInstalled, setInstalled] = useState(false);
  const [canShare, setCanShare] = useState(false);
  const [status, setStatus] = useState("");
  const [copyFailed, setCopyFailed] = useState(false);
  const [installPrompt, setInstallPrompt] = useState<InstallPrompt | null>(null);
  const [installing, setInstalling] = useState(false);

  useEffect(() => {
    // Authenticated device controls already own the browser's install prompt.
    const capture = (event: Event) => {
      event.preventDefault();
      setInstallPrompt(event as InstallPrompt);
    };
    const completed = () => {
      setInstalled(true);
      setInstallPrompt(null);
    };
    if (!authenticated) window.addEventListener("beforeinstallprompt", capture);
    window.addEventListener("appinstalled", completed);
    return () => {
      if (!authenticated) window.removeEventListener("beforeinstallprompt", capture);
      window.removeEventListener("appinstalled", completed);
    };
  }, [authenticated]);

  function open() {
    setAddress(publicAddress());
    setDevice(currentDevice());
    setInstalled((current) => current || installed());
    setCanShare(typeof navigator.share === "function");
    setStatus("");
    setCopyFailed(false);
    dialogRef.current?.showModal();
  }

  async function copyAddress() {
    try {
      await navigator.clipboard.writeText(address);
      setCopyFailed(false);
      setStatus("Link copied. Send it to your phone or the other member.");
    } catch {
      setCopyFailed(true);
      setStatus("");
      addressRef.current?.focus();
      addressRef.current?.select();
    }
  }

  async function shareAddress() {
    try {
      await navigator.share({ title: "OurChat", url: address });
      setStatus("Link shared.");
    } catch (error) {
      if (typeof error === "object" && error !== null && "name" in error && error.name === "AbortError") return;
      setStatus("Sharing is unavailable. Copy the link below instead.");
    }
  }

  async function installApp() {
    if (!installPrompt || installing) return;
    setInstalling(true);
    try {
      await installPrompt.prompt();
      const choice = await installPrompt.userChoice;
      setStatus(choice.outcome === "accepted"
        ? "Installation started. Follow your browser's instructions."
        : "You can install OurChat later from your browser menu.");
    } catch {
      setStatus("Use your browser menu to install OurChat.");
    } finally {
      setInstallPrompt(null);
      setInstalling(false);
    }
  }

  const buttonClass = "inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-white/30 bg-raised px-4 py-2 text-sm font-semibold text-white transition hover:bg-highlight disabled:opacity-50";

  return (
    <>
      <button
        type="button"
        aria-label="Use on phone"
        title="Use on phone"
        onClick={open}
        className={`${buttonClass} ${compact ? "px-3" : ""}`}
      >
        <Smartphone size={19} aria-hidden="true" />
        <span className={compact ? "hidden sm:inline" : ""}>Use on phone</span>
      </button>
      <dialog
        ref={dialogRef}
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="m-auto max-h-[calc(var(--chat-viewport-height,100dvh)-2rem)] w-[calc(100%-1.5rem)] max-w-xl overflow-y-auto rounded-3xl border border-white/30 bg-panel p-0 text-white shadow-2xl backdrop:bg-black/40"
      >
        <div className="sticky top-0 z-10 flex items-start justify-between gap-3 border-b border-white/20 bg-panel px-5 py-4">
          <div>
            <h2 id={titleId} className="text-xl font-semibold">OurChat on your phone</h2>
            <p id={descriptionId} className="mt-1 text-sm text-white/80">Open the link and sign in with your own account.</p>
          </div>
          <button type="button" aria-label="Close phone guide" onClick={() => dialogRef.current?.close()} className="flex min-h-11 min-w-11 shrink-0 items-center justify-center rounded-xl hover:bg-highlight">
            <X size={21} aria-hidden="true" />
          </button>
        </div>
        <div className="space-y-5 p-5">
          {address && (
            <section aria-label="Open OurChat on your phone" className="space-y-3">
              <div className="flex flex-col items-center gap-2 rounded-2xl bg-raised p-4 text-center text-white">
                <div className="rounded-xl bg-white p-2"><QRCodeSVG value={address} size={176} level="M" marginSize={4} title="OurChat public website address" /></div>
                <p className="text-sm">Scan with your phone&apos;s camera to open OurChat.</p>
              </div>
              <label htmlFor={`${titleId}-address`} className="block text-sm font-medium">Website link</label>
              <input ref={addressRef} id={`${titleId}-address`} aria-label="OurChat website link" value={address} readOnly onFocus={(event) => event.currentTarget.select()} className="min-h-11 w-full min-w-0 rounded-xl border border-white/30 bg-field px-3 py-2 text-base text-white" />
              <div className="flex flex-wrap gap-2">
                <button type="button" onClick={() => void copyAddress()} className={buttonClass}><Copy size={17} aria-hidden="true" />Copy link</button>
                {canShare && <button type="button" onClick={() => void shareAddress()} className={buttonClass}><Share2 size={17} aria-hidden="true" />Share link</button>}
              </div>
              {copyFailed && <p role="status" className="text-sm">Copy was unavailable. The link is selected: press and hold it on your phone, or use Ctrl+C / ⌘C.</p>}
              {status && <p role="status" className="text-sm">{status}</p>}
            </section>
          )}
          <section aria-label="Add OurChat to your Home Screen" className="space-y-3 rounded-2xl border border-white/20 p-4 text-sm">
            <h3 className="font-semibold">Add an icon to your Home Screen</h3>
            {isInstalled ? <p>OurChat is installed on this device. Open it from your Home Screen.</p> : (
              <>
                {(device === "iphone" || device === "other") && <div><h4 className="font-semibold">iPhone / iPad</h4><ol className="mt-1 list-decimal space-y-1 pl-5"><li>Open this link in Safari.</li><li>In Safari&apos;s toolbar, tap Share: the square with an upward arrow. If it is hidden, open the ⋯ menu first.</li><li>Scroll to Add to Home Screen, tap Add, then open the OurChat icon.</li></ol></div>}
                {(device === "android" || device === "other") && <div><h4 className="font-semibold">Android</h4><ol className="mt-1 list-decimal space-y-1 pl-5"><li>Open this link in Chrome.</li><li>Tap the ⋮ menu, then Install app or Add to Home screen.</li><li>Confirm, then open the OurChat icon.</li></ol></div>}
                {!authenticated && installPrompt && <button type="button" onClick={() => void installApp()} disabled={installing} className={buttonClass}>{installing ? "Opening installer…" : "Install OurChat"}</button>}
                <p className="text-white/80">Sign in on each device with its member&apos;s account. Your password is never included in this link or QR code.</p>
              </>
            )}
            {device === "iphone" && <p className="text-white/80">For alerts when OurChat is closed, enable notifications from the Home Screen app. In iPhone Settings → Notifications → OurChat, turn on Sounds.</p>}
          </section>
          {authenticated ? <PwaControls defaultOpen /> : <p className="text-sm text-white/80">After signing in, open Use on phone to set up notifications. On iPhone, notifications require the Home Screen app and iOS 16.4 or later.</p>}
        </div>
      </dialog>
    </>
  );
}
