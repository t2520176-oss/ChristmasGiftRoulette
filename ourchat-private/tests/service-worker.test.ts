import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { describe, expect, it, vi } from "vitest";

const source = readFileSync(new URL("../public/sw.js", import.meta.url), "utf8");

type WindowClient = {
  url: string;
  focus?: ReturnType<typeof vi.fn>;
  visibilityState?: "visible" | "hidden";
  focused?: boolean;
};

function worker(windows: WindowClient[] = []) {
  const listeners = new Map<string, (event: Record<string, unknown>) => void>();
  const showNotification = vi.fn().mockResolvedValue(undefined);
  const openWindow = vi.fn().mockResolvedValue(undefined);
  const claim = vi.fn().mockResolvedValue(undefined);
  const skipWaiting = vi.fn().mockResolvedValue(undefined);
  const matchAll = vi.fn().mockResolvedValue(windows);
  const cacheOpen = vi.fn(() => {
    throw new Error("Private content must never be cached");
  });
  runInNewContext(source, {
    URL,
    caches: { open: cacheOpen },
    self: {
      location: { origin: "https://ourchat.example" },
      addEventListener: (name: string, handler: (event: Record<string, unknown>) => void) =>
        listeners.set(name, handler),
      registration: { showNotification },
      clients: { matchAll, openWindow, claim },
      skipWaiting,
    },
  });
  async function dispatch(name: string, extra: Record<string, unknown> = {}) {
    const promises: Promise<unknown>[] = [];
    const handler = listeners.get(name);
    expect(handler).toBeTypeOf("function");
    handler!({ ...extra, waitUntil: (promise: Promise<unknown>) => promises.push(promise) });
    await Promise.all(promises);
  }
  return { dispatch, listeners, showNotification, openWindow, claim, skipWaiting, matchAll, cacheOpen };
}

