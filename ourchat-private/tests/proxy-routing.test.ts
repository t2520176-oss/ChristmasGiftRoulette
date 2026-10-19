import { describe, expect, it } from "vitest";
import { unstable_doesMiddlewareMatch as doesProxyMatch } from "next/experimental/testing/server";
import { config } from "@/proxy";

describe("Android website association routing", () => {
  it.each(["/.well-known/assetlinks.json", "/.well-known/assetlinks.json?check=1"])(
    "serves the public association without session processing: %s", (url) => {
      expect(doesProxyMatch({ config, nextConfig: {}, url })).toBe(false);
    },
  );

  it.each([
    "/chat", "/login", "/api/messages", "/api/read", "/api/push/subscribe",
    "/.well-known/assetlinks.json/extra", "/.well-known/assetlinks.jsonx",
  ])("keeps session processing for app routes and similar paths: %s", (url) => {
    expect(doesProxyMatch({ config, nextConfig: {}, url })).toBe(true);
  });
});
