import "server-only";
import { NextResponse } from "next/server";
import { ZodError, type ZodType } from "zod";

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); this.name = "ApiError"; }
}
export function json(data: unknown, status = 200) {
  return NextResponse.json(data, { status, headers: { "Cache-Control": "private, no-store, max-age=0" } });
}
export function fail(error: unknown) {
  if (error instanceof ApiError) return json({ error: error.message }, error.status);
  if (error instanceof ZodError) return json({ error: "Please check the information you entered." }, 400);
  return json({ error: "Something went wrong. Please try again." }, 500);
}
export function assertSameOrigin(request: Request) {
  const origin = request.headers.get("origin");
  const site = process.env.NEXT_PUBLIC_SITE_URL;
  let expected: string;
  try {
    if (site) expected = new URL(site).origin;
    else if (process.env.NODE_ENV !== "production") expected = new URL(request.url).origin;
    else throw new Error("Missing site URL");
  } catch { throw new ApiError(503, "OurChat is not configured yet. Please contact the administrator."); }
  if (!origin || origin !== expected || request.headers.get("sec-fetch-site") === "cross-site") {
    throw new ApiError(403, "This request is not allowed.");
  }
}
export async function readJson<T>(request: Request, schema: ZodType<T>, limit = 16_384): Promise<T> {
  if (!request.headers.get("content-type")?.toLowerCase().startsWith("application/json")) {
    throw new ApiError(415, "Please send a JSON request.");
  }
  const length = Number(request.headers.get("content-length") ?? "0");
  if (length > limit) throw new ApiError(413, "This request is too large.");
  const reader = request.body?.getReader();
  if (!reader) throw new ApiError(400, "The request is empty.");
  const chunks: Uint8Array[] = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > limit) { await reader.cancel(); throw new ApiError(413, "This request is too large."); }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(size);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
  let input: unknown;
  try { input = JSON.parse(new TextDecoder("utf-8", { fatal: true }).decode(bytes)); }
  catch { throw new ApiError(400, "The request could not be read."); }
  return schema.parse(input);
}
export function throwDatabaseError(error: { code?: string; message?: string } | null) {
  if (!error) return;
  if (error.code === "P0001" && error.message?.includes("rate")) throw new ApiError(429, "Too many requests. Please wait a moment and try again.");
  if (error.code === "42501") throw new ApiError(403, "You do not have access to this conversation.");
  if (["23514", "22P02", "22023", "23503"].includes(error.code ?? "")) throw new ApiError(400, "Please check the information you entered.");
  if (["42P01", "PGRST205", "PGRST202"].includes(error.code ?? "")) throw new ApiError(503, "OurChat needs its database setup. Please contact the administrator.");
  throw new ApiError(500, "Your request could not be completed. Please try again.");
}
