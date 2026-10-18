import { createClient } from "@/lib/supabase/server";
import { ApiError, assertSameOrigin, fail, json, readJson, throwDatabaseError } from "@/lib/server/http";
import { signInSchema } from "@/lib/validation";

export const runtime = "nodejs";
export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const input = await readJson(request, signInSchema, 4096);
    const client = await createClient();
    const { data, error } = await client.auth.signInWithPassword(input);
    if (error || !data.user) {
      if (error?.status === 429) throw new ApiError(429, "Too many sign-in attempts. Please try again later.");
      throw new ApiError(401, "Invalid email or password.");
    }
    const member = await client.from("members").select("user_id").eq("user_id", data.user.id).maybeSingle();
    if (member.error || !member.data) {
      await client.auth.signOut({ scope: "local" });
      throwDatabaseError(member.error);
      throw new ApiError(403, "This account does not have access to OurChat.");
    }
    return json({ ok: true });
  } catch (error) { return fail(error); }
}
