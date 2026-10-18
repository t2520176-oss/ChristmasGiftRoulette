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
    (async () => {
      let foregroundChat = false;
      try {
        const windows = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
        for (const client of windows) {
          try {
            const url = new URL(client.url);
            if (
              url.origin === self.location.origin &&
              (url.pathname === "/chat" || url.pathname === "/chat/") &&
              client.visibilityState === "visible" &&
              client.focused === true
            ) {
              // Keep the notification visible; the active chat handles its own sound.
              foregroundChat = true;
              break;
            }
          } catch {
            // Ignore malformed clients and continue looking for an active chat.
          }
        }
      } catch {
        // A client lookup failure must not lose a background message alert.
      }

      await self.registration.showNotification("OurChat", {
        body: "You have a new message.",
        icon: "/icons/icon-192.png",
        badge: "/icons/badge-96.png",
        tag: "ourchat-new-message",
        renotify: true,
        // The operating system still controls sound, volume, and Focus modes.
        silent: foregroundChat,
      });
    })(),
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
