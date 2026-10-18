import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { describe, expect, it, vi } from "vitest";

const source = readFileSync(new URL("../public/sw.js", import.meta.url), "utf8");

function worker(windows: Array<{ url: string; focus: ReturnType<typeof vi.fn> }> = []) {
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
