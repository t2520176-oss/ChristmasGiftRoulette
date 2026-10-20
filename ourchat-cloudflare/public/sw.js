"use strict";
const SHELL_CACHE = "ourchat-shell-20261020-photos-1";
const SHELL = ["/", "/index.html", "/styles.css?v=20261020-photos-1", "/app.js?v=20261020-photos-1", "/manifest.webmanifest", "/icons/icon-192.png", "/icons/icon-512.png", "/icons/icon-maskable-512.png", "/icons/apple-touch-icon.png", "/icons/badge-96.png"];
self.addEventListener("install", (event) => {
  event.waitUntil((async () => { const cache = await caches.open(SHELL_CACHE); await cache.addAll(SHELL); await self.skipWaiting(); })());
});
self.addEventListener("activate", (event) => {
  event.waitUntil((async () => {
    for (const key of await caches.keys()) if (key.startsWith("ourchat-shell-") && key !== SHELL_CACHE) await caches.delete(key);
    await self.clients.claim();
  })());
});
self.addEventListener("fetch", (event) => {
  const request = event.request, url = new URL(request.url);
  // API responses and private conversation data never enter Cache Storage.
  if (request.method !== "GET" || url.origin !== self.location.origin || url.pathname.startsWith("/api/")) return;
  if (!SHELL.includes(url.pathname + url.search) && !(request.mode === "navigate" && ["/", "/index.html"].includes(url.pathname))) return;
  event.respondWith((async () => {
    try {
      const response = await fetch(request);
      if (response.ok && !response.headers.get("Content-Type")?.includes("application/json") && !response.headers.has("Set-Cookie")) {
        const cache = await caches.open(SHELL_CACHE); await cache.put(request, response.clone());
      }
      return response;
    } catch {
      return (await caches.match(request)) || (request.mode === "navigate" ? await caches.match("/") : undefined) || new Response("Offline", { status: 503 });
    }
  })());
});
self.addEventListener("push", (event) => {
  event.waitUntil((async () => {
    // Never use notification titles, message bodies or URLs supplied in a payload.
    let foreground = false;
    try {
      const windows = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
      foreground = windows.some((client) => {
        try { return new URL(client.url).origin === self.location.origin && client.visibilityState === "visible" && client.focused; }
        catch { return false; }
      });
    } catch { /* A failed window lookup must never discard a background alert. */ }
    await self.registration.showNotification("OurChat", {
      body: "You have a new message.", icon: "/icons/icon-192.png", badge: "/icons/badge-96.png", tag: "ourchat-new-message", renotify: true,
      // Push subscriptions require a visible notification. The focused chat owns its opt-in sound.
      silent: foreground,
    });
  })());
});
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil((async () => {
    try {
      const windows = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
      for (const client of windows) {
        try { if (new URL(client.url).origin === self.location.origin && "focus" in client) { await client.focus(); return; } }
        catch { /* Continue if a tab was closed or its URL was malformed. */ }
      }
    } catch { /* Opening the fixed same-origin address below is still available. */ }
    await self.clients.openWindow(new URL("/", self.location.origin).href);
  })());
});
