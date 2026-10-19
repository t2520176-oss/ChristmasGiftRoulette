// @vitest-environment jsdom
import React from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ChatClient from "@/components/chat/chat-client";
import type { ChatMessage } from "@/lib/types";
import { MESSAGE_RETENTION_MS } from "@/lib/message-retention";

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
  id, body, sender_id: sender, client_nonce: nonce, created_at: new Date(Date.now() - 60_000).toISOString(),
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

afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });

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

describe("seven-day message retention", () => {
  const serverNow = Date.parse("2026-10-26T12:00:00.000Z");
  const dated = (id: string, expiresIn: number, body = `Retained ${id}`): ChatMessage => ({
    ...message(id, body), created_at: new Date(serverNow - MESSAGE_RETENTION_MS + expiresIn).toISOString(),
  });
  const settle = async () => { await act(async () => {}); };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(serverNow);
  });

  it("uses the server's initial time and never renders or offers to copy a boundary-expired message", async () => {
    vi.setSystemTime("1999-01-01T00:00:00.000Z");
    const body = "아직 보관되는 긴 글 👋\n".repeat(350);
    render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("1", 0, "Expired exactly at 168 hours"), dated("2", 1, body)]} initialReceipts={[]} />);
    expect(screen.queryByText("Expired exactly at 168 hours")).toBeNull();
    expect(screen.getAllByRole("button", { name: "Copy message" })).toHaveLength(1);
    expect(screen.getByText(body, { normalizer: (value) => value })).toBeTruthy();
    await act(async () => vi.advanceTimersByTime(1));
    expect(screen.queryByText(body, { normalizer: (value) => value })).toBeNull();
    expect(screen.queryByRole("button", { name: "Copy message" })).toBeNull();
    expect(realtime.playSound).not.toHaveBeenCalled();
  });

  it("expires successive stored messages while open without editing a focused draft or marking expired content read", async () => {
    render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("1", 300), dated("2", 1000)]} initialReceipts={[]} />);
    await settle();
    const input = screen.getByRole("textbox", { name: "Type a message" }) as HTMLTextAreaElement;
    fireEvent.change(input, { target: { value: "내 초안은 그대로 👋" } });
    input.focus();
    act(() => TestObserver.instances.at(-1)!.reveal("1"));
    await act(async () => vi.advanceTimersByTime(500));
    expect(screen.queryByText("Retained 1")).toBeNull();
    expect(screen.getByText("Retained 2")).toBeTruthy();
    expect(fetchMock.mock.calls.filter(([url, options]) => url === "/api/read" && options?.method === "POST")).toHaveLength(0);
    await act(async () => vi.advanceTimersByTime(500));
    expect(screen.queryByText("Retained 2")).toBeNull();
    expect(input.value).toBe("내 초안은 그대로 👋");
    expect(document.activeElement).toBe(input);
    expect(posts()).toHaveLength(0);
  });

  it("prunes on an offline hidden-page return even when browser timers were suspended", async () => {
    render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("1", 1000)]} initialReceipts={[]} />);
    await settle();
    const requestsBefore = fetchMock.mock.calls.length;
    act(() => {
      networkOnline = false;
      window.dispatchEvent(new Event("offline"));
      Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
      document.dispatchEvent(new Event("visibilitychange"));
    });
    // setSystemTime deliberately does not run timers or performance.now().
    vi.setSystemTime(serverNow + 2000);
    act(() => {
      Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
      document.dispatchEvent(new Event("visibilitychange"));
      window.dispatchEvent(new Event("focus"));
    });
    expect(screen.queryByText("Retained 1")).toBeNull();
    expect(fetchMock.mock.calls).toHaveLength(requestsBefore);
  });

  it("removes a manual-copy dialog with the expired message instead of retaining a second text copy", async () => {
    const originalClipboard = Object.getOwnPropertyDescriptor(navigator, "clipboard");
    const originalShowModal = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "showModal");
    const originalClose = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "close");
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: undefined });
    Object.defineProperty(HTMLDialogElement.prototype, "showModal", {
      configurable: true, value: function(this: HTMLDialogElement) { this.open = true; },
    });
    Object.defineProperty(HTMLDialogElement.prototype, "close", {
      configurable: true, value: function(this: HTMLDialogElement) { this.open = false; },
    });
    try {
      render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
        initialMessages={[dated("1", 1000, "This text must expire everywhere")] } initialReceipts={[]} />);
      await settle();
      const composer = screen.getByRole("textbox", { name: "Type a message" }) as HTMLTextAreaElement;
      fireEvent.change(composer, { target: { value: "Keep this draft" } });
      composer.focus();
      composer.setSelectionRange(2, 6);
      fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
      await settle();
      expect(screen.getByRole("dialog", { name: "Copy message manually" })).toBeTruthy();
      expect((screen.getByRole("textbox", { name: "Message text to copy" }) as HTMLTextAreaElement).value).toBe("This text must expire everywhere");
      await act(async () => vi.advanceTimersByTime(1000));
      expect(screen.queryByRole("dialog", { name: "Copy message manually" })).toBeNull();
      expect(screen.queryByRole("textbox", { name: "Message text to copy" })).toBeNull();
      expect(screen.queryByText("This text must expire everywhere")).toBeNull();
      expect(document.activeElement).toBe(composer);
      expect(composer.value).toBe("Keep this draft");
      expect([composer.selectionStart, composer.selectionEnd]).toEqual([2, 6]);
    } finally {
      if (originalClipboard) Object.defineProperty(navigator, "clipboard", originalClipboard);
      else Reflect.deleteProperty(navigator, "clipboard");
      if (originalShowModal) Object.defineProperty(HTMLDialogElement.prototype, "showModal", originalShowModal);
      else Reflect.deleteProperty(HTMLDialogElement.prototype, "showModal");
      if (originalClose) Object.defineProperty(HTMLDialogElement.prototype, "close", originalClose);
      else Reflect.deleteProperty(HTMLDialogElement.prototype, "close");
    }
  });

  it("filters expired incoming batches before sound, popup, unread counts, or history merge", async () => {
    let incoming: ChatMessage[] = [];
    fetchMock.mockImplementation(async (url: string) => url === "/api/read"
      ? json({ receipts: [] }) : json({ messages: incoming, hasMore: false }));
    render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("10", 60_000)]} initialReceipts={[]} />);
    await settle();
    const history = screen.getByRole("region", { name: "Message history" });
    Object.defineProperties(history, {
      scrollHeight: { configurable: true, value: 1200 }, clientHeight: { configurable: true, value: 400 },
    });
    history.scrollTop = 100;
    fireEvent.scroll(history);
    incoming = [dated("11", 0, "A delayed, expired peer message")];
    act(() => window.dispatchEvent(new Event("online")));
    await settle();
    expect(screen.queryByText("A delayed, expired peer message")).toBeNull();
    expect(screen.getAllByRole("button", { name: "Copy message" })).toHaveLength(1);
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
    expect(screen.getByRole("button", { name: "Back to latest" })).toBeTruthy();
  });

  it("preserves failed and in-flight unsent messages and a new draft after stored history expires", async () => {
    let sends = 0;
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (url === "/api/messages" && options?.method === "POST") {
        sends += 1;
        if (sends === 1) return new Promise<Response>(() => {});
        throw new TypeError("Disconnected");
      }
      return url === "/api/read" ? json({ receipts: [] }) : json({ messages: [], hasMore: false });
    });
    const view = render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("1", 1000)]} initialReceipts={[]} />);
    await settle();
    const input = screen.getByRole("textbox", { name: "Type a message" }) as HTMLTextAreaElement;
    for (const body of ["Still sending", "Failed, keep for retry"]) {
      fireEvent.change(input, { target: { value: body } });
      fireEvent.click(screen.getByRole("button", { name: "Send message" }));
      await settle();
    }
    fireEvent.change(input, { target: { value: "An unsent draft" } });
    act(() => { networkOnline = false; window.dispatchEvent(new Event("offline")); });
    vi.setSystemTime(serverNow + MESSAGE_RETENTION_MS + 1);
    act(() => window.dispatchEvent(new Event("focus")));
    expect(screen.queryByText("Retained 1")).toBeNull();
    expect(screen.getByText("Still sending")).toBeTruthy();
    expect(screen.getByText("Failed, keep for retry")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Retry failed message" })).toBeTruthy();
    expect(input.value).toBe("An unsent draft");
    view.unmount();
  });

  it("resynchronizes from a response Date before processing messages", async () => {
    let responseNow = serverNow;
    fetchMock.mockImplementation(async (url: string) => new Response(JSON.stringify(url === "/api/read"
      ? { receipts: [] } : { messages: [dated("11", 1000, "Now expired at the server")], hasMore: false }), {
      status: 200, headers: { "Content-Type": "application/json", Date: new Date(responseNow).toUTCString() },
    }));
    render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("10", 1000)]} initialReceipts={[]} />);
    await settle();
    expect(screen.getByText("Now expired at the server")).toBeTruthy();
    realtime.playSound.mockClear();
    fireEvent.click(screen.getByRole("button", { name: "Dismiss message notification" }));
    responseNow += 2000;
    act(() => window.dispatchEvent(new Event("online")));
    await settle();
    expect(screen.queryByText("Retained 10")).toBeNull();
    expect(screen.queryByText("Now expired at the server")).toBeNull();
    expect(realtime.playSound).not.toHaveBeenCalled();
    expect(screen.queryByRole("status", { name: "Incoming message notification" })).toBeNull();
  });

  it("does not extend retention when the device clock moves backward and stops expiry timers after unmount", async () => {
    const view = render(<ChatClient currentMember={self} peer={peer} initialServerTime={serverNow}
      initialMessages={[dated("1", 1000)]} initialReceipts={[]} />);
    await settle();
    vi.setSystemTime(serverNow - MESSAGE_RETENTION_MS);
    await act(async () => vi.advanceTimersByTime(1000));
    expect(screen.queryByText("Retained 1")).toBeNull();
    view.unmount();
    const calls = fetchMock.mock.calls.length;
    await act(async () => vi.advanceTimersByTime(MESSAGE_RETENTION_MS));
    expect(fetchMock.mock.calls).toHaveLength(calls);
    expect(vi.getTimerCount()).toBe(0);
  });
});
