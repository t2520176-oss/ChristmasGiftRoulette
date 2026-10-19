import { cloudflareTest, readD1Migrations } from "@cloudflare/vitest-plugin";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [cloudflareTest({
      wrangler: { configPath: "./wrangler.jsonc" },
      remoteBindings: false,
      miniflare: {
        bindings: {
          MEMBER_1_EMAIL: "husband@example.test",
          MEMBER_1_PASSWORD: "local-test-husband-password-12345",
          MEMBER_2_EMAIL: "wife@example.test",
          MEMBER_2_PASSWORD: "local-test-wife-password-67890",
          TEST_MIGRATIONS: await readD1Migrations("./migrations"),
        },
      },
    })],
  test: {
    include: ["tests/**/*.test.ts"],
    fileParallelism: false,
    testTimeout: 30000,
  },
});
