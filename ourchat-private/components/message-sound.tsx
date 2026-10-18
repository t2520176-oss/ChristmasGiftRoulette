"use client";

import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { Volume2, VolumeX } from "lucide-react";

const preferenceKey = "ourchat-message-sound";
const preferenceEvent = "ourchat:message-sound";
let memoryPreference = false;

function readPreference(): boolean {
  try { return localStorage.getItem(preferenceKey) === "on"; }
  catch { return memoryPreference; }
}

function subscribePreference(callback: () => void): () => void {
  window.addEventListener("storage", callback);
  window.addEventListener(preferenceEvent, callback);
  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener(preferenceEvent, callback);
  };
}

function savePreference(enabled: boolean) {
  memoryPreference = enabled;
  try { localStorage.setItem(preferenceKey, enabled ? "on" : "off"); }
  catch { /* This visit still works when browser storage is unavailable. */ }
  window.dispatchEvent(new Event(preferenceEvent));
}

/** A short local chime: no audio downloads or message content leave the device. */
function chime(context: AudioContext) {
  const now = context.currentTime;
  for (const [offset, frequency] of [[0, 660], [0.11, 880]]) {
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    const start = now + offset;
    oscillator.type = "sine";
    oscillator.frequency.setValueAtTime(frequency, start);
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(0.055, start + 0.015);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.22);
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.onended = () => { oscillator.disconnect(); gain.disconnect(); };
    oscillator.start(start);
    oscillator.stop(start + 0.23);
  }
}

export function useMessageSound() {
  const preferred = useSyncExternalStore(subscribePreference, readPreference, () => false);
  const [enabled, setEnabled] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const contextRef = useRef<AudioContext | null>(null);
  const activeRef = useRef(false);
  const desiredRef = useRef(false);
  const pendingRef = useRef(false);
  const mountedRef = useRef(true);
  const generationRef = useRef(0);
  const automaticAttemptRef = useRef(false);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      activeRef.current = false;
      desiredRef.current = false;
      pendingRef.current = false;
      generationRef.current += 1;
      const context = contextRef.current;
      contextRef.current = null;
      if (context) {
        context.onstatechange = null;
        void context.close().catch(() => { /* Already closed or disposed by the browser. */ });
      }
    };
  }, []);

  const enable = useCallback((preview: boolean) => {
    const generation = ++generationRef.current;
    desiredRef.current = true;
    pendingRef.current = true;
    setError("");
    setPending(true);
    const fail = (message: string) => {
      if (!mountedRef.current || generationRef.current !== generation) return;
      desiredRef.current = false;
      activeRef.current = false;
      pendingRef.current = false;
      setEnabled(false);
      setPending(false);
      setError(message);
    };

    try {
      const Audio = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Audio) {
        fail("Message sound is unavailable in this browser. Phone notifications can still use your system sound settings.");
        return;
      }
      let context = contextRef.current;
      if (!context || context.state === "closed") {
        // Both creation and resume happen directly inside this tap, as iOS requires.
        context = new Audio();
        contextRef.current = context;
        const created = context;
        context.onstatechange = () => {
          if (!mountedRef.current || contextRef.current !== created) return;
          const running = desiredRef.current && created.state === "running";
          activeRef.current = running;
          setEnabled(running);
        };
      }
      const resume = context.state === "running" ? Promise.resolve() : context.resume();
      void resume.then(() => {
        if (!mountedRef.current || generationRef.current !== generation) return;
        if (context.state !== "running") {
          fail("Sound is paused by your browser. Tap again to start it.");
          return;
        }
        // Do not show “on” until the browser has actually unlocked audio.
        if (preview) chime(context);
        activeRef.current = true;
        pendingRef.current = false;
        automaticAttemptRef.current = false;
        setEnabled(true);
        setPending(false);
        savePreference(true);
      }).catch(() => fail("Sound could not start. Tap the sound button to try again."));
    } catch {
      fail("Sound could not start. Tap the sound button to try again.");
    }
  }, []);

  const toggle = useCallback(() => {
    if (activeRef.current || pendingRef.current) {
      generationRef.current += 1;
      desiredRef.current = false;
      activeRef.current = false;
      pendingRef.current = false;
      setEnabled(false);
      setPending(false);
      setError("");
      savePreference(false);
      return;
    }
    automaticAttemptRef.current = false;
    enable(true);
  }, [enable]);

  useEffect(() => {
    const unlockRememberedSound = (event: Event) => {
      if (!readPreference() || activeRef.current || pendingRef.current || automaticAttemptRef.current) return;
      if (event.target instanceof Element && event.target.closest("[data-message-sound-control]")) return;
      if (event instanceof KeyboardEvent && (event.repeat || event.altKey || event.ctrlKey || event.metaKey ||
        ["Alt", "Control", "Meta", "Shift", "Escape", "Tab"].includes(event.key))) return;
      // A browser still requires a gesture after reopening. Restore only a prior
      // opt-in, inside that gesture, without a test chime or autoplay on mount.
      automaticAttemptRef.current = true;
      enable(false);
    };
    document.addEventListener("click", unlockRememberedSound);
    document.addEventListener("keydown", unlockRememberedSound);
    return () => {
      document.removeEventListener("click", unlockRememberedSound);
      document.removeEventListener("keydown", unlockRememberedSound);
    };
  }, [enable]);

  const playIncoming = useCallback(() => {
    const context = contextRef.current;
    if (!activeRef.current || !context || context.state !== "running" ||
      document.visibilityState !== "visible" || !document.hasFocus()) return;
    try { chime(context); }
    catch {
      activeRef.current = false;
      desiredRef.current = false;
      if (mountedRef.current) {
        setEnabled(false);
        setError("Sound stopped. Tap the sound button to try again.");
      }
    }
  }, []);

  return { enabled, pending, preferred, error, toggle, playIncoming };
}

export function MessageSoundControl({ sound }: { sound: ReturnType<typeof useMessageSound> }) {
  const label = sound.enabled ? "Turn off message sound" : sound.pending ? "Cancel enabling message sound" : "Turn on message sound";
  const text = sound.enabled ? "Sound on" : sound.pending ? "Starting…" : sound.preferred ? "Tap for sound" : "Sound off";
  return <div data-message-sound-control className="relative shrink-0">
    <button type="button" onClick={sound.toggle} aria-label={label} aria-pressed={sound.enabled}
      title={sound.enabled ? "Sound plays for new messages while this chat is open. Tap to turn it off." : "Tap to hear a test chime and enable message sound for this visit."}
      className="flex min-h-11 items-center gap-2 rounded-full border border-white/30 bg-raised px-3 text-xs font-medium text-white hover:bg-highlight focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus">
      {sound.enabled ? <Volume2 size={18} aria-hidden="true" /> : <VolumeX size={18} aria-hidden="true" />}
      <span>{text}</span>
    </button>
    {sound.error && <p role="status" className="absolute top-full right-0 z-20 mt-2 w-56 rounded-xl border border-white/20 bg-warning p-3 text-xs leading-5 text-white shadow-lg">{sound.error}</p>}
  </div>;
}
