"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";

type InstallPrompt = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
};

type Availability = "checking" | "available" | "ios-install" | "unavailable";
type BrowserSnapshot =
  | "checking"
  | "ios-install"
  | "available"
  | "unavailable"
  | "installed-available"
  | "installed-unavailable";

function standalone(): boolean {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

function isAppleMobile(): boolean {
  return (
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1)
  );
}

function browserSnapshot(): BrowserSnapshot {
  const installed = standalone();
  if (isAppleMobile() && !installed) return "ios-install";
  const supported =
    window.isSecureContext &&
    "serviceWorker" in navigator &&
    "PushManager" in window &&
    "Notification" in window;
  if (installed) return supported ? "installed-available" : "installed-unavailable";
  return supported ? "available" : "unavailable";
}

function subscribeBrowser(onChange: () => void): () => void {
  const media = window.matchMedia("(display-mode: standalone)");
  media.addEventListener("change", onChange);
  window.addEventListener("appinstalled", onChange);
  window.addEventListener("pageshow", onChange);
  return () => {
    media.removeEventListener("change", onChange);
    window.removeEventListener("appinstalled", onChange);
    window.removeEventListener("pageshow", onChange);
  };
}

function serverBrowserSnapshot(): BrowserSnapshot {
  return "checking";
}

function applicationServerKey(value: string): Uint8Array<ArrayBuffer> {
  if (!/^[A-Za-z0-9_-]+$/.test(value)) throw new Error("Invalid public key");
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const decoded = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "="));
  const key = new Uint8Array(new ArrayBuffer(decoded.length));
  for (let index = 0; index < decoded.length; index += 1) key[index] = decoded.charCodeAt(index);
  if (key.length !== 65 || key[0] !== 4) throw new Error("Invalid public key");
  return key;
}

/** Remove only this browser's server subscription while its auth cookie is still valid. */
export async function cleanupCurrentPushSubscription(): Promise<void> {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) return;
  try {
    const registration = await navigator.serviceWorker.getRegistration("/");
    if (!registration || !("pushManager" in registration)) return;
    const subscription = await registration.pushManager.getSubscription();
    if (!subscription) return;
    const response = await fetch("/api/push/subscribe", {
      method: "DELETE",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ endpoint: subscription.endpoint }),
    });
    if (!response.ok) throw new Error("Subscription not removed");
    try {
      await subscription.unsubscribe();
    } catch {
      // The server no longer has this endpoint. Browser cleanup must not block sign-out.
    }
  } catch {
    throw new Error("This device's notifications could not be removed. Check your connection and try signing out again.");
  }
}