describe("OurChat service worker", () => {
  it("never reads or displays untrusted push payloads", async () => {
    const sw = worker();
    const json = vi.fn(() => ({ title: "Private name", body: "Secret message", url: "https://evil.example" }));
    await sw.dispatch("push", { data: { json } });
    expect(json).not.toHaveBeenCalled();
    expect(sw.showNotification).toHaveBeenCalledExactlyOnceWith("OurChat", {
      body: "You have a new message.",
      icon: "/icons/icon-192.png",
      badge: "/icons/badge-96.png",
      tag: "ourchat-new-message",
      renotify: true,
      silent: false,
    });
  });

  it("handles empty push events with the same private notification", async () => {
    const sw = worker();
    await sw.dispatch("push", { data: null });
    expect(sw.showNotification).toHaveBeenCalledWith(
      "OurChat",
      expect.objectContaining({ body: "You have a new message." }),
    );
  });

  it.each(["/chat", "/chat/?from=home"])("keeps a visible notification but leaves sound to a focused chat at %s", async (path) => {
    const sw = worker([{ url: `https://ourchat.example${path}`, visibilityState: "visible", focused: true }]);
    await sw.dispatch("push");
    expect(sw.matchAll).toHaveBeenCalledExactlyOnceWith({ type: "window", includeUncontrolled: true });
    expect(sw.showNotification).toHaveBeenCalledExactlyOnceWith("OurChat", expect.objectContaining({
      body: "You have a new message.", silent: true, renotify: true,
    }));
  });

  it.each([
    { name: "hidden chat", url: "https://ourchat.example/chat", visibilityState: "hidden", focused: true },
    { name: "unfocused chat", url: "https://ourchat.example/chat", visibilityState: "visible", focused: false },
    { name: "unknown focus", url: "https://ourchat.example/chat", visibilityState: "visible" },
    { name: "unknown visibility", url: "https://ourchat.example/chat", focused: true },
    { name: "foreign chat", url: "https://evil.example/chat", visibilityState: "visible", focused: true },
    { name: "login page", url: "https://ourchat.example/login", visibilityState: "visible", focused: true },
    { name: "different path", url: "https://ourchat.example/chatroom", visibilityState: "visible", focused: true },
    { name: "malformed client", url: "not a valid URL", visibilityState: "visible", focused: true },
  ])("keeps the background alert for $name", async ({ url, visibilityState, focused }) => {
    const sw = worker([{ url, visibilityState: visibilityState as WindowClient["visibilityState"], focused }]);
    await sw.dispatch("push");
    expect(sw.showNotification).toHaveBeenCalledExactlyOnceWith("OurChat", expect.objectContaining({ silent: false }));
  });

  it("still finds a focused chat after an unrelated malformed client", async () => {
    const sw = worker([
      { url: "not a valid URL", visibilityState: "visible", focused: true },
      { url: "https://ourchat.example/chat", visibilityState: "visible", focused: true },
    ]);
    await sw.dispatch("push");
    expect(sw.showNotification).toHaveBeenCalledExactlyOnceWith("OurChat", expect.objectContaining({ silent: true }));
  });

  it("shows the background alert when the client lookup fails", async () => {
    const sw = worker();
    sw.matchAll.mockRejectedValue(new Error("Client lookup unavailable"));
    await sw.dispatch("push");
    expect(sw.showNotification).toHaveBeenCalledExactlyOnceWith("OurChat", expect.objectContaining({ silent: false }));
  });

  it("allows the operating system to alert again for repeated private notifications", async () => {
    const sw = worker();
    await sw.dispatch("push");
    await sw.dispatch("push");
    expect(sw.showNotification).toHaveBeenCalledTimes(2);
    for (const [title, options] of sw.showNotification.mock.calls) {
      expect(title).toBe("OurChat");
      expect(options).toEqual({
        body: "You have a new message.",
        icon: "/icons/icon-192.png",
        badge: "/icons/badge-96.png",
        tag: "ourchat-new-message",
        renotify: true,
        silent: false,
      });
      expect(options).not.toHaveProperty("sound");
    }
  });

  it("focuses only an existing same-origin chat window", async () => {
    const foreignFocus = vi.fn();
    const loginFocus = vi.fn();
    const chatFocus = vi.fn().mockResolvedValue(undefined);
    const close = vi.fn();
    const sw = worker([
      { url: "https://evil.example/chat", focus: foreignFocus },
      { url: "https://ourchat.example/login", focus: loginFocus },
      { url: "https://ourchat.example/chat?from=home", focus: chatFocus },
    ]);
    await sw.dispatch("notificationclick", {
      notification: { close, data: { url: "https://evil.example" } },
    });
    expect(close).toHaveBeenCalledOnce();
    expect(chatFocus).toHaveBeenCalledOnce();
    expect(foreignFocus).not.toHaveBeenCalled();
    expect(loginFocus).not.toHaveBeenCalled();
    expect(sw.openWindow).not.toHaveBeenCalled();
    expect(sw.matchAll).toHaveBeenCalledWith({ type: "window", includeUncontrolled: true });
  });

  it("opens fixed /chat even when notification data supplies a malicious URL", async () => {
    const sw = worker([{ url: "https://ourchat.example/chatroom", focus: vi.fn() }]);
    await sw.dispatch("notificationclick", {
      notification: { close: vi.fn(), data: { url: "javascript:alert(1)" } },
    });
    expect(sw.openWindow).toHaveBeenCalledExactlyOnceWith("https://ourchat.example/chat");
  });

  it("opens /chat when an existing window cannot be focused", async () => {
    const sw = worker([
      { url: "https://ourchat.example/chat", focus: vi.fn().mockRejectedValue(new Error("Closed")) },
    ]);
    await sw.dispatch("notificationclick", { notification: { close: vi.fn() } });
    expect(sw.openWindow).toHaveBeenCalledWith("https://ourchat.example/chat");
  });

  it("activates without a fetch handler or any Cache Storage access", async () => {
    const sw = worker();
    await sw.dispatch("install");
    await sw.dispatch("activate");
    expect(sw.skipWaiting).toHaveBeenCalledOnce();
    expect(sw.claim).toHaveBeenCalledOnce();
    expect(sw.listeners.has("fetch")).toBe(false);
    expect(sw.cacheOpen).not.toHaveBeenCalled();
    expect(source).not.toMatch(/caches\s*\.|addEventListener\s*\(\s*["']fetch["']/);
  });
});
