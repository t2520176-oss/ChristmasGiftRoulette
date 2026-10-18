import "server-only";
import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";
import { publicConfig } from "@/lib/config";
import { ApiError } from "@/lib/server/http";

export async function createClient() {
  const config = publicConfig();
  if (!config) throw new ApiError(503, "OurChat is not configured yet. Please contact the administrator.");
  const jar = await cookies();
  return createServerClient(config.url, config.key, {
    cookies: {
      getAll: () => jar.getAll(),
      setAll: (values) => {
        try { values.forEach(({ name, value, options }) => jar.set(name, value, options)); }
        catch { /* The proxy refreshes cookies when a Server Component cannot write them. */ }
      },
    },
  });
}
