"use client";
export default function ErrorPage({ reset }: { reset: () => void }) {
  return <main className="grid min-h-dvh place-items-center p-6"><section className="max-w-sm text-center"><h1 className="text-2xl font-semibold">We couldn’t load your conversation</h1><p className="mt-4 text-stone-500">Please check your connection and try again.</p><button onClick={reset} className="mt-6 rounded-xl bg-[#375c44] px-6 py-3 text-white">Try again</button></section></main>;
}
