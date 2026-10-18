/* OurChat deliberately keeps private pages and API responses out of Cache Storage. */
"use strict";

self.addEventListener("install", (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("push", (event) => {
  // The payload is untrusted. Never display a message preview or a supplied URL.
  event.waitUntil(
    self.registration.showNotification("OurChat", {
      body: "You have a new message.",
      icon: "/icons/icon-192.png",
      badge: "/icons/badge-96.png",
      tag: "ourchat-new-message",
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const chatUrl = new URL("/chat", self.location.origin);

  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true,
      });

      for (const client of windows) {
        try {
          const url = new URL(client.url);
          if (
            url.origin === chatUrl.origin &&
            (url.pathname === "/chat" || url.pathname === "/chat/") &&
            typeof client.focus === "function"
          ) {
            await client.focus();
            return;
          }
        } catch {
          // A malformed or closed client must not prevent opening OurChat.
        }
      }

      await self.clients.openWindow(chatUrl.href);
    })(),
  );
});
