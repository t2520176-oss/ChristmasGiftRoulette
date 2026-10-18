// @vitest-environment jsdom
import React from "react";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { IncomingMessageNotice, useIncomingMessageNotice } from "@/components/incoming-message-notice";

function NoticeTest({ onView = () => {} }: { onView?: () => void }) {
  const notifications = useIncomingMessageNotice();
  return <>
    <input aria-label="Draft" defaultValue="Keep this draft" />
    <button onClick={() => notifications.show(1)}>One arrival</button>
    <button onClick={() => notifications.show(2)}>Two arrivals</button>
    <IncomingMessageNotice notifications={notifications} senderName="Wife" onView={() => { notifications.dismiss(); onView(); }} />
  </>;
}
const notice = () => screen.queryByRole("status", { name: "Incoming message notification" });
const advance = (milliseconds: number) => act(() => vi.advanceTimersByTime(milliseconds));

beforeEach(() => {
  vi.useFakeTimers();
  Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
  vi.spyOn(document, "hasFocus").mockReturnValue(true);
});
afterEach(() => { cleanup(); vi.useRealTimers(); vi.restoreAllMocks(); });

describe("incoming message notice", () => {
  it("announces a generic count without changing focus or the draft", () => {
    render(<NoticeTest />);
    const input = screen.getByRole("textbox", { name: "Draft" });
    input.focus();
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    expect(notice()?.textContent).toContain("WifeNew message");
    expect(notice()?.getAttribute("aria-live")).toBe("polite");
    expect(document.activeElement).toBe(input);
    expect((input as HTMLInputElement).value).toBe("Keep this draft");
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getByRole("button", { name: "Dismiss message notification" }).className).toContain("size-11");
    expect(screen.getByRole("button", { name: "View new messages" }).className).toContain("min-h-11");
  });

  it("aggregates an active notice and restarts its six second timeout for a fresh batch", () => {
    render(<NoticeTest />);
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    advance(5000);
    fireEvent.click(screen.getByRole("button", { name: "Two arrivals" }));
    expect(notice()?.textContent).toContain("3 new messages");
    advance(5999);
    expect(notice()).not.toBeNull();
    advance(1);
    expect(notice()).toBeNull();
  });

  it("pauses the remaining timeout while hovered and resumes when the pointer leaves", () => {
    render(<NoticeTest />);
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    advance(2000);
    fireEvent.mouseEnter(notice()!);
    advance(20000);
    expect(notice()).not.toBeNull();
    fireEvent.mouseLeave(notice()!);
    advance(3999);
    expect(notice()).not.toBeNull();
    advance(1);
    expect(notice()).toBeNull();
  });

  it("pauses while either action has focus, including a fresh batch and focus moving between actions", () => {
    render(<NoticeTest />);
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    const view = screen.getByRole("button", { name: "View new messages" });
    const close = screen.getByRole("button", { name: "Dismiss message notification" });
    fireEvent.focus(view);
    advance(20000);
    fireEvent.blur(view, { relatedTarget: close });
    fireEvent.focus(close);
    fireEvent.click(screen.getByRole("button", { name: "Two arrivals" }));
    advance(20000);
    expect(notice()?.textContent).toContain("3 new messages");
    fireEvent.blur(close, { relatedTarget: screen.getByRole("textbox") });
    advance(5999);
    expect(notice()).not.toBeNull();
    advance(1);
    expect(notice()).toBeNull();
  });

  it("dismisses explicitly and starts the next notice with a fresh count", () => {
    render(<NoticeTest />);
    fireEvent.click(screen.getByRole("button", { name: "Two arrivals" }));
    fireEvent.click(screen.getByRole("button", { name: "Dismiss message notification" }));
    expect(notice()).toBeNull();
    expect(vi.getTimerCount()).toBe(0);
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    expect(notice()?.textContent).toContain("New message");
    expect(notice()?.textContent).not.toContain("3 new messages");
  });

  it("only runs the view action after a deliberate click", () => {
    const onView = vi.fn();
    render(<NoticeTest onView={onView} />);
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    expect(onView).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "View new messages" }));
    expect(onView).toHaveBeenCalledOnce();
    expect(notice()).toBeNull();
  });

  it("suppresses hidden and unfocused arrivals and clears an active notice on either transition", () => {
    render(<NoticeTest />);
    const arrive = screen.getByRole("button", { name: "One arrival" });
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
    fireEvent.click(arrive);
    expect(notice()).toBeNull();
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
    vi.mocked(document.hasFocus).mockReturnValue(false);
    fireEvent.click(arrive);
    expect(notice()).toBeNull();
    vi.mocked(document.hasFocus).mockReturnValue(true);
    fireEvent.click(arrive);
    act(() => window.dispatchEvent(new Event("blur")));
    expect(notice()).toBeNull();
    fireEvent.click(arrive);
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
    act(() => document.dispatchEvent(new Event("visibilitychange")));
    expect(notice()).toBeNull();
    expect(vi.getTimerCount()).toBe(0);
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
    act(() => document.dispatchEvent(new Event("visibilitychange")));
    expect(notice()).toBeNull();
  });

  it("removes the timeout and listeners when the chat unmounts", () => {
    const view = render(<NoticeTest />);
    fireEvent.click(screen.getByRole("button", { name: "One arrival" }));
    expect(vi.getTimerCount()).toBe(1);
    view.unmount();
    expect(vi.getTimerCount()).toBe(0);
    act(() => { window.dispatchEvent(new Event("blur")); document.dispatchEvent(new Event("visibilitychange")); });
    advance(10000);
  });
});
