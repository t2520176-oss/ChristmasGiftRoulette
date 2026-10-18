// @vitest-environment jsdom
import React from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ChatClient from "@/components/chat/chat-client";
import type { ChatMessage } from "@/lib/types";

const realtime = vi.hoisted(() => {
  const events = new Map<string, () => void>();
  const channel = {
    on: vi.fn(), subscribe: vi.fn(), track: vi.fn().mockResolvedValue("ok"),
    untrack: vi.fn().mockResolvedValue("ok"), presenceState: vi.fn().mockReturnValue({}),
  };
  channel.on.mockImplementation((event: string, filter: { table?: string; event?: string }, callback: () => void) => {
    events.set(filter.table ?? `${event}:${filter.event}`, callback);
    return channel;
  });
  channel.subscribe.mockReturnValue(channel);
  return { channel, events, removeChannel: vi.fn(), replace: vi.fn(), refresh: vi.fn(), cleanupPush: vi.fn().mockResolvedValue(undefined) };
});

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: realtime.replace, refresh: realtime.refresh }) }));
vi.mock("@/lib/supabase/browser", () => ({ createClient: () => ({ channel: () => realtime.channel, removeChannel: realtime.removeChannel }) }));
vi.mock("@/components/pwa-controls", () => ({ PwaControls: () => <div>Device settings</div>, cleanupCurrentPushSubscription: realtime.cleanupPush }));

const self = { user_id: "husband", display_name: "Husband" };
const peer = { user_id: "wife", display_name: "Wife" };
const message = (id: string, body = `Message ${id}`, sender = "wife", nonce = `nonce-${id}`): ChatMessage => ({
  id, body, sender_id: sender, client_nonce: nonce, created_at: "2026-10-18T12:00:00.000Z",
});

class TestObserver {
  static instances: TestObserver[] = [];
  elements = new Set<Element>();
  constructor(public callback: IntersectionObserverCallback) { TestObserver.instances.push(this); }
  observe(element: Element) { this.elements.add(element); }
  unobserve(element: Element) { this.elements.delete(element); }
  disconnect() { this.elements.clear(); }
  takeRecords() { return []; }
  reveal(id: string) {
    const element = [...this.elements].find((entry) => (entry as HTMLElement).dataset.messageId === id);
    if (!element) throw new Error(`Message ${id} is not observed`);
    this.callback([{
      target: element, isIntersecting: true, intersectionRatio: 1,
      intersectionRect: { height: 40 }, boundingClientRect: { height: 40 },
    } as IntersectionObserverEntry], this as unknown as IntersectionObserver);
  }
}

let networkOnline = true;
let fetchMock: ReturnType<typeof vi.fn>;
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
const posts = () => fetchMock.mock.calls.filter(([url, options]) => url === "/api/messages" && options?.method === "POST");

