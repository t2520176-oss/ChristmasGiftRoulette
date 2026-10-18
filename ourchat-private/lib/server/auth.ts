import "server-only";
import { createClient } from "@/lib/supabase/server";
import { memberSchema } from "@/lib/validation";
import { ApiError, throwDatabaseError } from "./http";

export async function requireMember() {
  const client = await createClient();
  const { data: { user }, error } = await client.auth.getUser();
  if (error || !user) throw new ApiError(401, "Please sign in to continue.");
  const result = await client.from("members").select("user_id,display_name,created_at").eq("user_id", user.id).maybeSingle();
  throwDatabaseError(result.error);
  if (!result.data) throw new ApiError(403, "This account does not have access to OurChat.");
  return { client, user, member: memberSchema.parse(result.data), authorized: true as const };
}
export type MemberContext = Awaited<ReturnType<typeof requireMember>>;
export async function checkRateLimit(context: MemberContext, action: "read" | "push_subscribe" | "push_delete") {
  const result = await context.client.rpc("check_api_rate_limit", { p_action: action });
  throwDatabaseError(result.error);
}
