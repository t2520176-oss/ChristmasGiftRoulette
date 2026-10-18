"use client";
export default function ErrorPage({ reset }: { reset: () => void }) {
  return <main className="grid min-h-dvh place-items-center bg-canvas p-6 text-white"><section className="max-w-sm text-center"><h1 className="text-2xl font-semibold">We couldn’t load your conversation</h1><p className="mt-4 text-white/80">Please check your connection and try again.</p><button onClick={reset} className="mt-6 rounded-xl border border-white/30 bg-accent px-6 py-3 text-white hover:bg-highlight">Try again</button></section></main>;
}