beforeEach(() => {
  networkOnline = true;
  realtime.events.clear();
  realtime.cleanupPush.mockResolvedValue(undefined);
  TestObserver.instances = [];
  Object.defineProperty(navigator, "onLine", { configurable: true, get: () => networkOnline });
  Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
  vi.stubGlobal("IntersectionObserver", TestObserver);
  vi.stubGlobal("matchMedia", () => ({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() }));
  fetchMock = vi.fn(async (url: string, options?: RequestInit) => {
    if (url === "/api/read" && options?.method === "POST") {
      const body = JSON.parse(String(options.body)) as { message_id: string };
      return json({ receipt: { user_id: self.user_id, last_read_message_id: body.message_id, updated_at: new Date().toISOString() } });
    }
    if (url === "/api/read") return json({ receipts: [] });
    if (url === "/api/messages" && options?.method === "POST") {
      const body = JSON.parse(String(options.body)) as { body: string; client_nonce: string };
      return json({ message: message("101", body.body, self.user_id, body.client_nonce) });
    }
    return json({ messages: [], hasMore: false });
  });
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe("chat interaction", () => {
  it("waits for IME composition and Shift+Enter before sending a Unicode message", async () => {
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox", { name: "Type a message" });
    fireEvent.change(input, { target: { value: "안녕하세요 👋" } });
    fireEvent.compositionStart(input);
    fireEvent.keyDown(input, { key: "Enter", code: "Enter" });
    expect(posts()).toHaveLength(0);
    expect((input as HTMLTextAreaElement).value).toBe("안녕하세요 👋");
    fireEvent.compositionEnd(input);
    fireEvent.keyDown(input, { key: "Enter", shiftKey: true });
    expect(posts()).toHaveLength(0);
    fireEvent.keyDown(input, { key: "Enter" });
    await waitFor(() => expect(posts()).toHaveLength(1));
    await waitFor(() => expect(screen.getAllByText("안녕하세요 👋")).toHaveLength(1));
    expect((input as HTMLTextAreaElement).value).toBe("");
  });

  it("keeps the draft offline and sends it only after a connection returns", async () => {
    networkOnline = false;
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox");
    fireEvent.change(input, { target: { value: "Keep this note" } });
    fireEvent.keyDown(input, { key: "Enter" });
    expect(posts()).toHaveLength(0);
    expect((input as HTMLTextAreaElement).value).toBe("Keep this note");
    expect((screen.getByRole("button", { name: "Send message" }) as HTMLButtonElement).disabled).toBe(true);
    act(() => { networkOnline = true; window.dispatchEvent(new Event("online")); });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    await waitFor(() => expect(posts()).toHaveLength(1));
  });

  it("retries the same nonce and merges a duplicate acknowledgement into one message", async () => {
    let attempts = 0;
    const original = fetchMock.getMockImplementation()! as (url: string, options?: RequestInit) => Promise<Response>;
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (url === "/api/messages" && options?.method === "POST" && attempts++ === 0) throw new TypeError("Network unavailable");
      return original(url, options);
    });
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Just one note" } });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    await screen.findByText("Failed to send");
    fireEvent.click(screen.getByRole("button", { name: "Retry failed message" }));
    await waitFor(() => expect(posts()).toHaveLength(2));
    expect(JSON.parse(String(posts()[0][1].body)).client_nonce).toBe(JSON.parse(String(posts()[1][1].body)).client_nonce);
    await waitFor(() => expect(screen.queryByText("Failed to send")).toBeNull());
    expect(screen.getAllByText("Just one note")).toHaveLength(1);
  });

  it("blocks a 2001-code-point draft while accepting Unicode input", () => {
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "👋".repeat(2001) } });
    expect((screen.getByRole("button", { name: "Send message" }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText("2,001 / 2,000")).toBeTruthy();
  });

  it("marks only an actually visible history message, and never while hidden", async () => {
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10"), message("999")]} initialReceipts={[]} />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
    act(() => TestObserver.instances.at(-1)!.reveal("10"));
    await new Promise((resolve) => setTimeout(resolve, 500));
    expect(fetchMock.mock.calls.filter(([url, options]) => url === "/api/read" && options?.method === "POST")).toHaveLength(0);
    act(() => {
      Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
      document.dispatchEvent(new Event("visibilitychange"));
    });
    await waitFor(() => {
      const calls = fetchMock.mock.calls.filter(([url, options]) => url === "/api/read" && options?.method === "POST");
      expect(calls).toHaveLength(1);
      expect(JSON.parse(String(calls[0][1].body)).message_id).toBe("10");
    }, { timeout: 1600 });
  });

  it("fetches every missed page after reconnect and retains older history", async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === "/api/read") return json({ receipts: [] });
      if (url === "/api/messages?after=100") return json({ messages: [message("101")], hasMore: true });
      if (url === "/api/messages?after=101") return json({ messages: [message("102")], hasMore: false });
      return json({ messages: [], hasMore: false });
    });
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("1"), message("100")]} initialReceipts={[]} />);
    await screen.findByText("Message 102");
    expect(screen.getByText("Message 1")).toBeTruthy();
    expect(screen.getByText("Message 100")).toBeTruthy();
    expect(screen.getAllByText("Message 101")).toHaveLength(1);
    expect(fetchMock.mock.calls.some(([url]) => url === "/api/messages?after=101")).toBe(true);
  });

  it("stays signed in when current-device push cleanup fails", async () => {
    realtime.cleanupPush.mockRejectedValue(new Error("Could not disconnect this device. Try again."));
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    fireEvent.click(screen.getByRole("button", { name: /Husband/ }));
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
    await screen.findByRole("alert");
    expect(fetchMock.mock.calls.some(([url]) => url === "/api/auth/signout")).toBe(false);
    expect(realtime.replace).not.toHaveBeenCalled();
  });
});
