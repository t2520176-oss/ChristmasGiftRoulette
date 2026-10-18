import { z } from "zod";
import { checkRateLimit, requireMember } from "@/lib/server/auth";
import { ApiError, assertSameOrigin, fail, json, readJson, throwDatabaseError } from "@/lib/server/http";
import { listReceipts } from "@/lib/server/messages";
import { readInputSchema, receiptRowSchema } from "@/lib/validation";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export async function GET() {
  try { return json({ receipts: await listReceipts(await requireMember()) }); }
  catch (error) { return fail(error); }
}
export async function POST(request: Request) {
  try {
    assertSameOrigin(request);
    const context = await requireMember();
    const input = await readJson(request, readInputSchema);
    await checkRateLimit(context, "read");
    const result = await context.client.rpc("mark_read", { p_message_id: input.message_id });
    throwDatabaseError(result.error);
    const receipts = z.array(receiptRowSchema).parse(result.data);
    if (!receipts[0]) throw new ApiError(400, "That message could not be found.");
    return json({ receipt: receipts[0] });
  } catch (error) { return fail(error); }
}
