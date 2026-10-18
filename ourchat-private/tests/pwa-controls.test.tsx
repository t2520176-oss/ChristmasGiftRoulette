// @vitest-environment jsdom

import { createElement } from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanupCurrentPushSubscription, PwaControls } from "@/components/pwa-controls";

const endpoint = "https://fcm.googleapis.com/fcm/send/device-subscription";
const subscriptionJson = { endpoint, expirationTime: null, keys: { p256dh: "public-key", auth: "auth-key" } };
const validVapidKey = Buffer.from([4, ...new Array<number>(64).fill(1)]).toString("base64url");
const originalServiceWorker = Object.getOwnPropertyDescriptor(navigator, "serviceWorker");

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise) => { resolve = resolvePromise; });
  return { promise, resolve };
}

function setupBrowser(existingSubscription = false) {
  const unsubscribe = vi.fn().mockResolvedValue(true);
  const subscription = { endpoint, unsubscribe, toJSON: () => subscriptionJson };
  const getSubscription = vi.fn().mockResolvedValue(existingSubscription ? subscription : null);
  const subscribe = vi.fn().mockResolvedValue(subscription);
  const registration = { pushManager: { getSubscription, subscribe } };
  const serviceWorker = {
    register: vi.fn().mockResolvedValue(registration),
    ready: Promise.resolve(registration),
    getRegistration: vi.fn().mockResolvedValue(registration),
  };
  Object.defineProperty(navigator, "serviceWorker", { configurable: true, value: serviceWorker });
  const requestPermission = vi.fn().mockResolvedValue("granted");
  vi.stubGlobal("Notification", { requestPermission });
  vi.stubGlobal("PushManager", function PushManager() {});
  vi.stubGlobal("isSecureContext", true);
  vi.stubGlobal("matchMedia", vi.fn().mockImplementation(() => ({
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })));
  const fetchMock = vi.fn().mockResolvedValue({ ok: true });
  vi.stubGlobal("fetch", fetchMock);
  vi.stubEnv("NEXT_PUBLIC_VAPID_PUBLIC_KEY", validVapidKey);
  return { subscription, unsubscribe, getSubscription, subscribe, registration, serviceWorker, requestPermission, fetchMock };
}

async function showControls() {
  render(createElement(PwaControls));
  fireEvent.click(screen.getByText("Install & notifications"));
  return screen.findByRole("button", { name: "Enable notifications" });
}

beforeEach(() => { vi.clearAllMocks(); });
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  if (originalServiceWorker) Object.defineProperty(navigator, "serviceWorker", originalServiceWorker);
  else Reflect.deleteProperty(navigator, "serviceWorker");
});

