import { randomUUID } from "node:crypto";
import { expect, test, type BrowserContext, type Page } from "@playwright/test";

const credentials = {
  baseURL: process.env.OURCHAT_E2E_BASE_URL ?? "",
  husbandEmail: process.env.OURCHAT_E2E_HUSBAND_EMAIL ?? "",
  husbandPassword: process.env.OURCHAT_E2E_HUSBAND_PASSWORD ?? "",
  wifeEmail: process.env.OURCHAT_E2E_WIFE_EMAIL ?? "",
  wifePassword: process.env.OURCHAT_E2E_WIFE_PASSWORD ?? "",
};
const configured = Object.values(credentials).every((value) => value.length > 0);
test.use({ trace: "off", screenshot: "off", video: "off" });

function trustedOrigin(): string {
  try {
    const url = new URL(credentials.baseURL);
    if (url.protocol === "https:" && !url.username && !url.password &&
      url.pathname === "/" && !url.search && !url.hash) return url.origin;
  } catch { /* Fail below without printing the configured URL or credentials. */ }
  throw new Error("Live browser checks require an HTTPS application origin without a path or embedded credentials.");
}

async function signIn(page: Page, email: string, password: string) {
  // Suppress the original action error: a failed fill can include its argument
  // in Playwright's call log. Never attach secrets, browser traces, or screenshots.
  try {
    await page.goto("/login");
    await page.getByLabel("Email", { exact: true }).fill(email);
    await page.getByLabel("Password", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await page.waitForURL("**/chat", { timeout: 30_000 });
    await page.getByRole("textbox", { name: "Type a message", exact: true }).waitFor();
  } catch {
    throw new Error("Live member sign-in failed. Check the supplied account, member seed, and deployment configuration privately.");
  }
}

function savedBubble(page: Page, body: string) {
  return page.locator("[data-message-id]").filter({ has: page.getByText(body, { exact: true }) });
}

async function send(page: Page, body: string) {
  await page.getByRole("textbox", { name: "Type a message", exact: true }).fill(body);
  await page.getByRole("button", { name: "Send message", exact: true }).click();
  await expect(savedBubble(page, body)).toHaveCount(1, { timeout: 30_000 });
}

test.describe("optional hosted two-member integration", () => {
  test.describe.configure({ mode: "serial" });
  test.skip(!configured, "Requires all five OURCHAT_E2E_* live environment variables; no credentials are fabricated.");
  test.beforeEach(async ({}, testInfo) => {
    // One run adds only three immutable messages, rather than repeating the
    // credentialed flow for every viewport in the normal smoke-test matrix.
    test.skip(testInfo.project.name !== "desktop-chromium", "Run the hosted account checks once with desktop-chromium.");
  });

  test("two accounts exchange, read, deduplicate, persist, and reconnect", async ({ browser }) => {
    test.setTimeout(150_000);
    const origin = trustedOrigin();
    const contexts: BrowserContext[] = [];
    try {
      const husbandContext = await browser.newContext({ baseURL: origin });
      contexts.push(husbandContext);
      const wifeContext = await browser.newContext({ baseURL: origin });
      contexts.push(wifeContext);
      const husband = await husbandContext.newPage();
      const wife = await wifeContext.newPage();
      await signIn(husband, credentials.husbandEmail, credentials.husbandPassword);
      await signIn(wife, credentials.wifeEmail, credentials.wifePassword);
      // Do not let an initial history fetch masquerade as a connected channel.
      await expect(husband.getByRole("status").filter({ hasText: /^(Online|Away)$/ })).toBeVisible({ timeout: 30_000 });
      await expect(wife.getByRole("status").filter({ hasText: /^(Online|Away)$/ })).toBeVisible({ timeout: 30_000 });

      const token = randomUUID();
      const first = `OurChat integration ${token}: 안녕하세요 👋`;
      await send(husband, first);
      await wife.bringToFront();
      await expect(savedBubble(wife, first)).toHaveCount(1, { timeout: 30_000 });
      await savedBubble(wife, first).scrollIntoViewIfNeeded();
      await expect(savedBubble(husband, first).getByText("Read", { exact: true })).toBeVisible({ timeout: 30_000 });

      await wife.reload();
      await expect(savedBubble(wife, first)).toHaveCount(1, { timeout: 30_000 });

      const duplicateBody = `OurChat integration ${token}: idempotent retry`;
      const duplicate = await husband.evaluate(async ({ body, nonce }) => {
        return Promise.all([0, 1].map(async () => {
          const response = await fetch("/api/messages", {
            method: "POST", credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ body, client_nonce: nonce }),
          });
          const payload = await response.json().catch(() => null) as { message?: { id?: unknown } } | null;
          return { status: response.status, id: payload?.message?.id };
        }));
      }, { body: duplicateBody, nonce: randomUUID() });
      expect(duplicate.every((result) => result.status === 200 || result.status === 201)).toBe(true);
      expect(typeof duplicate[0].id).toBe("string");
      expect(duplicate[0].id).toBe(duplicate[1].id);
      await expect(savedBubble(husband, duplicateBody)).toHaveCount(1, { timeout: 30_000 });
      await expect(savedBubble(wife, duplicateBody)).toHaveCount(1, { timeout: 30_000 });

      const draft = `Unsent integration draft ${token}: 보관 📝`;
      await wife.getByRole("textbox", { name: "Type a message", exact: true }).fill(draft);
      await wifeContext.setOffline(true);
      await expect(wife.getByText("Offline. Your draft stays here. Reconnect to send.", { exact: true })).toBeVisible();
      await expect(wife.getByRole("button", { name: "Send message", exact: true })).toBeDisabled();
      await expect(wife.getByRole("textbox", { name: "Type a message", exact: true })).toHaveValue(draft);

      const missed = `OurChat integration ${token}: reconnect catch-up`;
      await send(husband, missed);
      await expect(savedBubble(wife, missed)).toHaveCount(0);
      await wifeContext.setOffline(false);
      await wife.bringToFront();
      await expect(savedBubble(wife, missed)).toHaveCount(1, { timeout: 30_000 });
      await expect(wife.getByRole("textbox", { name: "Type a message", exact: true })).toHaveValue(draft);
      await expect(wife.getByRole("button", { name: "Send message", exact: true })).toBeEnabled();
      await expect(savedBubble(wife, first)).toHaveCount(1);
      await expect(savedBubble(wife, duplicateBody)).toHaveCount(1);
    } finally {
      await Promise.allSettled(contexts.map((context) => context.close()));
    }
  });

  test("signed-out hosted API access is denied", async ({ request }) => {
    const origin = trustedOrigin();
    const history = await request.get(`${origin}/api/messages`);
    expect(history.status()).toBe(401);
    expect(history.headers()["cache-control"]).toContain("no-store");
    const insert = await request.post(`${origin}/api/messages`, {
      headers: { origin },
      data: { body: "Unstored anonymous integration check", client_nonce: randomUUID() },
    });
    expect(insert.status()).toBe(401);
  });
});
