import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: true,
  use: { baseURL: process.env.OURCHAT_E2E_BASE_URL || "http://127.0.0.1:3000", trace: process.env.OURCHAT_E2E_BASE_URL ? "off" : "retain-on-failure",
    launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH } : {},
  },
  projects: [ { name: "desktop-chromium", use: { ...devices["Desktop Chrome"] } }, { name: "mobile-chromium", use: { ...devices["Pixel 7"] } } ],
  webServer: process.env.OURCHAT_E2E_BASE_URL ? undefined : { command: "npm run dev", url: "http://127.0.0.1:3000/login", reuseExistingServer: !process.env.CI, timeout: 120_000,
    env: { NEXT_TELEMETRY_DISABLED: "1", NEXT_PUBLIC_SUPABASE_URL: "", NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY: "", NEXT_PUBLIC_SITE_URL: "http://127.0.0.1:3000" } },
});