describe("PWA notification consent", () => {
  it("explains the iPhone Home Screen requirement without prompting from a Safari tab", async () => {
    const browser = setupBrowser();
    vi.spyOn(navigator, "userAgent", "get").mockReturnValue(
      "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
    );
    render(createElement(PwaControls));
    fireEvent.click(screen.getByText("Install & notifications"));
    await screen.findByText(/Notifications are available from the installed Home Screen app/);
    expect(screen.getByText(/iOS and iPadOS 16.4 or later/)).toBeDefined();
    expect(screen.queryByRole("button", { name: "Enable notifications" })).toBeNull();
    expect(browser.requestPermission).not.toHaveBeenCalled();
  });

  it("keeps unsupported browsers usable without a notification action", async () => {
    const browser = setupBrowser();
    Reflect.deleteProperty(window, "PushManager");
    render(createElement(PwaControls));
    fireEvent.click(screen.getByText("Install & notifications"));
    await screen.findByText(/Notifications are unavailable in this browser/);
    expect(screen.queryByRole("button", { name: "Enable notifications" })).toBeNull();
    expect(browser.requestPermission).not.toHaveBeenCalled();
  });

  it("prepares the service worker without automatically asking for permission", async () => {
    const browser = setupBrowser();
    await showControls();
    await waitFor(() => expect(browser.serviceWorker.register).toHaveBeenCalledWith(
      "/sw.js", { scope: "/", updateViaCache: "none" },
    ));
    expect(browser.requestPermission).not.toHaveBeenCalled();
    expect(browser.subscribe).not.toHaveBeenCalled();
    expect(browser.fetchMock).not.toHaveBeenCalled();
  });

  it("requests permission directly from the click before awaiting service-worker readiness", async () => {
    const browser = setupBrowser();
    const registration = deferred<typeof browser.registration>();
    browser.serviceWorker.register.mockReturnValue(registration.promise);
    const enable = await showControls();
    fireEvent.click(enable);
    expect(browser.requestPermission).toHaveBeenCalledOnce();
    expect(browser.fetchMock).not.toHaveBeenCalled();
    registration.resolve(browser.registration);
    await screen.findByText("Notifications enabled on this device.");
    expect(browser.subscribe).toHaveBeenCalledOnce();
    expect(browser.fetchMock).toHaveBeenCalledWith("/api/push/subscribe", {
      method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json" },
      body: JSON.stringify(subscriptionJson),
    });
  });

  it("re-registers an existing browser subscription only after an explicit Enable action", async () => {
    const browser = setupBrowser(true);
    const enable = await showControls();
    await screen.findByText(/A browser subscription was found/);
    expect(browser.fetchMock).not.toHaveBeenCalled();
    fireEvent.click(enable);
    await screen.findByText("Notifications enabled on this device.");
    expect(browser.subscribe).not.toHaveBeenCalled();
    expect(browser.fetchMock).toHaveBeenCalledOnce();
  });

  it("explains missing configuration without showing a permission prompt", async () => {
    const browser = setupBrowser();
    vi.stubEnv("NEXT_PUBLIC_VAPID_PUBLIC_KEY", "");
    fireEvent.click(await showControls());
    expect(screen.getByRole("alert").textContent).toContain("Notifications are not configured yet");
    expect(browser.requestPermission).not.toHaveBeenCalled();
    expect(browser.fetchMock).not.toHaveBeenCalled();
  });

  it("explains a permission denial without creating a subscription", async () => {
    const browser = setupBrowser();
    browser.requestPermission.mockResolvedValue("denied");
    fireEvent.click(await showControls());
    await screen.findByText(/Notifications are blocked/);
    expect(browser.subscribe).not.toHaveBeenCalled();
    expect(browser.fetchMock).not.toHaveBeenCalled();
  });

  it("does not unsubscribe in the browser when server deletion fails", async () => {
    const browser = setupBrowser(true);
    browser.fetchMock.mockResolvedValue({ ok: false });
    await showControls();
    fireEvent.click(await screen.findByRole("button", { name: "Disable notifications" }));
    await screen.findByText(/Notifications could not be disabled/);
    expect(browser.unsubscribe).not.toHaveBeenCalled();
  });

  it("waits for server deletion before removing the current browser subscription", async () => {
    const browser = setupBrowser(true);
    const deleted = deferred<{ ok: boolean }>();
    browser.fetchMock.mockReturnValue(deleted.promise);
    await showControls();
    fireEvent.click(await screen.findByRole("button", { name: "Disable notifications" }));
    await waitFor(() => expect(browser.fetchMock).toHaveBeenCalled());
    expect(browser.unsubscribe).not.toHaveBeenCalled();
    deleted.resolve({ ok: true });
    await screen.findByText("Notifications disabled on this device.");
    expect(browser.unsubscribe).toHaveBeenCalledOnce();
    expect(browser.fetchMock).toHaveBeenCalledWith("/api/push/subscribe", expect.objectContaining({
      method: "DELETE", body: JSON.stringify({ endpoint }),
    }));
  });
});

describe("current-device notification cleanup before sign-out", () => {
  it("rejects when server deletion fails and preserves the browser subscription for a retry", async () => {
    const browser = setupBrowser(true);
    browser.fetchMock.mockResolvedValue({ ok: false });
    await expect(cleanupCurrentPushSubscription()).rejects.toThrow("try signing out again");
    expect(browser.unsubscribe).not.toHaveBeenCalled();
    expect(browser.requestPermission).not.toHaveBeenCalled();
  });

  it("removes only the current endpoint and allows sign-out if browser cleanup fails afterward", async () => {
    const browser = setupBrowser(true);
    browser.unsubscribe.mockRejectedValue(new Error("Browser unavailable"));
    await expect(cleanupCurrentPushSubscription()).resolves.toBeUndefined();
    expect(browser.fetchMock).toHaveBeenCalledExactlyOnceWith("/api/push/subscribe", {
      method: "DELETE", credentials: "same-origin", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ endpoint }),
    });
    expect(browser.unsubscribe).toHaveBeenCalledOnce();
    expect(browser.requestPermission).not.toHaveBeenCalled();
  });

  it("does nothing when this browser has no subscription", async () => {
    const browser = setupBrowser(false);
    await cleanupCurrentPushSubscription();
    expect(browser.fetchMock).not.toHaveBeenCalled();
    expect(browser.requestPermission).not.toHaveBeenCalled();
  });
});
