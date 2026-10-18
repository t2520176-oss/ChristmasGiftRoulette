import Link from "next/link";
export default function NotFound() { return <main className="grid min-h-dvh place-items-center bg-canvas p-6 text-center text-white"><section><h1 className="text-2xl font-semibold">This page isn’t here</h1><Link href="/chat" className="mt-6 inline-block text-white underline">Go to messages</Link></section></main>; }