/** Mount this only inside the authenticated members' chat screen. */
export function PwaControls() {
  const registrationRef = useRef<Promise<ServiceWorkerRegistration> | null>(null);
  const browser = useSyncExternalStore(subscribeBrowser, browserSnapshot, serverBrowserSnapshot);
  const availability: Availability =
    browser === "installed-available"
      ? "available"
      : browser === "installed-unavailable"
        ? "unavailable"
        : browser;
  const [installPrompt, setInstallPrompt] = useState<InstallPrompt | null>(null);
  const [installationAccepted, setInstallationAccepted] = useState(false);
  const installed = installationAccepted || browser.startsWith("installed-");
  const [hasSubscription, setHasSubscription] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    const capturePrompt = (event: Event) => {
      event.preventDefault();
      if (mounted) setInstallPrompt(event as InstallPrompt);
    };
    const handleInstalled = () => {
      if (!mounted) return;
      setInstallationAccepted(true);
      setInstallPrompt(null);
    };
    window.addEventListener("beforeinstallprompt", capturePrompt);
    window.addEventListener("appinstalled", handleInstalled);
    if (window.isSecureContext && "serviceWorker" in navigator) {
      registrationRef.current = navigator.serviceWorker
        .register("/sw.js", { scope: "/", updateViaCache: "none" })
        .then(() => navigator.serviceWorker.ready);
      void registrationRef.current
        .then(async (registration) => {
          if (!("pushManager" in registration)) return;
          const subscription = await registration.pushManager.getSubscription();
          if (mounted) setHasSubscription(Boolean(subscription));
        })
        .catch(() => {
          if (mounted) setError("Notifications could not be prepared. Please refresh and try again.");
        });
    }

    return () => {
      mounted = false;
      window.removeEventListener("beforeinstallprompt", capturePrompt);
      window.removeEventListener("appinstalled", handleInstalled);
    };
  }, []);

  async function enableNotifications() {
    if (busy || availability !== "available") return;
    setError("");
    setStatus("");

    let key: Uint8Array<ArrayBuffer>;
    try {
      key = applicationServerKey(process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY ?? "");
    } catch {
      setError("Notifications are not configured yet. Please ask the app administrator.");
      return;
    }

    // Invoke the browser permission request directly in the button's user gesture.
    let permissionRequest: Promise<NotificationPermission>;
    try {
      permissionRequest = Notification.requestPermission();
    } catch {
      setError("This browser could not request notifications. Please check its notification settings.");
      return;
    }
    setBusy(true);
    let newSubscription: PushSubscription | null = null;
    try {
      const permission = await permissionRequest;
      if (permission !== "granted") {
        setError(
          permission === "denied"
            ? "Notifications are blocked. Allow them in your browser or device settings, then try again."
            : "Notifications were not enabled. You can try again whenever you are ready.",
        );
        return;
      }

      const registration = await registrationRef.current;
      if (!registration) throw new Error("Service worker unavailable");
      let subscription = await registration.pushManager.getSubscription();
      if (!subscription) {
        subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: key,
        });
        newSubscription = subscription;
      }
      const response = await fetch("/api/push/subscribe", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(subscription.toJSON()),
      });
      if (!response.ok) throw new Error("Subscription not saved");
      setHasSubscription(true);
      setConfirmed(true);
      setStatus("Notifications enabled on this device.");
    } catch {
      if (newSubscription) {
        try {
          await newSubscription.unsubscribe();
        } catch {
          // A later Enable action will reconcile any remaining browser subscription.
        }
      }
      setError("Notifications could not be enabled. Check your connection and try again.");
    } finally {
      setBusy(false);
    }
  }

  async function disableNotifications() {
    if (busy || availability !== "available") return;
    setBusy(true);
    setError("");
    setStatus("");
    try {
      const registration = await registrationRef.current;
      if (!registration) throw new Error("Service worker unavailable");
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        // Keep the browser subscription available for a retry if server deletion fails.
        const response = await fetch("/api/push/subscribe", {
          method: "DELETE",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ endpoint: subscription.endpoint }),
        });
        if (!response.ok) throw new Error("Subscription not removed");
        const unsubscribed = await subscription.unsubscribe();
        if (!unsubscribed) throw new Error("Browser subscription not removed");
      }
      setHasSubscription(false);
      setConfirmed(false);
      setStatus("Notifications disabled on this device.");
    } catch {
      setError("Notifications could not be disabled. Check your connection and try again.");
    } finally {
      setBusy(false);
    }
  }

  async function install() {
    if (!installPrompt || busy) return;
    setBusy(true);
    setError("");
    try {
      await installPrompt.prompt();
      const choice = await installPrompt.userChoice;
      setStatus(
        choice.outcome === "accepted"
          ? "Installation started. You can open OurChat from your apps."
          : "You can install OurChat later from your browser menu.",
      );
      setInstallPrompt(null);
    } catch {
      setError("Installation could not start. Use your browser menu to install OurChat.");
      setInstallPrompt(null);
    } finally {
      setBusy(false);
    }
  }

  const buttonClass =
    "rounded-xl border border-current/20 px-3 py-2 text-sm font-medium transition hover:bg-black/5 focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-white/10";

  return (
    <details className="rounded-2xl border border-current/10 bg-white/60 p-3 text-sm dark:bg-slate-900/60">
      <summary className="cursor-pointer font-medium">Install &amp; notifications</summary>
      <div className="mt-3 space-y-3">
        {installed ? (
          <p>OurChat is installed on this device.</p>
        ) : (
          <p>
            On Windows or Android, use Install app in a supported browser&apos;s menu. On iPhone or iPad,
            open OurChat in Safari, tap Share, then Add to Home Screen. Open the installed app to enable
            notifications. iOS and iPadOS 16.4 or later are required for web push.
          </p>
        )}
        {installPrompt && !installed && (
          <button className={buttonClass} type="button" onClick={() => void install()} disabled={busy}>
            Install OurChat
          </button>
        )}
        {availability === "checking" && <p>Checking notification support…</p>}
        {availability === "ios-install" && (
          <p>Notifications are available from the installed Home Screen app, rather than a Safari tab.</p>
        )}
        {availability === "unavailable" && (
          <p>
            Notifications are unavailable in this browser. Use a supported browser over HTTPS, or read
            your messages here while OurChat is open.
          </p>
        )}
        {availability === "available" && (
          <>
            <p>
              Notifications show “You have a new message.” Your messages stay out of notification
              previews. Browser and device power-saving settings can delay delivery.
            </p>
            {hasSubscription && !confirmed && (
              <p>A browser subscription was found. Enable notifications to confirm this device is registered.</p>
            )}
            <div className="flex flex-wrap gap-2">
              <button
                className={buttonClass}
                type="button"
                onClick={() => void enableNotifications()}
                disabled={busy || confirmed}
              >
                {confirmed ? "Notifications enabled" : "Enable notifications"}
              </button>
              {hasSubscription && (
                <button
                  className={buttonClass}
                  type="button"
                  onClick={() => void disableNotifications()}
                  disabled={busy}
                >
                  Disable notifications
                </button>
              )}
            </div>
          </>
        )}
        {busy && <p role="status">Updating this device…</p>}
        {status && <p role="status">{status}</p>}
        {error && <p role="alert">{error}</p>}
      </div>
    </details>
  );
}
