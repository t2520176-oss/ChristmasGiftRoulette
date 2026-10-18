import { requireMember } from "@/lib/server/auth";
import { assertSameOrigin, fail, json } from "@/lib/server/http";

export const runtime = "nodejs";
export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const { client } = await requireMember();
    const { error } = await client.auth.signOut({ scope: "local" });
    if (error) return json({ error: "Could not sign out. Please try again." }, 500);
    return json({ ok: true });
  } catch (error) { return fail(error); }
}
