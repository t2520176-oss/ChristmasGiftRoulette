import { test, expect } from "@playwright/test";
test.skip(Boolean(process.env.OURCHAT_E2E_BASE_URL), "Local unconfigured-preview checks only.");
test("English setup screen remains usable without credentials and offers no signup", async ({ page }) => {
  await page.goto("/login");
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
  await expect(page.getByRole("heading", { name: "Sign in", exact: true })).toBeVisible();
  await expect(page.getByText("Setup required", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign in", exact: true })).toBeDisabled();
  await expect(page.getByRole("link", { name: /sign up|register/i })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});
test("private APIs fail closed and return no cacheable conversation without setup", async ({ request }) => {
  const response = await request.get("/api/messages");
  expect([401, 503]).toContain(response.status());
  expect(response.headers()["cache-control"]).toContain("no-store");
  expect(await response.text()).not.toContain("client_nonce");
  const mutation = await request.post("/api/messages", { headers: { origin: "https://evil.example" }, data: { body: "forged", client_nonce: "bad" } });
  expect(mutation.status()).toBe(403);
});
test("manifest, real install icons and privacy-preserving service worker are served", async ({ request }) => {
  const response = await request.get("/manifest.webmanifest");
  const manifest = await response.json();
  expect(manifest).toMatchObject({ name: "OurChat", start_url: "/chat", display: "standalone", lang: "en" });
  for (const icon of manifest.icons) expect((await request.get(icon.src)).ok()).toBe(true);
  const worker = await request.get("/sw.js");
  expect(worker.headers()["cache-control"]).toContain("no-store");
  expect(await worker.text()).toContain("You have a new message.");
});
