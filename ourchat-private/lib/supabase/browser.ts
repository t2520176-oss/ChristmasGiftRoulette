"use client";
import { createBrowserClient } from "@supabase/ssr";
import { publicConfig } from "@/lib/config";

let client: ReturnType<typeof createBrowserClient> | undefined;
export function createClient() {
  const config = publicConfig();
  if (!config) throw new Error("OurChat is not configured yet. Please contact the administrator.");
  return client ??= createBrowserClient(config.url, config.key);
}
