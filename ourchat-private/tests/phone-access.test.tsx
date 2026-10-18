// @vitest-environment jsdom

import { createElement } from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PhoneAccess } from "@/components/phone-access";

vi.mock("@/components/pwa-controls", () => ({
  PwaControls: ({ defaultOpen }: { defaultOpen?: boolean }) => createElement("div", { "data-testid": "device-controls" }, defaultOpen ? "Expanded device controls" : "Device controls"),
}));

const originalClipboard = Object.getOwnPropertyDescriptor(navigator, "clipboard");
const originalShare = Object.getOwnPropertyDescriptor(navigator, "share");
const originalStandalone = Object.getOwnPropertyDescriptor(navigator, "standalone");
const originalShowModal = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "showModal");
const originalClose = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "close");

function restoreProperty(object: object, name: string, value: PropertyDescriptor | undefined) {
  if (value) Object.defineProperty(object, name, value);
  else Reflect.deleteProperty(object, name);
}

function showGuide(authenticated = false) {
  render(createElement(PhoneAccess, { authenticated, compact: true }));
  fireEvent.click(screen.getByRole("button", { name: "Use on phone" }));
  return screen.getByRole("dialog", { name: "OurChat on your phone" });
}

beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_SITE_URL", "");
  vi.stubGlobal("matchMedia", vi.fn(() => ({ matches: false })));
  Object.defineProperty(HTMLDialogElement.prototype, "showModal", { configurable: true, value: function(this: HTMLDialogElement) { this.open = true; } });
  Object.defineProperty(HTMLDialogElement.prototype, "close", { configurable: true, value: function(this: HTMLDialogElement) { this.open = false; } });
  Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText: vi.fn().mockResolvedValue(undefined) } });
  Reflect.deleteProperty(navigator, "share");
  window.history.replaceState(null, "", "/login?code=private-code#access_token=private-token");
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  restoreProperty(navigator, "clipboard", originalClipboard);
  restoreProperty(navigator, "share", originalShare);
  restoreProperty(navigator, "standalone", originalStandalone);
  restoreProperty(HTMLDialogElement.prototype, "showModal", originalShowModal);
  restoreProperty(HTMLDialogElement.prototype, "close", originalClose);
});

describe("phone access", () => {
  it("copies only the public canonical origin, excluding current login parameters", async () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "https://ourchat.example.com/login?code=never-share#private");
    const dialog = showGuide();
    const input = screen.getByRole("textbox", { name: "OurChat website link" }) as HTMLInputElement;
    expect(input.value).toBe("https://ourchat.example.com");
    expect(dialog.innerHTML).not.toContain("private-token");
    expect(dialog.innerHTML).not.toContain("never-share");
    expect(dialog.querySelectorAll("svg[role='img']").length + dialog.querySelectorAll("svg > title").length).toBeGreaterThan(0);
    fireEvent.click(screen.getByRole("button", { name: "Copy link" }));
    await screen.findByText(/Link copied/);
    expect(navigator.clipboard.writeText).toHaveBeenCalledExactlyOnceWith("https://ourchat.example.com");
  });

  it("uses the current origin when the configured address is invalid and keeps a manual copy fallback", async () => {
    vi.stubEnv("NEXT_PUBLIC_SITE_URL", "javascript:alert('private')");
    vi.mocked(navigator.clipboard.writeText).mockRejectedValue(new Error("blocked"));
    showGuide();
    fireEvent.click(screen.getByRole("button", { name: "Copy link" }));
    await screen.findByText(/Copy was unavailable/);
    const input = screen.getByRole("textbox", { name: "OurChat website link" }) as HTMLInputElement;
    expect(input.value).toBe(window.location.origin);
    expect(input.readOnly).toBe(true);
    expect(document.activeElement).toBe(input);
    expect(input.selectionStart).toBe(0);
    expect(input.selectionEnd).toBe(input.value.length);
  });

  it("shares the origin only on a click and treats cancellation quietly", async () => {
    const share = vi.fn().mockRejectedValue(new DOMException("Canceled", "AbortError"));
    Object.defineProperty(navigator, "share", { configurable: true, value: share });
    showGuide();
    expect(share).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Share link" }));
    await waitFor(() => expect(share).toHaveBeenCalledExactlyOnceWith({ title: "OurChat", url: window.location.origin }));
    expect(screen.queryByText(/Sharing is unavailable/)).toBeNull();
    expect(screen.queryByText("Link shared.")).toBeNull();
  });

  it("shows the iPhone Safari steps without Android steps or authenticated controls", () => {
    vi.spyOn(navigator, "userAgent", "get").mockReturnValue("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)");
    showGuide();
    expect(screen.getByText("iPhone / iPad")).toBeDefined();
    expect(screen.getByText("Open this link in Safari.")).toBeDefined();
    expect(screen.queryByText("Open this link in Chrome.")).toBeNull();
    expect(screen.queryByTestId("device-controls")).toBeNull();
    expect(screen.queryByRole("button", { name: "Install OurChat" })).toBeNull();
  });

  it("shows Android Chrome steps and the single authenticated device control", () => {
    vi.spyOn(navigator, "userAgent", "get").mockReturnValue("Mozilla/5.0 (Linux; Android 15)");
    showGuide(true);
    expect(screen.getByText("Open this link in Chrome.")).toBeDefined();
    expect(screen.queryByText("Open this link in Safari.")).toBeNull();
    expect(screen.getAllByTestId("device-controls")).toHaveLength(1);
    expect(screen.getByText("Expanded device controls")).toBeDefined();
  });

  it("recognizes an installed app and lets the user close the dialog", () => {
    vi.stubGlobal("matchMedia", () => ({ matches: true }));
    showGuide();
    expect(screen.getByText("OurChat is installed on this device. Open it from your Home Screen.")).toBeDefined();
    expect(screen.queryByText("Open this link in Chrome.")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Close phone guide" }));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getByRole("button", { name: "Use on phone" })).toBeDefined();
  });

  it("offers a native install action only after the browser supplies a prompt", async () => {
    showGuide();
    const prompt = vi.fn().mockResolvedValue(undefined);
    const event = Object.assign(new Event("beforeinstallprompt", { cancelable: true }), {
      prompt, userChoice: Promise.resolve({ outcome: "accepted", platform: "web" }),
    });
    act(() => { window.dispatchEvent(event); });
    expect(event.defaultPrevented).toBe(true);
    expect(prompt).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Install OurChat" }));
    await screen.findByText("Installation started. Follow your browser's instructions.");
    expect(prompt).toHaveBeenCalledOnce();
    act(() => { window.dispatchEvent(new Event("appinstalled")); });
    expect(screen.getByText("OurChat is installed on this device. Open it from your Home Screen.")).toBeDefined();
  });
});
