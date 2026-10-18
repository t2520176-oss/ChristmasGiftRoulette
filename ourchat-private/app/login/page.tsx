import { redirect } from "next/navigation";
import { Heart, LockKeyhole, MessageCircle } from "lucide-react";
import { LoginForm } from "@/components/login-form";
import { PhoneAccess } from "@/components/phone-access";
import { publicConfig } from "@/lib/config";
import { requireMember } from "@/lib/server/auth";

export const dynamic = "force-dynamic";
export default async function Login() {
  const configured = Boolean(publicConfig());
  if (configured) {
    const context = await requireMember().catch(() => null);
    if (context) redirect("/chat");
  }
  return <main className="login-shell flex min-h-dvh items-center justify-center bg-[#f5f2eb] text-[#2c332b]">
    <div className="grid w-full max-w-5xl overflow-hidden rounded-[2rem] border border-[#e7e3d8] bg-[#fcfbf7] shadow-[0_24px_90px_-40px_rgba(45,60,39,0.3)] md:grid-cols-[1.05fr_1fr]">
      <section className="relative flex flex-col justify-between overflow-hidden bg-[#dfe8dc] px-6 py-5 md:min-h-[650px] md:p-12">
        <div className="flex items-center gap-3 text-lg font-bold tracking-tight"><span className="grid h-10 w-10 place-items-center rounded-2xl bg-[#375c44] text-white"><MessageCircle size={22} aria-hidden /></span>OurChat</div>
        <div className="relative hidden py-16 md:block">
          <span className="mb-5 inline-flex items-center gap-2 rounded-full border border-[#bccdb7] px-3 py-1.5 text-xs font-semibold tracking-wider text-[#446044]"><Heart size={12} aria-hidden />JUST THE TWO OF YOU</span>
          <h1 className="max-w-sm text-4xl leading-[1.14] font-medium tracking-[-.045em] sm:text-5xl">A little closer,<br />wherever you are.</h1>
          <p className="mt-5 max-w-xs text-base leading-7 text-[#597052]">The everyday moments. The little check-ins. A quiet place to stay connected.</p>
          <div className="mt-9 max-w-xs space-y-3" aria-hidden="true">
            <div className="w-fit rounded-2xl rounded-bl-sm bg-white/80 px-4 py-3 text-sm text-[#53644f] shadow-sm">Made it home. Miss you already.</div>
            <div className="ml-auto w-fit rounded-2xl rounded-br-sm bg-[#375c44] px-4 py-3 text-sm text-white shadow-sm">See you soon. <span className="text-[#f1c0b5]">♥</span></div>
          </div>
        </div>
        <p className="hidden items-center gap-2 text-xs text-[#61735c] md:flex"><LockKeyhole size={13} aria-hidden />A private space. No ads. No noise.</p>
      </section>
      <section className="flex min-w-0 flex-col justify-center px-6 py-7 sm:px-12 md:py-16">
        <div className="mb-6 md:mb-8"><p className="mb-2 text-xs font-semibold tracking-[.18em] text-[#84907a]">WELCOME BACK</p><h2 className="text-3xl font-semibold tracking-tight">Sign in</h2><p className="mt-3 text-sm leading-6 text-stone-500">Your conversations are waiting for you.</p></div>
        {!configured && <div role="status" className="mb-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900"><strong className="block">Setup required</strong>OurChat is waiting for its administrator to finish setup. You can sign in once it is ready.</div>}
        <LoginForm configured={configured} />
        <div className="mt-5"><PhoneAccess /></div>
        <p className="mt-7 text-center text-xs leading-5 text-stone-400">Access is reserved for two invited accounts.<br />Need help signing in? Contact your administrator.</p>
      </section>
    </div>
  </main>;
}
