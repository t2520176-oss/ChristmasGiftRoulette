import { afterEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { ApiError, assertSameOrigin, fail, readJson } from "@/lib/server/http";
afterEach(() => vi.unstubAllEnvs());
describe("mutation request boundary", () => {
  it("requires the configured exact Origin and rejects suffix spoofs and missing origins", () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://ourchat.example");
    expect(() => assertSameOrigin(new Request("https://ourchat.example/api/messages", { headers: { origin: "https://ourchat.example" } }))).not.toThrow();
    for (const origin of ["https://ourchat.example.evil.test", "https://evil.test", "null", ""]) {
      expect(() => assertSameOrigin(new Request("https://ourchat.example/api/messages", { headers: origin ? { origin } : {} }))).toThrow(ApiError);
    }
  });
  it("rejects oversized streaming bodies even when Content-Length is absent", async () => {
    const req = new Request("https://ourchat.example/api/messages", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ body: "x".repeat(100) }) });
    await expect(readJson(req, z.object({ body: z.string() }), 32)).rejects.toMatchObject({ status: 413 });
  });
  it("rejects malformed JSON and unsupported content types", async () => {
    await expect(readJson(new Request("https://example.test", { method: "POST", headers: { "content-type": "application/json" }, body: "{" }), z.unknown())).rejects.toMatchObject({ status: 400 });
    await expect(readJson(new Request("https://example.test", { method: "POST", body: "{}" }), z.unknown())).rejects.toMatchObject({ status: 415 });
  });
  it("returns generic errors without leaking secret or database details", async () => {
    const result = fail(new Error("private key and database password"));
    expect(result.status).toBe(500);
    expect(await result.text()).not.toMatch(/private key|password/);
    expect(result.headers.get("cache-control")).toContain("no-store");
  });
});
