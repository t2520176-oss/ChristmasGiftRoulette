"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Eye, EyeOff, LoaderCircle } from "lucide-react";

export function LoginForm({ configured }: { configured: boolean }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [visible, setVisible] = useState(false);
  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending || !configured) return;
    const form = new FormData(event.currentTarget);
    setPending(true); setError("");
    try {
      const response = await fetch("/api/auth/signin", {
        method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: form.get("email"), password: form.get("password") }),
      });
      const result = await response.json();
      if (!response.ok) { setError(typeof result.error === "string" ? result.error : "Could not sign in. Please try again."); return; }
      router.replace("/chat"); router.refresh();
    } catch { setError("Could not connect. Please check your connection and try again."); }
    finally { setPending(false); }
  }
  return <form onSubmit={submit} className="space-y-5">
    <div>
      <label htmlFor="email" className="mb-2 block text-sm font-semibold text-stone-700">Email</label>
      <input id="email" name="email" type="email" inputMode="email" autoCapitalize="none" autoCorrect="off" spellCheck={false} autoComplete="username" placeholder="you@example.com" required maxLength={254} disabled={!configured || pending}
        className="w-full rounded-xl border border-stone-300 bg-white px-4 py-3.5 text-base text-stone-900 placeholder:text-stone-400 disabled:opacity-60" />
    </div>
    <div>
      <label htmlFor="password" className="mb-2 block text-sm font-semibold text-stone-700">Password</label>
      <div className="relative">
        <input id="password" name="password" type={visible ? "text" : "password"} autoComplete="current-password" placeholder="Your password" required maxLength={1024} disabled={!configured || pending}
          className="w-full rounded-xl border border-stone-300 bg-white py-3.5 pr-14 pl-4 text-base text-stone-900 placeholder:text-stone-400 disabled:opacity-60" />
        <button type="button" onClick={() => setVisible(!visible)} aria-label={visible ? "Hide password" : "Show password"} aria-pressed={visible}
          className="absolute inset-y-0 right-1 flex min-w-12 items-center justify-center rounded-lg text-stone-500 hover:text-stone-800">
          {visible ? <EyeOff size={19} aria-hidden /> : <Eye size={19} aria-hidden />}
        </button>
      </div>
    </div>
    {error && <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm leading-relaxed text-red-800">{error}</p>}
    <button type="submit" disabled={!configured || pending}
      className="flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-[#375c44] px-5 py-3.5 font-semibold text-white transition hover:bg-[#294834] disabled:opacity-50">
      {pending ? <><LoaderCircle className="animate-spin" size={18} aria-hidden />Signing in…</> : <>Sign in<ArrowRight size={18} aria-hidden /></>}
    </button>
  </form>;
}
