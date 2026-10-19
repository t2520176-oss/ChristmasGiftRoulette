// @vitest-environment jsdom
import React from "react";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { CopyMessageButton } from "@/components/copy-message-button";

const originalClipboard = Object.getOwnPropertyDescriptor(navigator, "clipboard");
const originalShowModal = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "showModal");
const originalClose = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "close");

function restoreProperty(object: object, name: string, value: PropertyDescriptor | undefined) {
  if (value) Object.defineProperty(object, name, value);
  else Reflect.deleteProperty(object, name);
}

beforeEach(() => {
  vi.useFakeTimers();
  Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText: vi.fn().mockResolvedValue(undefined) } });
  Object.defineProperty(HTMLDialogElement.prototype, "showModal", { configurable: true, value: function(this: HTMLDialogElement) { this.open = true; } });
  Object.defineProperty(HTMLDialogElement.prototype, "close", { configurable: true, value: function(this: HTMLDialogElement) { this.open = false; this.dispatchEvent(new Event("close")); } });
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.restoreAllMocks();
  restoreProperty(navigator, "clipboard", originalClipboard);
  restoreProperty(HTMLDialogElement.prototype, "showModal", originalShowModal);
  restoreProperty(HTMLDialogElement.prototype, "close", originalClose);
});

describe("copy message", () => {
  it("copies exact Unicode, spaces, and line breaks directly from a click, with a transient announcement", async () => {
    const body = "  안녕 👨‍👩‍👧‍👦 café e\u0301\n\nSecond line\r\n 끝  ";
    render(<CopyMessageButton body={body} />);
    expect(navigator.clipboard.writeText).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
    // Invocation occurs synchronously, before a fulfilled Clipboard promise settles.
    expect(navigator.clipboard.writeText).toHaveBeenCalledExactlyOnceWith(body);
    expect(screen.queryByRole("button", { name: "Copied" })).toBeNull();
    await act(async () => {});
    expect(screen.getByRole("button", { name: "Copied" })).toBeTruthy();
    expect(screen.getByRole("status").textContent).toBe("Message copied.");
    act(() => vi.advanceTimersByTime(2000));
    expect(screen.getByRole("button", { name: "Copy message" })).toBeTruthy();
    expect(screen.getByRole("status").textContent).toBe("");
  });

  it("keeps very long messages intact and copies the selected message instance only", async () => {
    const body = "💚안녕하세요\n".repeat(15000);
    render(<><section aria-label="Own message"><CopyMessageButton body="My message" /></section><section aria-label="Peer message"><CopyMessageButton body={body} /></section></>);
    fireEvent.click(screen.getAllByRole("button", { name: "Copy message" })[1]);
    await act(async () => {});
    expect(navigator.clipboard.writeText).toHaveBeenCalledExactlyOnceWith(body);
    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
    await act(async () => {});
    expect(vi.mocked(navigator.clipboard.writeText).mock.calls[1][0]).toBe("My message");
  });

  it("does not claim success while a request is pending and ignores repeated clicks", async () => {
    let finish!: () => void;
    vi.mocked(navigator.clipboard.writeText).mockImplementation(() => new Promise<void>((resolve) => { finish = resolve; }));
    render(<CopyMessageButton body="Not yet copied" />);
    const button = screen.getByRole("button", { name: "Copy message" });
    fireEvent.click(button);
    fireEvent.click(button);
    expect(navigator.clipboard.writeText).toHaveBeenCalledOnce();
    expect(button.getAttribute("aria-busy")).toBe("true");
    expect(screen.queryByRole("button", { name: "Copied" })).toBeNull();
    await act(async () => finish());
    expect(screen.getByRole("button", { name: "Copied" }).getAttribute("aria-busy")).toBe("false");
  });

  it.each(["missing", "denied"])("selects the full message for truthful manual copying when clipboard is %s", async (failure) => {
    if (failure === "missing") Reflect.deleteProperty(navigator, "clipboard");
    else vi.mocked(navigator.clipboard.writeText).mockRejectedValue(new DOMException("Permission denied", "NotAllowedError"));
    const body = "First line 안녕 👋\n  Second line\n";
    render(<CopyMessageButton body={body} />);
    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
    await act(async () => {});
    expect(screen.getByRole("dialog", { name: "Copy message manually" })).toBeTruthy();
    expect(screen.getByText(/Automatic copy is unavailable/)).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Copied" })).toBeNull();
    const text = screen.getByRole("textbox", { name: "Message text to copy" }) as HTMLTextAreaElement;
    expect(text.readOnly).toBe(true);
    expect(text.value).toBe(body);
    expect(document.activeElement).toBe(text);
    expect(text.selectionStart).toBe(0);
    expect(text.selectionEnd).toBe(body.length);
    text.setSelectionRange(2, 5);
    fireEvent.click(screen.getByRole("button", { name: "Select message" }));
    expect(text.selectionStart).toBe(0);
    expect(text.selectionEnd).toBe(body.length);
  });

  it("restores the draft, caret, focus, and history position after manual copy closes", async () => {
    vi.mocked(navigator.clipboard.writeText).mockRejectedValue(new Error("blocked"));
    render(<div data-testid="history"><CopyMessageButton body="Copy this message" /><textarea aria-label="Draft" defaultValue="내 초안 stays intact 👋" /></div>);
    const draft = screen.getByRole("textbox", { name: "Draft" }) as HTMLTextAreaElement;
    const history = screen.getByTestId("history");
    history.scrollTop = 173;
    draft.focus();
    draft.setSelectionRange(2, 8, "backward");
    const button = screen.getByRole("button", { name: "Copy message" });
    fireEvent.pointerDown(button);
    fireEvent.mouseDown(button);
    fireEvent.click(button);
    await act(async () => {});
    expect(document.activeElement).toBe(screen.getByRole("textbox", { name: "Message text to copy" }));
    history.scrollTop = 999; // Browser focus/reflow must not change where reading resumes.
    fireEvent.click(screen.getByRole("button", { name: "Close manual copy" }));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(document.activeElement).toBe(draft);
    expect(draft.value).toBe("내 초안 stays intact 👋");
    expect([draft.selectionStart, draft.selectionEnd, draft.selectionDirection]).toEqual([2, 8, "backward"]);
    expect(history.scrollTop).toBe(173);
  });

  it("keeps a draft focused on a normal successful mouse copy", async () => {
    render(<><CopyMessageButton body="Copy, without editing" /><textarea aria-label="Draft" defaultValue="Keep this" /></>);
    const draft = screen.getByRole("textbox", { name: "Draft" }) as HTMLTextAreaElement;
    draft.focus();
    draft.setSelectionRange(2, 4);
    const button = screen.getByRole("button", { name: "Copy message" });
    fireEvent.pointerDown(button);
    expect(fireEvent.mouseDown(button)).toBe(false);
    fireEvent.click(button);
    await act(async () => {});
    expect(document.activeElement).toBe(draft);
    expect(draft.value).toBe("Keep this");
    expect([draft.selectionStart, draft.selectionEnd]).toEqual([2, 4]);
  });

  it("removes an active manual-copy dialog and restores the connected draft when a message expires", async () => {
    vi.mocked(navigator.clipboard.writeText).mockRejectedValue(new Error("blocked"));
    const content = (visible: boolean) => <div data-testid="history">{visible && <CopyMessageButton body="Expiring private message" />}<textarea aria-label="Draft" defaultValue="안녕 👋 saved draft" /></div>;
    const view = render(content(true));
    const draft = screen.getByRole("textbox", { name: "Draft" }) as HTMLTextAreaElement;
    const history = screen.getByTestId("history");
    history.scrollTop = 123;
    draft.focus();
    draft.setSelectionRange(2, 7, "backward");
    const button = screen.getByRole("button", { name: "Copy message" });
    fireEvent.pointerDown(button);
    fireEvent.click(button);
    await act(async () => {});
    expect(document.activeElement).toBe(screen.getByRole("textbox", { name: "Message text to copy" }));
    history.scrollTop = 999;
    view.rerender(content(false));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.queryByRole("textbox", { name: "Message text to copy" })).toBeNull();
    expect(document.body.textContent).not.toContain("Expiring private message");
    expect(document.activeElement).toBe(draft);
    expect(draft.value).toBe("안녕 👋 saved draft");
    expect([draft.selectionStart, draft.selectionEnd, draft.selectionDirection]).toEqual([2, 7, "backward"]);
    expect(history.scrollTop).toBe(123);
  });

  it("lets Escape close the fallback and clears confirmation timers on unmount", async () => {
    vi.mocked(navigator.clipboard.writeText).mockRejectedValueOnce(new Error("blocked"));
    const view = render(<CopyMessageButton body="Message" />);
    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
    await act(async () => {});
    fireEvent(screen.getByRole("dialog", { name: "Copy message manually" }), new Event("cancel", { cancelable: true }));
    expect(screen.queryByRole("dialog")).toBeNull();
    const startTimer = vi.spyOn(globalThis, "setTimeout");
    const stopTimer = vi.spyOn(globalThis, "clearTimeout");
    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
    await act(async () => {});
    const confirmationIndex = startTimer.mock.calls.findIndex(([, delay]) => delay === 2000);
    expect(confirmationIndex).toBeGreaterThanOrEqual(0);
    const confirmationTimer = startTimer.mock.results[confirmationIndex].value;
    view.unmount();
    expect(stopTimer).toHaveBeenCalledWith(confirmationTimer);
  });

  it("does not open a fallback or create timers after unmounting a pending request", async () => {
    let reject!: (reason: Error) => void;
    vi.mocked(navigator.clipboard.writeText).mockImplementation(() => new Promise<void>((_resolve, fail) => { reject = fail; }));
    const view = render(<CopyMessageButton body="Expired while copying" />);
    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));
    view.unmount();
    await act(async () => reject(new Error("blocked")));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(vi.getTimerCount()).toBe(0);
  });
});
