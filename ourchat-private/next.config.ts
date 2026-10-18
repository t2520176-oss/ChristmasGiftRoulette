import type { NextConfig } from "next";

const csp = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'" + (process.env.NODE_ENV === "development" ? " 'unsafe-eval'" : ""),
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self'",
  "connect-src 'self' https://*.supabase.co wss://*.supabase.co" + (process.env.NODE_ENV === "development" ? " http://127.0.0.1:* http://localhost:* ws://127.0.0.1:* ws://localhost:*" : ""),
  "worker-src 'self'",
  "manifest-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const config: NextConfig = {
  allowedDevOrigins: ["localhost", "127.0.0.1"],
  poweredByHeader: false,
  async headers() {
    return [
      { source: "/:path*", headers: [
        { key: "X-Content-Type-Options", value: "nosniff" },
        { key: "Referrer-Policy", value: "no-referrer" },
        { key: "X-Frame-Options", value: "DENY" },
        { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        { key: "Content-Security-Policy", value: csp },
      ] },
      { source: "/sw.js", headers: [
        { key: "Cache-Control", value: "no-cache, no-store, must-revalidate" },
        { key: "Service-Worker-Allowed", value: "/" },
      ] },
      { source: "/api/:path*", headers: [{ key: "Cache-Control", value: "private, no-store, max-age=0" }] },
      { source: "/chat", headers: [{ key: "Cache-Control", value: "private, no-store, max-age=0" }] },
      { source: "/login", headers: [{ key: "Cache-Control", value: "private, no-store, max-age=0" }] },
    ];
  },
};
export default config;
