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
  return { channel, events, removeChannel: vi.fn(), replace: vi.fn(), refresh: vi.fn(), cleanupPush: vi.fn().mockResolvedValue(undefined), playSound: vi.fn() };
});

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: realtime.replace, refresh: realtime.refresh }) }));
vi.mock("@/lib/supabase/browser", () => ({ createClient: () => ({ channel: () => realtime.channel, removeChannel: realtime.removeChannel }) }));
vi.mock("@/components/pwa-controls", () => ({ PwaControls: () => <div>Device settings</div>, cleanupCurrentPushSubscription: realtime.cleanupPush }));
vi.mock("@/components/message-sound", () => ({
  useMessageSound: () => ({ playIncoming: realtime.playSound }),
  MessageSoundControl: () => <div>Sound settings</div>,
}));

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
  vi.spyOn(document, "hasFocus").mockReturnValue(true);
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

afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe("chat interaction", () => {
  it("loads earlier messages when the server marks a short initial page as having more history", async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url === "/api/read") return json({ receipts: [] });
      return json({ messages: url.includes("before=") ? [message("9", "An earlier note")] : [], hasMore: false });
    });
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10")]} initialHasMore initialReceipts={[]} />);
    fireEvent.click(screen.getByRole("button", { name: "Earlier messages" }));
    await screen.findByText("An earlier note");
    expect(fetchMock.mock.calls.some(([url]) => url === "/api/messages?before=10")).toBe(true);
    expect(screen.queryByRole("button", { name: "Earlier messages" })).toBeNull();
  });

  it("chimes once for a fresh peer batch, without replaying duplicate or own messages", async () => {
    let incoming: ChatMessage[] = [];
    fetchMock.mockImplementation(async (url: string) => url === "/api/read"
      ? json({ receipts: [] }) : json({ messages: incoming, hasMore: false }));
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10")]} initialReceipts={[]} />);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url === "/api/read")).toBe(true));
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    incoming = [message("11"), message("12"), message("13", "My reply", self.user_id)];
    act(() => window.dispatchEvent(new Event("online")));
    await screen.findByText("Message 12");
    expect(realtime.playSound).toHaveBeenCalledOnce();
    expect(screen.getByRole("status", { name: "Incoming message notification" }).textContent).toContain("2 new messages");
    incoming = [...incoming, message("14", "Another own message", self.user_id)];
    act(() => window.dispatchEvent(new Event("online")));
    await screen.findByText("Another own message");
    expect(realtime.playSound).toHaveBeenCalledOnce();
    expect(screen.getByRole("status", { name: "Incoming message notification" }).textContent).toContain("2 new messages");
    act(() => window.dispatchEvent(new Event("online")));
    await waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => url === "/api/messages?after=14").length).toBeGreaterThan(0));
    expect(realtime.playSound).toHaveBeenCalledOnce();
    expect(screen.getByRole("status", { name: "Incoming message notification" }).textContent).toContain("2 new messages");
  });

  it("does not silence a delayed new peer message after a newer own reply", async () => {
    let incoming: ChatMessage[] = [];
    fetchMock.mockImplementation(async (url: string) => url === "/api/read"
      ? json({ receipts: [] }) : json({ messages: incoming, hasMore: false }));
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("90071992547409930")]} initialReceipts={[]} />);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url === "/api/read")).toBe(true));
    incoming = [message("90071992547409932", "My newer reply", self.user_id)];
    act(() => window.dispatchEvent(new Event("online")));
    await screen.findByText("My newer reply");
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    incoming = [message("90071992547409931", "A delayed peer reply")];
    act(() => window.dispatchEvent(new Event("online")));
    await screen.findByText("A delayed peer reply");
    expect(realtime.playSound).toHaveBeenCalledOnce();
    expect(screen.getByRole("status", { name: "Incoming message notification" }).textContent).toContain("New message");
  });

  it("does not chime for initial or loaded history, including a repeated history page", async () => {
    const initial = Array.from({ length: 50 }, (_, index) => message(String(index + 50)));
    const history = [message("49", "An earlier note")];
    let repeatHistory = false;
    fetchMock.mockImplementation(async (url: string) => {
      if (url === "/api/read") return json({ receipts: [] });
      return json({ messages: url.includes("before=") || repeatHistory ? history : initial, hasMore: false });
    });
    render(<ChatClient currentMember={self} peer={peer} initialMessages={initial} initialReceipts={[]} />);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url === "/api/read")).toBe(true));
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Earlier messages" }));
    await screen.findByText("An earlier note");
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    repeatHistory = true;
    act(() => window.dispatchEvent(new Event("online")));
    await waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => url === "/api/read").length).toBeGreaterThan(1));
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
  });

  it("shows a private popup without stealing a draft or scroll position, and views it only on request", async () => {
    let incoming: ChatMessage[] = [];
    fetchMock.mockImplementation(async (url: string) => url === "/api/read"
      ? json({ receipts: [] }) : json({ messages: incoming, hasMore: false }));
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10")]} initialReceipts={[]} />);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url === "/api/read")).toBe(true));
    const input = screen.getByRole("textbox", { name: "Type a message" }) as HTMLTextAreaElement;
    const history = screen.getByRole("region", { name: "Message history" });
    const scrollTo = vi.fn();
    Object.defineProperties(history, {
      scrollHeight: { configurable: true, value: 1200 },
      clientHeight: { configurable: true, value: 400 },
      scrollTo: { configurable: true, value: scrollTo },
    });
    history.scrollTop = 100;
    fireEvent.scroll(history);
    input.focus();
    fireEvent.change(input, { target: { value: "Keep my Korean draft 안녕 👋" } });
    incoming = [message("11", "Private body stays in chat")];
    act(() => window.dispatchEvent(new Event("online")));
    const popup = await screen.findByRole("status", { name: "Incoming message notification" });
    expect(popup.textContent).toContain("WifeNew message");
    expect(popup.textContent).not.toContain("Private body stays in chat");
    expect(document.activeElement).toBe(input);
    expect(input.value).toBe("Keep my Korean draft 안녕 👋");
    expect(history.scrollTop).toBe(100);
    expect(scrollTo).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "View new messages" }));
    expect(scrollTo).toHaveBeenCalledWith({ top: 1200, behavior: "smooth" });
    expect(document.activeElement).toBe(input);
    expect(input.value).toBe("Keep my Korean draft 안녕 👋");
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
  });

  it("never queues a hidden or unfocused arrival to show later on return", async () => {
    let incoming: ChatMessage[] = [];
    fetchMock.mockImplementation(async (url: string) => url === "/api/read"
      ? json({ receipts: [] }) : json({ messages: incoming, hasMore: false }));
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10")]} initialReceipts={[]} />);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url === "/api/read")).toBe(true));
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
    incoming = [message("11", "A hidden arrival")];
    act(() => window.dispatchEvent(new Event("online")));
    await screen.findByText("A hidden arrival");
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
    vi.mocked(document.hasFocus).mockReturnValue(false);
    incoming = [message("11", "A hidden arrival"), message("12", "An unfocused arrival")];
    act(() => document.dispatchEvent(new Event("visibilitychange")));
    await screen.findByText("An unfocused arrival");
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    vi.mocked(document.hasFocus).mockReturnValue(true);
    act(() => window.dispatchEvent(new Event("online")));
    await waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => url === "/api/messages?after=12").length).toBeGreaterThan(0));
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    incoming = [message("13", "A fresh foreground arrival")];
    act(() => window.dispatchEvent(new Event("online")));
    await screen.findByRole("status", { name: "Incoming message notification" });
    fireEvent.click(screen.getByRole("button", { name: /Husband account/ }));
    fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
  });

  it("keeps a phone draft and the latest message in place when the keyboard resizes the viewport", async () => {
    const visual = Object.assign(new EventTarget(), { height: 780, offsetTop: 0, scale: 1 });
    vi.stubGlobal("visualViewport", visual);
    const view = render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10")]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox", { name: "Type a message" });
    const shell = input.closest<HTMLElement>(".chat-shell")!;
    const history = screen.getByRole("region", { name: "Message history" });
    Object.defineProperties(history, {
      scrollHeight: { configurable: true, value: 1200 },
      clientHeight: { configurable: true, value: 400 },
    });
    history.scrollTop = 800;
    fireEvent.scroll(history);
    fireEvent.change(input, { target: { value: "A phone draft 👋" } });
    act(() => {
      visual.height = 350;
      visual.offsetTop = 24;
      visual.dispatchEvent(new Event("resize"));
    });
    expect(shell.style.getPropertyValue("--chat-viewport-height")).toBe("350px");
    expect(shell.style.getPropertyValue("--chat-viewport-top")).toBe("24px");
    await waitFor(() => expect(history.scrollTop).toBe(1200));
    expect(screen.getByRole("textbox", { name: "Type a message" })).toBe(input);
    expect((input as HTMLTextAreaElement).value).toBe("A phone draft 👋");
    expect(posts()).toHaveLength(0);
    view.unmount();
    visual.height = 780;
    visual.dispatchEvent(new Event("resize"));
    expect(shell.style.getPropertyValue("--chat-viewport-height")).toBe("350px");
  });

  it("preserves the reader's position in older messages during a phone resize and allows browser zoom", async () => {
    const visual = Object.assign(new EventTarget(), { height: 780, offsetTop: 0, scale: 1 });
    vi.stubGlobal("visualViewport", visual);
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[message("10")]} initialReceipts={[]} />);
    const history = screen.getByRole("region", { name: "Message history" });
    Object.defineProperties(history, {
      scrollHeight: { configurable: true, value: 1200 },
      clientHeight: { configurable: true, value: 400 },
    });
    // Let the initial scroll settle before the user deliberately reads history.
    await waitFor(() => expect(history.scrollTop).toBe(1200));
    history.scrollTop = 100;
    fireEvent.scroll(history);
    act(() => { visual.height = 350; visual.dispatchEvent(new Event("resize")); });
    expect(history.scrollTop).toBe(100);
    act(() => { visual.scale = 2; visual.dispatchEvent(new Event("resize")); });
    const shell = history.closest<HTMLElement>(".chat-shell")!;
    expect(shell.style.getPropertyValue("--chat-viewport-height")).toBe("");
    expect(shell.style.getPropertyValue("--chat-viewport-top")).toBe("");
    expect(history.scrollTop).toBe(100);
    expect(posts()).toHaveLength(0);
  });

  it("waits for IME composition and Shift+Enter before sending a Unicode message", async () => {
    const body = "안녕하세요 👋".repeat(300);
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox", { name: "Type a message" });
    fireEvent.change(input, { target: { value: body } });
    fireEvent.compositionStart(input);
    fireEvent.keyDown(input, { key: "Enter", code: "Enter" });
    expect(posts()).toHaveLength(0);
    expect((input as HTMLTextAreaElement).value).toBe(body);
    fireEvent.compositionEnd(input);
    fireEvent.keyDown(input, { key: "Enter", shiftKey: true });
    expect(posts()).toHaveLength(0);
    fireEvent.keyDown(input, { key: "Enter" });
    await waitFor(() => expect(posts()).toHaveLength(1));
    await waitFor(() => expect(screen.getAllByText(body)).toHaveLength(1));
    expect((input as HTMLTextAreaElement).value).toBe("");
  });

  it("keeps the draft offline and sends it only after a connection returns", async () => {
    const body = "Keep this note 👋".repeat(200);
    networkOnline = false;
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox");
    fireEvent.change(input, { target: { value: body } });
    fireEvent.keyDown(input, { key: "Enter" });
    expect(posts()).toHaveLength(0);
    expect((input as HTMLTextAreaElement).value).toBe(body);
    expect((screen.getByRole("button", { name: "Send message" }) as HTMLButtonElement).disabled).toBe(true);
    act(() => { networkOnline = true; window.dispatchEvent(new Event("online")); });
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    await waitFor(() => expect(posts()).toHaveLength(1));
    expect(JSON.parse(String(posts()[0][1].body)).body).toBe(body);
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

  it.each(["a".repeat(2001), "안녕 👋\n".repeat(501).trim()])("sends long text unchanged without a character counter (%#)", async (body) => {
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox") as HTMLTextAreaElement;
    fireEvent.change(input, { target: { value: ` \n${body}\n ` } });
    expect(input.hasAttribute("maxlength")).toBe(false);
    expect((screen.getByRole("button", { name: "Send message" }) as HTMLButtonElement).disabled).toBe(false);
    expect(screen.queryByText(/\/ 2,000/)).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Send message" }));
    await waitFor(() => expect(posts()).toHaveLength(1));
    expect(JSON.parse(String(posts()[0][1].body)).body).toBe(body);
    await waitFor(() => expect(screen.getAllByText(body, { normalizer: (value) => value })).toHaveLength(1));
    expect(input.value).toBe("");
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
  });

  it("keeps empty and whitespace-only drafts unsendable", () => {
    render(<ChatClient currentMember={self} peer={peer} initialMessages={[]} initialReceipts={[]} />);
    const input = screen.getByRole("textbox");
    for (const body of ["", " \n\t ", "\u2003\u00a0"]) {
      fireEvent.change(input, { target: { value: body } });
      expect((screen.getByRole("button", { name: "Send message" }) as HTMLButtonElement).disabled).toBe(true);
      fireEvent.keyDown(input, { key: "Enter" });
    }
    expect(posts()).toHaveLength(0);
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
