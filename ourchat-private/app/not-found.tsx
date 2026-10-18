import Link from "next/link";
export default function NotFound() { return <main className="grid min-h-dvh place-items-center p-6 text-center"><section><h1 className="text-2xl font-semibold">This page isn’t here</h1><Link href="/chat" className="mt-6 inline-block text-emerald-700 underline">Go to messages</Link></section></main>; }
