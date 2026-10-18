// @vitest-environment jsdom
import React from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MessageSoundControl, useMessageSound } from "@/components/message-sound";

class TestAudioContext {
  static instances: TestAudioContext[] = [];
  state: AudioContextState = "suspended";
  currentTime = 1;
  destination = {};
  onstatechange: (() => void) | null = null;
  resume = vi.fn(async () => { this.state = "running"; });
  close = vi.fn(async () => { this.state = "closed"; });
  createOscillator = vi.fn(() => ({
    type: "sine", frequency: { setValueAtTime: vi.fn() },
    connect: vi.fn(), disconnect: vi.fn(), start: vi.fn(), stop: vi.fn(), onended: null,
  }));
  createGain = vi.fn(() => ({
    gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
    connect: vi.fn(), disconnect: vi.fn(),
  }));
  constructor() { TestAudioContext.instances.push(this); }
}

function SoundTest() {
  const sound = useMessageSound();
  // This fixture invokes a network-arrival callback; it must not also count as
  // a gesture that restores previously granted sound permission.
  return <><MessageSoundControl sound={sound} /><button data-message-sound-control onClick={sound.playIncoming}>Incoming message</button></>;
}

beforeEach(() => {
  TestAudioContext.instances = [];
  localStorage.clear();
  Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
  vi.spyOn(document, "hasFocus").mockReturnValue(true);
  vi.stubGlobal("AudioContext", TestAudioContext);
});
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe("message sound", () => {
  it("stays silent on mount, unlocks audio directly on a tap, and stops when disabled", async () => {
    render(<SoundTest />);
    expect(TestAudioContext.instances).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(TestAudioContext.instances).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    const audio = TestAudioContext.instances[0];
    expect(audio.resume).toHaveBeenCalledOnce(); // Already invoked before the resume promise settles.
    await screen.findByRole("button", { name: "Turn off message sound", pressed: true });
    expect(audio.createOscillator).toHaveBeenCalledTimes(2); // Confirmation chime.
    expect(localStorage.getItem("ourchat-message-sound")).toBe("on");
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(audio.createOscillator).toHaveBeenCalledTimes(4);
    fireEvent.click(screen.getByRole("button", { name: "Turn off message sound" }));
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(audio.createOscillator).toHaveBeenCalledTimes(4);
    expect(localStorage.getItem("ourchat-message-sound")).toBe("off");
    expect(screen.getByRole("button", { name: "Turn on message sound", pressed: false })).toBeTruthy();
  });

  it("remembers the preference after reload but requests another tap before claiming audio is on", async () => {
    localStorage.setItem("ourchat-message-sound", "on");
    render(<SoundTest />);
    expect(screen.getByRole("button", { name: "Turn on message sound", pressed: false }).textContent).toContain("Tap for sound");
    expect(TestAudioContext.instances).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    await screen.findByRole("button", { name: "Turn off message sound" });
    expect(TestAudioContext.instances[0].createOscillator).toHaveBeenCalledTimes(2);
  });

  it("silently unlocks remembered consent on the next chat gesture without toggling the sound button twice", async () => {
    localStorage.setItem("ourchat-message-sound", "on");
    render(<SoundTest />);
    expect(TestAudioContext.instances).toHaveLength(0);
    fireEvent.click(document.body);
    const audio = TestAudioContext.instances[0];
    expect(audio.resume).toHaveBeenCalledOnce();
    await screen.findByRole("button", { name: "Turn off message sound", pressed: true });
    expect(audio.createOscillator).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(audio.createOscillator).toHaveBeenCalledTimes(2);
    fireEvent.click(screen.getByRole("button", { name: "Turn off message sound" }));
    fireEvent.click(document.body);
    expect(screen.getByRole("button", { name: "Turn on message sound", pressed: false })).toBeTruthy();
    expect(audio.resume).toHaveBeenCalledOnce();
  });

  it("accepts a typing gesture for saved consent and never opts in a first-time visitor automatically", async () => {
    const view = render(<SoundTest />);
    fireEvent.click(document.body);
    fireEvent.keyDown(document.body, { key: "a" });
    expect(TestAudioContext.instances).toHaveLength(0);
    view.unmount();
    localStorage.setItem("ourchat-message-sound", "on");
    render(<SoundTest />);
    fireEvent.keyDown(document.body, { key: "Shift" });
    expect(TestAudioContext.instances).toHaveLength(0);
    fireEvent.keyDown(document.body, { key: "a" });
    await screen.findByRole("button", { name: "Turn off message sound" });
    expect(TestAudioContext.instances[0].createOscillator).not.toHaveBeenCalled();
  });

  it("uses the Safari prefixed audio context and closes it when the chat unmounts", async () => {
    vi.stubGlobal("AudioContext", undefined);
    vi.stubGlobal("webkitAudioContext", TestAudioContext);
    const view = render(<SoundTest />);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    await screen.findByRole("button", { name: "Turn off message sound" });
    const audio = TestAudioContext.instances[0];
    view.unmount();
    expect(audio.close).toHaveBeenCalledOnce();
    expect(audio.onstatechange).toBeNull();
  });

  it("does not play foreground audio while the page is hidden or unfocused", async () => {
    render(<SoundTest />);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    await screen.findByRole("button", { name: "Turn off message sound" });
    const audio = TestAudioContext.instances[0];
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
    vi.mocked(document.hasFocus).mockReturnValue(false);
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(audio.createOscillator).toHaveBeenCalledTimes(2);
    vi.mocked(document.hasFocus).mockReturnValue(true);
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(audio.createOscillator).toHaveBeenCalledTimes(4);
  });

  it("shows a truthful error when audio is unsupported", () => {
    vi.stubGlobal("AudioContext", undefined);
    render(<SoundTest />);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    expect(screen.getByRole("status").textContent).toContain("unavailable");
    expect(screen.getByRole("button", { name: "Turn on message sound", pressed: false })).toBeTruthy();
    expect(localStorage.getItem("ourchat-message-sound")).not.toBe("on");
  });

  it("keeps sound off after a rejected resume and allows a successful retry", async () => {
    render(<SoundTest />);
    // Delay resume so rejection happens after the gesture has returned.
    let rejectResume!: (reason: Error) => void;
    class BlockedAudio extends TestAudioContext {
      resume = vi.fn(() => new Promise<void>((_resolve, reject) => { rejectResume = reject; }));
    }
    vi.stubGlobal("AudioContext", BlockedAudio);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    expect(screen.getByRole("button", { name: "Cancel enabling message sound", pressed: false })).toBeTruthy();
    await act(async () => rejectResume(new Error("NotAllowedError")));
    expect(screen.getByRole("status").textContent).toContain("could not start");
    const audio = TestAudioContext.instances[0];
    audio.resume.mockImplementation(async () => { audio.state = "running"; });
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    await screen.findByRole("button", { name: "Turn off message sound" });
    expect(audio.createOscillator).toHaveBeenCalledTimes(2);
  });

  it("requires a fresh tap when the browser suspends audio, without replaying old arrivals", async () => {
    render(<SoundTest />);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    await screen.findByRole("button", { name: "Turn off message sound" });
    const audio = TestAudioContext.instances[0];
    act(() => { audio.state = "suspended"; audio.onstatechange?.(); });
    expect(screen.getByRole("button", { name: "Turn on message sound", pressed: false })).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Incoming message" }));
    expect(audio.createOscillator).toHaveBeenCalledTimes(2);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    await screen.findByRole("button", { name: "Turn off message sound" });
    expect(audio.resume).toHaveBeenCalledTimes(2);
    expect(audio.createOscillator).toHaveBeenCalledTimes(4);
  });

  it("does not turn sound back on when a pending enable is canceled or unmounted", async () => {
    let finishResume!: () => void;
    class SlowAudio extends TestAudioContext {
      resume = vi.fn(() => new Promise<void>((resolve) => {
        finishResume = () => { this.state = "running"; resolve(); };
      }));
    }
    vi.stubGlobal("AudioContext", SlowAudio);
    const view = render(<SoundTest />);
    fireEvent.click(screen.getByRole("button", { name: "Turn on message sound" }));
    fireEvent.click(screen.getByRole("button", { name: "Cancel enabling message sound" }));
    await act(async () => finishResume());
    expect(screen.getByRole("button", { name: "Turn on message sound", pressed: false })).toBeTruthy();
    expect(TestAudioContext.instances[0].createOscillator).not.toHaveBeenCalled();
    view.unmount();
    await waitFor(() => expect(TestAudioContext.instances[0].close).toHaveBeenCalledOnce());
  });
});
