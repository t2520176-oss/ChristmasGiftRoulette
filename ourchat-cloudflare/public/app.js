"use strict";

const $ = (id) => document.getElementById(id);
const KEEP_MS = 168 * 60 * 60 * 1000;
const MAX_TIMEOUT = 2147483000;
const MAX_PHOTO_INPUT = 20 * 1024 * 1024;
const MAX_PHOTO_BYTES = 128 * 1024;
const MAX_REQUEST_BYTES = 4 * 1024 * 1024;
const state = {
  member: null, peer: null, messages: [], receipts: [], nodes: new Map(),
  seen: new Set(), cursor: "0", initialBoundary: "0", initialized: false,
  hasMore: false, loadingOlder: false, syncing: false, syncAgain: false,
  generation: 0, senders: new Set(), visible: new Set(), lastRead: "0", reading: false,
  pollTimer: null, expiryTimer: null, noticeTimer: null, readTimer: null,
  composing: false, unread: 0, copyTarget: null, copyRestore: null, copyRevision: 0,
  sound: false, audio: null, registration: null, pushAvailable: false, pushRegistered: false, lastPresence: 0,
  photo: null, photoRevision: 0, objectUrls: new Set(), uploads: new Set(), viewer: null,
  clock: { server: Date.now(), mono: performance.now(), wall: Date.now(), last: Date.now() },
};
const compare = (a, b) => {
  const left = String(a).replace(/^0+/, "") || "0";
  const right = String(b).replace(/^0+/, "") || "0";
  return left.length - right.length || left.localeCompare(right);
};
const validId = (id) => typeof id === "string" && /^[1-9]\d*$/.test(id);
const newest = (rows) => rows.reduce((id, message) => validId(message.id) && compare(message.id, id) > 0 ? message.id : id, "0");
const oldest = () => state.messages.filter((message) => validId(message.id)).reduce((id, message) => id === null || compare(message.id, id) < 0 ? message.id : id, null);
const retained = (message) => !validId(message.id) || Date.parse(message.created_at) > now() - KEEP_MS;
const visiblePage = () => document.visibilityState === "visible";
const atBottom = () => $("messages").scrollHeight - $("messages").scrollTop - $("messages").clientHeight < 90;
const draftKey = () => `ourchat-draft:${state.member?.id || ""}`;
function announce(text) { $("announcement").textContent = text; }
function now() {
  const clock = state.clock;
  clock.last = Math.max(clock.last, clock.server + Math.max(0, performance.now() - clock.mono, Date.now() - clock.wall));
  return clock.last;
}
function anchor(value) {
  const server = typeof value === "number" ? value : Date.parse(value);
  if (!Number.isFinite(server)) return;
  state.clock = { server, mono: performance.now(), wall: Date.now(), last: server };
}
class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}
function responseError(status, data) {
  let message = "Something went wrong. Please try again.";
  if (status === 401) message = "Please sign in again.";
  else if (status === 413) message = typeof data?.error === "string" ? data.error.slice(0, 300) : "This message is too large to transfer in one request. Please split it and try again.";
  else if (status === 429) message = "Please wait a moment and try again.";
  else if (status === 503) message = "This OurChat server is not ready yet. Please try again after setup is complete.";
  else if (data && typeof data.error === "string") message = data.error.slice(0, 300);
  return new ApiError(message, status);
}
async function api(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20000);
  const headers = new Headers(options.headers);
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  try {
    const response = await fetch(path, { ...options, headers, credentials: "same-origin", cache: "no-store", signal: controller.signal });
    const data = await response.json().catch(() => null);
    if (!response.ok) throw responseError(response.status, data);
    if (data && Number.isFinite(data.serverTime)) anchor(data.serverTime);
    else if (response.headers.get("Date")) anchor(response.headers.get("Date"));
    return data;
  } catch (error) {
    if (controller.signal.aborted) throw new ApiError("The connection took too long. Please try again.", 0);
    if (error instanceof ApiError) throw error;
    throw new ApiError("Unable to connect. Check your internet connection and try again.", 0);
  } finally { clearTimeout(timeout); }
}
const post = (path, data) => api(path, { method: "POST", body: JSON.stringify(data) });
function setError(message) { $("chat-error").textContent = message || ""; $("chat-error").hidden = !message; }
function setConnection(message) { $("connection").textContent = message || ""; $("connection").hidden = !message; }
function autosize() {
  const input = $("draft");
  input.style.height = "auto";
  input.style.height = `${Math.min(input.scrollHeight, Math.min(innerHeight * .27, 180))}px`;
  $("send").disabled = state.photo?.status === "preparing" || (!input.value.trim() && state.photo?.status !== "ready");
}
function objectUrl(blob) {
  const url = URL.createObjectURL(blob); state.objectUrls.add(url); return url;
}
function revokeUrl(url) {
  if (url && state.objectUrls.delete(url)) URL.revokeObjectURL(url);
}
function photoError(message = "") {
  $("photo-error").textContent = message; $("photo-error").hidden = !message;
}
function clearPhoto({ release = true } = {}) {
  ++state.photoRevision;
  if (release) revokeUrl(state.photo?.url);
  state.photo = null;
  $("photo-input").value = ""; $("photo-preview").removeAttribute("src"); $("photo-preview").hidden = true;
  $("photo-selection").hidden = true; $("photo-status").textContent = "";
  autosize();
}
async function decodePhoto(file, source) {
  // Browsers apply EXIF orientation while decoding; drawing strips the original metadata.
  if (typeof createImageBitmap === "function") {
    try { return await createImageBitmap(file); } catch { /* Some browser-supported formats only decode through Image. */ }
  }
  const image = new Image(); image.decoding = "async";
  await new Promise((resolve, reject) => { image.onload = resolve; image.onerror = reject; image.src = source; });
  return image;
}
const jpegBlob = (canvas, quality) => new Promise((resolve, reject) => canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error("encoding")), "image/jpeg", quality));
const photoBase64 = (blob) => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = () => resolve(String(reader.result).split(",")[1]);
  reader.onerror = reject; reader.readAsDataURL(blob);
});
async function preparePhoto(file) {
  if (!state.member || !file) return;
  clearPhoto(); photoError();
  if (file.size > MAX_PHOTO_INPUT) { photoError("Choose a photo smaller than 20 MiB."); return; }
  const revision = state.photoRevision;
  const source = objectUrl(file);
  state.photo = { url: source, status: "preparing" };
  $("photo-selection").hidden = false; $("photo-status").textContent = "Preparing photo…"; autosize();
  let decoded;
  try {
    decoded = await decodePhoto(file, source);
    const width = decoded.width || decoded.naturalWidth, height = decoded.height || decoded.naturalHeight;
    if (!width || !height) throw new Error("decoding");
    const scale = Math.min(1, 1600 / Math.max(width, height));
    let targetWidth = Math.max(1, Math.round(width * scale)), targetHeight = Math.max(1, Math.round(height * scale));
    const canvas = document.createElement("canvas");
    let blob;
    for (;;) {
      if (revision !== state.photoRevision) return;
      canvas.width = targetWidth; canvas.height = targetHeight;
      const context = canvas.getContext("2d");
      if (!context) throw new Error("encoding");
      context.fillStyle = "#fff"; context.fillRect(0, 0, targetWidth, targetHeight);
      context.drawImage(decoded, 0, 0, targetWidth, targetHeight);
      for (const quality of [.88, .78, .68, .58, .48]) {
        blob = await jpegBlob(canvas, quality);
        if (blob.type !== "image/jpeg") throw new Error("encoding");
        if (blob.size <= MAX_PHOTO_BYTES) break;
      }
      if (blob.size <= MAX_PHOTO_BYTES) break;
      if (targetWidth === 1 && targetHeight === 1) throw new Error("encoding");
      targetWidth = Math.max(1, Math.floor(targetWidth * .8)); targetHeight = Math.max(1, Math.floor(targetHeight * .8));
    }
    const data = await photoBase64(blob);
    if (revision !== state.photoRevision || !state.member) return;
    const url = objectUrl(blob); revokeUrl(source);
    state.photo = { url, data, width: targetWidth, height: targetHeight, bytes: blob.size, status: "ready" };
    $("photo-preview").src = url; $("photo-preview").hidden = false;
    $("photo-status").textContent = "Photo ready. Add a caption if you like.";
    announce("Photo ready to send."); autosize();
  } catch {
    if (revision !== state.photoRevision) return;
    clearPhoto();
    photoError(/hei[cf]/i.test(`${file.type} ${file.name}`) ? "This browser cannot open this HEIC photo. Choose a JPG or PNG photo instead." : "This photo could not be opened. Choose a JPG, PNG, or another photo your browser supports.");
  } finally {
    decoded?.close?.();
    if (state.photo?.url !== source) revokeUrl(source);
  }
}
function imageSource(message) {
  const image = message.image;
  if (!image || image.mimeType !== "image/jpeg" || !Number.isInteger(image.width) || !Number.isInteger(image.height) || image.width < 1 || image.height < 1 || image.width > 1600 || image.height > 1600) return null;
  if (message._photoUrl && image.url === message._photoUrl && state.objectUrls.has(image.url)) return image.url;
  // API image paths are fixed, same-origin identifiers; server-supplied external URLs are never followed.
  return typeof image.url === "string" && /^\/api\/images\/[A-Za-z0-9_-]+$/.test(image.url) ? image.url : null;
}
function closeViewer() {
  if ($("photo-dialog").open) $("photo-dialog").close();
  $("viewer-photo").removeAttribute("src"); $("save-photo").removeAttribute("href");
  state.viewer = null;
}
function viewPhoto(message) {
  const source = imageSource(message);
  if (!source || !retained(message) || !state.nodes.has(message.id)) { prune(); return; }
  state.viewer = { id: message.id, restore: rememberFocus() };
  $("viewer-photo").src = source; $("viewer-photo").width = message.image.width; $("viewer-photo").height = message.image.height;
  $("save-photo").href = source; openDialog($("photo-dialog"));
}
function releaseMessage(message) {
  if (state.viewer?.id === message.id) closeViewer();
  revokeUrl(message._photoUrl);
  delete message._payload; delete message._photoUrl;
}
function uploadPhoto(message) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest(); state.uploads.add(xhr);
    const finish = (error, data) => { state.uploads.delete(xhr); error ? reject(error) : resolve(data); };
    xhr.open("POST", "/api/messages"); xhr.withCredentials = true; xhr.timeout = 20000;
    xhr.responseType = "json"; xhr.setRequestHeader("Content-Type", "application/json");
    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) return;
      message.uploadProgress = Math.min(100, Math.round(event.loaded / event.total * 100));
      const row = state.nodes.get(message.id); if (row) updateRow(row, message);
    };
    xhr.onload = () => {
      const data = xhr.response;
      if (xhr.status < 200 || xhr.status >= 300) { finish(responseError(xhr.status, data)); return; }
      if (data && Number.isFinite(data.serverTime)) anchor(data.serverTime);
      else if (xhr.getResponseHeader("Date")) anchor(xhr.getResponseHeader("Date"));
      finish(null, data);
    };
    xhr.onerror = () => finish(new ApiError("Unable to connect. Check your internet connection and try again.", 0));
    xhr.ontimeout = () => finish(new ApiError("The connection took too long. Please try again.", 0));
    xhr.onabort = () => finish(new ApiError("Photo upload was cancelled.", 0));
    xhr.send(message._payload);
  });
}
function viewport() {
  const visual = window.visualViewport;
  document.documentElement.style.setProperty("--vh", `${visual?.height || innerHeight}px`);
  document.documentElement.style.setProperty("--vt", `${visual?.offsetTop || 0}px`);
}
function saveDraft() {
  if (!state.member) return;
  try {
    if ($("draft").value) sessionStorage.setItem(draftKey(), $("draft").value);
    else sessionStorage.removeItem(draftKey());
  } catch { /* The conversation still works when browser storage is unavailable. */ }
}
function rememberFocus() {
  const element = document.activeElement;
  return { element, start: typeof element?.selectionStart === "number" ? element.selectionStart : null,
    end: typeof element?.selectionEnd === "number" ? element.selectionEnd : null,
    direction: element?.selectionDirection, scroll: $("messages").scrollTop };
}
function restoreFocus(saved) {
  if (!saved?.element?.isConnected) return;
  saved.element.focus({ preventScroll: true });
  if (saved.start !== null && typeof saved.element.setSelectionRange === "function") {
    try { saved.element.setSelectionRange(saved.start, saved.end, saved.direction); } catch { /* Some inputs do not expose text selection. */ }
  }
  $("messages").scrollTop = saved.scroll;
}
function openDialog(dialog) {
  if (!dialog.open) dialog.showModal();
}
function scrollBottom() {
  $("messages").scrollTop = $("messages").scrollHeight;
  state.unread = 0; $("jump-bottom").hidden = true;
  scheduleRead();
}
function dismissNotice() {
  clearTimeout(state.noticeTimer); $("incoming-notice").hidden = true;
}
function incomingNotice(count) {
  if (!visiblePage()) return;
  $("incoming-notice").querySelector("p").textContent = count > 1 ? `You have ${count} new messages.` : "You have a new message.";
  $("incoming-notice").hidden = false;
  clearTimeout(state.noticeTimer);
  state.noticeTimer = setTimeout(dismissNotice, 8000);
}
function playIncoming() {
  if (!state.sound || !visiblePage() || !state.audio || state.audio.state !== "running") return;
  const audio = state.audio;
  const oscillator = audio.createOscillator(), gain = audio.createGain();
  oscillator.type = "sine"; oscillator.frequency.setValueAtTime(784, audio.currentTime);
  oscillator.frequency.setValueAtTime(1046.5, audio.currentTime + .09);
  gain.gain.setValueAtTime(0, audio.currentTime);
  gain.gain.linearRampToValueAtTime(.07, audio.currentTime + .015);
  gain.gain.exponentialRampToValueAtTime(.001, audio.currentTime + .25);
  oscillator.connect(gain); gain.connect(audio.destination);
  oscillator.start(); oscillator.stop(audio.currentTime + .27);
  oscillator.onended = () => { oscillator.disconnect(); gain.disconnect(); };
}
function closeCopy() {
  ++state.copyRevision;
  state.copyTarget = null;
  if ($("copy-dialog").open) $("copy-dialog").close();
  $("copy-text").value = "";
}
function manualCopy(message, saved) {
  if (!retained(message) || !state.nodes.has(message.id)) return;
  state.copyTarget = message.id;
  state.copyRestore = saved;
  $("copy-text").value = message.body;
  openDialog($("copy-dialog"));
  $("copy-text").focus({ preventScroll: true });
  $("copy-text").select();
  $("copy-text").setSelectionRange(0, message.body.length);
  announce("Automatic copying is unavailable. The entire message is selected for manual copying.");
}
function copyMessage(message, button, previousFocus) {
  const revision = ++state.copyRevision;
  const saved = previousFocus || rememberFocus();
  if (!retained(message)) { prune(); return; }
  // Call the clipboard API immediately inside the trusted click, before awaiting anything.
  if (!navigator.clipboard?.writeText) { manualCopy(message, saved); return; }
  let result;
  try { result = navigator.clipboard.writeText(message.body); }
  catch { manualCopy(message, saved); return; }
  Promise.resolve(result).then(() => {
    if (revision !== state.copyRevision || !button.isConnected || !retained(message)) return;
    button.textContent = "Copied";
    announce("Message copied.");
    restoreFocus(saved);
    setTimeout(() => { if (button.isConnected) button.textContent = "Copy message"; }, 2000);
  }, () => {
    if (revision === state.copyRevision && button.isConnected) manualCopy(message, saved);
  });
}
function makeRow(message) {
  const row = document.createElement("article");
  row.className = `message-row${message.sender_id === state.member.id ? " own" : ""}`;
  row.dataset.messageId = message.id;
  const bubble = document.createElement("div"); bubble.className = "message-bubble";
  const body = document.createElement("p"); body.className = "message-body"; body.textContent = message.body;
  const photo = document.createElement("button"); photo.type = "button"; photo.className = "message-photo"; photo.setAttribute("aria-label", "Open photo");
  const image = document.createElement("img"); image.alt = "Shared photo"; image.loading = "lazy"; image.decoding = "async";
  photo.append(image); photo.addEventListener("click", () => viewPhoto(row._message));
  bubble.append(photo, body);
  const meta = document.createElement("div"); meta.className = "message-meta";
  const copy = document.createElement("button"); copy.type = "button"; copy.className = "copy-button"; copy.textContent = "Copy message";
  copy.addEventListener("pointerdown", () => { row._copySaved = rememberFocus(); });
  copy.addEventListener("click", () => { copyMessage(row._message, copy, row._copySaved); row._copySaved = null; });
  const time = document.createElement("time"); time.className = "message-time";
  const status = document.createElement("span"); status.className = "message-state";
  const retry = document.createElement("button"); retry.type = "button"; retry.className = "retry-button"; retry.textContent = "Not sent · Retry";
  retry.addEventListener("click", () => sendMessage(row._message));
  meta.append(copy, time, status, retry); row.append(bubble, meta);
  row._elements = { body, time, status, retry, copy, photo, image };
  return row;
}
function updateRow(row, message) {
  row._message = message;
  const { body, time, status, retry, copy, photo, image } = row._elements;
  if (body.textContent !== message.body) body.textContent = message.body;
  body.hidden = !message.body; copy.hidden = !message.body;
  const source = imageSource(message);
  row.querySelector(".message-bubble").classList.toggle("has-photo", Boolean(source));
  photo.hidden = !source;
  if (source) {
    image.width = message.image.width; image.height = message.image.height;
    photo.style.aspectRatio = `${message.image.width} / ${message.image.height}`;
    if (image.getAttribute("src") !== source) image.src = source;
  } else image.removeAttribute("src");
  time.dateTime = message.created_at;
  time.textContent = new Date(message.created_at).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  const own = message.sender_id === state.member.id;
  const peerReceipt = state.receipts.find((receipt) => receipt.user_id !== state.member.id)?.last_read_message_id;
  status.textContent = message.status === "sending" ? message._photoUrl ? message.uploadProgress === 100 ? "Finishing…" : `Uploading photo… ${message.uploadProgress || 0}%` : "Sending…" : validId(message.id) && own ? peerReceipt && compare(peerReceipt, message.id) >= 0 ? "✓✓" : "✓" : "";
  status.setAttribute("aria-label", status.textContent === "✓✓" ? "Read" : status.textContent === "✓" ? "Sent" : status.textContent);
  retry.hidden = message.status !== "failed";
}
const messageObserver = new IntersectionObserver((entries) => {
  for (const entry of entries) {
    const id = entry.target.dataset.messageId;
    if (entry.isIntersecting && validId(id)) state.visible.add(id); else state.visible.delete(id);
  }
  scheduleRead();
}, { root: $("messages"), threshold: 0 });
function render({ prepend = false, forceBottom = false } = {}) {
  const viewport = $("messages"), wasBottom = atBottom();
  const oldHeight = viewport.scrollHeight, oldTop = viewport.scrollTop;
  const keep = new Set(state.messages.map((message) => message.id));
  for (const [id, row] of state.nodes) if (!keep.has(id)) {
    row._elements.image.removeAttribute("src");
    messageObserver.unobserve(row); row.remove(); state.nodes.delete(id); state.visible.delete(id);
  }
  const list = $("message-list");
  let previous = null;
  for (const message of state.messages) {
    let row = state.nodes.get(message.id);
    if (!row) { row = makeRow(message); state.nodes.set(message.id, row); messageObserver.observe(row); }
    updateRow(row, message);
    // Leave unchanged rows in place so selection, copy buttons and scroll do not jump on polling.
    const expected = previous ? previous.nextElementSibling : list.firstElementChild;
    if (expected !== row) list.insertBefore(row, expected);
    previous = row;
  }
  $("empty").hidden = state.messages.length !== 0;
  $("load-older").hidden = !state.hasMore;
  $("load-older").disabled = state.loadingOlder;
  $("load-older").textContent = state.loadingOlder ? "Loading…" : "Load older messages";
  if (prepend) viewport.scrollTop = oldTop + viewport.scrollHeight - oldHeight;
  else if (forceBottom || wasBottom) scrollBottom();
  else viewport.scrollTop = oldTop;
  armExpiry();
}
function armExpiry() {
  clearTimeout(state.expiryTimer);
  const expiries = state.messages.filter((message) => validId(message.id)).map((message) => Date.parse(message.created_at) + KEEP_MS);
  if (expiries.length) state.expiryTimer = setTimeout(prune, Math.min(MAX_TIMEOUT, Math.max(1, Math.min(...expiries) - now())));
}
function prune() {
  const next = state.messages.filter(retained);
  for (const message of state.messages) if (!next.includes(message)) releaseMessage(message);
  const clearOlder = state.hasMore && state.messages.some((message) => validId(message.id)) && !next.some((message) => validId(message.id));
  if (clearOlder) state.hasMore = false;
  if (state.copyTarget && !next.some((message) => message.id === state.copyTarget)) closeCopy();
  if (next.length !== state.messages.length || clearOlder) {
    state.messages = next; state.unread = 0; $("jump-bottom").hidden = true; dismissNotice(); render();
  } else armExpiry();
}
function accept(incoming, { notify = false, prepend = false, forceBottom = false } = {}) {
  prune();
  const peers = [];
  const bottom = atBottom();
  for (const message of incoming) {
    if (!validId(message.id) || typeof message.body !== "string" || !Number.isFinite(Date.parse(message.created_at))) continue;
    if (!retained(message)) continue;
    const known = state.seen.has(message.id);
    if (notify && !known && message.sender_id === state.peer?.id && compare(message.id, state.initialBoundary) > 0) peers.push(message);
    state.seen.add(message.id);
    // A server response or a poll can win the race. Reconcile by nonce and sender either way.
    state.messages = state.messages.filter((item) => {
      const replaced = item.id === message.id || (item.client_nonce === message.client_nonce && item.sender_id === message.sender_id);
      if (replaced && item !== message && (item._photoUrl || item.id !== message.id || item.image?.url !== message.image?.url)) releaseMessage(item);
      return !replaced;
    });
    state.messages.push(message);
  }
  state.messages.sort((left, right) => validId(left.id) && validId(right.id) ? compare(left.id, right.id) : validId(left.id) ? -1 : validId(right.id) ? 1 : Date.parse(left.created_at) - Date.parse(right.created_at));
  render({ prepend, forceBottom });
  if (peers.length && visiblePage()) {
    playIncoming(); incomingNotice(peers.length);
    if (!bottom) { state.unread += peers.length; $("jump-bottom").textContent = `${state.unread} new message${state.unread === 1 ? "" : "s"} ↓`; $("jump-bottom").hidden = false; }
  }
}
function acceptMetadata(data) {
  if (Array.isArray(data.receipts)) {
    for (const receipt of data.receipts) {
      const previous = state.receipts.find((item) => item.user_id === receipt.user_id);
      if (!previous) state.receipts.push(receipt);
      else if (receipt.last_read_message_id && (!previous.last_read_message_id || compare(receipt.last_read_message_id, previous.last_read_message_id) > 0)) Object.assign(previous, receipt);
    }
  }
  state.lastRead = state.receipts.find((receipt) => receipt.user_id === state.member.id)?.last_read_message_id || state.lastRead;
  $("peer-status").textContent = !navigator.onLine ? "Offline" : data.peerOnline ? "Online" : "Away";
}
function schedulePoll(delay = 5000) {
  clearTimeout(state.pollTimer);
  if (state.member && visiblePage()) state.pollTimer = setTimeout(sync, delay);
}
async function sync() {
  if (!state.member || !visiblePage()) return;
  if (state.syncing) { state.syncAgain = true; return; }
  if (!navigator.onLine) { setConnection("You’re offline. Your draft stays here."); schedulePoll(); return; }
  state.syncing = true;
  const generation = state.generation;
  let pendingMore = false;
  try {
    for (let page = 0; page < 20; page++) {
      if (!visiblePage()) { pendingMore = false; return; }
      const initial = !state.initialized;
      const data = await api(`/api/messages${initial ? "" : `?after=${encodeURIComponent(state.cursor)}`}`);
      if (generation !== state.generation || !state.member) return;
      const rows = Array.isArray(data.messages) ? data.messages : [];
      const nextCursor = newest(rows);
      acceptMetadata(data);
      if (initial) {
        state.initialBoundary = nextCursor;
        state.hasMore = Boolean(data.hasMore);
        state.initialized = true;
      }
      accept(rows, { notify: !initial, forceBottom: initial });
      const previousCursor = state.cursor;
      if (compare(nextCursor, state.cursor) > 0) state.cursor = nextCursor;
      pendingMore = !initial && Boolean(data.hasMore);
      if (!pendingMore) break;
      if (!rows.length || compare(nextCursor, previousCursor) <= 0) { pendingMore = false; throw new ApiError("Unable to continue message synchronization. Please try again.", 0); }
    }
    setConnection(""); setError("");
    if (visiblePage() && now() - state.lastPresence > 25000) presence(true);
    scheduleRead();
  } catch (error) {
    if (generation !== state.generation) return;
    if (error.status === 401) { endSession("Your session ended. Please sign in again."); return; }
    setConnection(navigator.onLine ? "Reconnecting…" : "You’re offline. Your draft stays here.");
    if (error.status === 503) setError(error.message);
  } finally {
    if (generation === state.generation) {
      state.syncing = false;
      const soon = pendingMore || state.syncAgain; state.syncAgain = false;
      schedulePoll(soon ? 100 : 5000);
    }
  }
}
async function loadOlder() {
  if (!state.member || !state.hasMore || state.loadingOlder) return;
  const before = oldest();
  if (!before) { state.hasMore = false; render(); return; }
  const generation = state.generation;
  state.loadingOlder = true; render();
  try {
    const data = await api(`/api/messages?before=${encodeURIComponent(before)}`);
    if (generation !== state.generation) return;
    const rows = Array.isArray(data.messages) ? data.messages : [];
    state.hasMore = Boolean(data.hasMore) && rows.some((message) => validId(message.id) && compare(message.id, before) < 0);
    acceptMetadata(data); accept(rows, { prepend: true });
  } catch (error) {
    if (generation === state.generation) {
      if (error.status === 401) endSession("Please sign in again."); else setError(error.message);
    }
  } finally { if (generation === state.generation) { state.loadingOlder = false; render(); } }
}
function scheduleRead() {
  clearTimeout(state.readTimer);
  if (state.member && visiblePage()) state.readTimer = setTimeout(flushRead, 250);
}
async function flushRead() {
  prune();
  if (!state.member || state.reading || !visiblePage() || !navigator.onLine) return;
  let candidate = "0";
  for (const id of state.visible) if (state.nodes.has(id) && compare(id, candidate) > 0) candidate = id;
  if (compare(candidate, state.lastRead) <= 0) return;
  state.reading = true;
  const generation = state.generation;
  try {
    await post("/api/read", { message_id: candidate });
    if (generation === state.generation && compare(candidate, state.lastRead) > 0) state.lastRead = candidate;
  } catch { /* Read receipts retry when the page becomes visible or polling completes. */ }
  finally { if (generation === state.generation) state.reading = false; }
}
async function sendMessage(message) {
  if (!state.member || state.senders.has(message.client_nonce)) return;
  const generation = state.generation;
  state.senders.add(message.client_nonce);
  message.status = "sending"; message.uploadProgress = 0; render();
  try {
    const result = message._photoUrl ? await uploadPhoto(message) : await post("/api/messages", { body: message.body, client_nonce: message.client_nonce });
    if (generation !== state.generation) return;
    if (!result?.message) throw new ApiError("The message could not be confirmed. Please retry.", 0);
    accept([result.message]); setError("");
    // Do not advance the poll cursor here: concurrent peer messages can have smaller IDs.
    schedulePoll(100);
  } catch (error) {
    if (generation !== state.generation) return;
    if (state.messages.some((item) => validId(item.id) && item.client_nonce === message.client_nonce && item.sender_id === message.sender_id)) return;
    message.status = "failed"; render(); setError(error.message);
    if (error.status === 401) endSession("Your session ended. Please sign in again.");
  } finally { if (generation === state.generation) state.senders.delete(message.client_nonce); }
}
function submit() {
  if (!state.member || state.composing) return;
  const body = $("draft").value;
  if (state.photo?.status === "preparing" || (!body.trim() && !state.photo)) return;
  const nonce = crypto.randomUUID();
  const message = { id: `pending:${nonce}`, sender_id: state.member.id, body, client_nonce: nonce, created_at: new Date(now()).toISOString(), status: "sending" };
  if (state.photo?.status === "ready") {
    const photo = state.photo;
    message._payload = JSON.stringify({ body, client_nonce: nonce, image: { mimeType: "image/jpeg", data: photo.data } });
    if (new Blob([message._payload]).size > MAX_REQUEST_BYTES) { setError("This message is too large to transfer in one request. Please split it and try again."); return; }
    message._photoUrl = photo.url;
    message.image = { url: photo.url, width: photo.width, height: photo.height, bytes: photo.bytes, mimeType: "image/jpeg" };
    clearPhoto({ release: false }); photoError();
  }
  state.messages.push(message);
  $("draft").value = ""; saveDraft(); autosize();
  render({ forceBottom: true }); sendMessage(message);
}
async function presence(online) {
  if (!state.member) return;
  state.lastPresence = now();
  try { await post("/api/presence", { online }); } catch { /* Presence does not block messages. */ }
}
function endSession(message = "") {
  const oldKey = draftKey();
  ++state.generation;
  clearTimeout(state.pollTimer); clearTimeout(state.expiryTimer); clearTimeout(state.readTimer);
  dismissNotice(); closeCopy();
  closeViewer(); clearPhoto(); photoError();
  for (const xhr of state.uploads) xhr.abort();
  for (const message of state.messages) releaseMessage(message);
  for (const url of state.objectUrls) revokeUrl(url);
  for (const dialog of document.querySelectorAll("dialog[open]")) dialog.close();
  messageObserver.disconnect(); state.nodes.clear(); state.visible.clear(); state.seen.clear();
  state.messages = []; state.receipts = []; state.member = null; state.peer = null;
  state.cursor = "0"; state.initialBoundary = "0"; state.initialized = false; state.hasMore = false;
  state.syncing = false; state.syncAgain = false; state.loadingOlder = false; state.reading = false; state.lastRead = "0";
  state.senders.clear(); state.unread = 0;
  $("push-button").disabled = false; $("push-button").hidden = true;
  $("push-status").hidden = true; state.pushAvailable = false; state.pushRegistered = false;
  $("message-list").replaceChildren(); $("draft").value = ""; $("password").value = "";
  $("chat").hidden = true; $("boot").hidden = true; $("login").hidden = false;
  $("login-error").textContent = message; $("login-error").hidden = !message;
  $("signin").disabled = false;
  try { sessionStorage.removeItem(oldKey); } catch { /* No storage is also safe. */ }
  setError(""); setConnection("");
}
async function startSession(session) {
  if (!session?.member?.id) { endSession(); return; }
  ++state.generation;
  state.member = session.member; state.peer = session.peer;
  $("peer-name").textContent = state.peer?.display_name || "Our conversation";
  $("avatar").textContent = (state.peer?.display_name || "♡").split(/\s+/).filter(Boolean).slice(0, 2).map((part) => Array.from(part)[0]).join("").toUpperCase();
  $("login").hidden = true; $("boot").hidden = true; $("chat").hidden = false;
  $("password").value = "";
  try { $("draft").value = sessionStorage.getItem(draftKey()) || ""; } catch { /* Draft memory only when session storage is unavailable. */ }
  autosize(); setConnection("Connecting…");
  sync(); presence(visiblePage()); checkPush();
}
async function boot() {
  try { const session = await api("/api/session"); await startSession(session); }
  catch (error) { endSession(error.status === 401 ? "" : error.message); }
}
function theme(dark) {
  document.documentElement.classList.toggle("dark", dark);
  $("theme-button").textContent = dark ? "Switch to light appearance" : "Switch to dark appearance";
  document.querySelector('meta[name="theme-color"]').content = dark ? "#181c19" : "#f5f2eb";
}
function soundLabel() {
  $("sound-button").textContent = `Message sound: ${state.sound ? "on" : "off"}`;
  $("sound-button").setAttribute("aria-pressed", String(state.sound));
}
async function resumeAudio() {
  if (!state.sound) return;
  const Audio = window.AudioContext || window.webkitAudioContext;
  if (!Audio) return;
  if (!state.audio) state.audio = new Audio();
  try { await state.audio.resume(); } catch { /* The sound control remains available after a browser restriction. */ }
}
async function checkPush() {
  $("push-button").hidden = true; state.pushAvailable = false; state.pushRegistered = false;
  if (!state.member || !("serviceWorker" in navigator) || !("PushManager" in window) || !("Notification" in window)) return;
  const generation = state.generation;
  try {
    const result = await api("/api/push/key");
    if (generation !== state.generation || !result?.publicKey) return;
    state.pushAvailable = true; $("push-button").hidden = false;
    $("push-button").textContent = "Enable background notifications";
    const registration = await navigator.serviceWorker.ready;
    if (generation !== state.generation) return;
    state.registration = registration;
    const subscription = await registration.pushManager.getSubscription();
    if (generation !== state.generation) return;
    if (subscription) {
      // Browser subscriptions outlive server sessions. Rebind to this authenticated session.
      $("push-button").textContent = "Retry background notifications";
      await post("/api/push/subscribe", subscription.toJSON());
      if (generation !== state.generation) return;
      state.pushRegistered = true;
      $("push-button").textContent = "Disable background notifications";
    }
  } catch {
    if (generation === state.generation && state.pushAvailable) {
      $("push-button").textContent = "Retry background notifications";
      $("push-status").textContent = "Background notifications could not be confirmed. Tap Retry to try again.";
      $("push-status").hidden = false;
    }
  }
}
function base64Bytes(value) {
  const padded = `${value}${"=".repeat((4 - value.length % 4) % 4)}`.replace(/-/g, "+").replace(/_/g, "/");
  return Uint8Array.from(atob(padded), (character) => character.charCodeAt(0));
}
async function togglePush() {
  if (!state.member || !state.pushAvailable) return;
  const button = $("push-button"), status = $("push-status");
  button.disabled = true; status.hidden = true;
  const generation = state.generation;
  try {
    // Ask permission from this trusted user action, before waiting for any network response.
    const permissionPromise = Notification.permission === "default" ? Notification.requestPermission() : Promise.resolve(Notification.permission);
    const permission = await permissionPromise;
    if (permission !== "granted") throw new Error("Notifications are not allowed. You can change this in your browser settings.");
    const registration = state.registration || await navigator.serviceWorker.ready;
    const old = await registration.pushManager.getSubscription();
    if (generation !== state.generation) return;
    if (old && state.pushRegistered) {
      await api("/api/push/subscribe", { method: "DELETE", body: JSON.stringify({ endpoint: old.endpoint }) });
      await old.unsubscribe();
      if (generation !== state.generation) return;
      state.pushRegistered = false;
      button.textContent = "Enable background notifications";
      status.textContent = "Background notifications are off on this device.";
    } else {
      const { publicKey } = await api("/api/push/key");
      const subscription = old || await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: base64Bytes(publicKey) });
      try { await post("/api/push/subscribe", subscription.toJSON()); }
      catch (error) { if (!old) await subscription.unsubscribe(); throw error; }
      if (generation !== state.generation) return;
      state.pushRegistered = true;
      button.textContent = "Disable background notifications";
      status.textContent = "Background notifications are enabled. Your phone controls sound and Focus settings.";
    }
  } catch (error) { status.textContent = error.message || "Unable to enable notifications on this device."; }
  finally { if (generation === state.generation) { button.disabled = false; status.hidden = false; } }
}

$("login-form").addEventListener("submit", async (event) => {
  event.preventDefault(); $("signin").disabled = true; $("login-error").hidden = true;
  try {
    await post("/api/auth/signin", { email: $("email").value.trim(), password: $("password").value });
    const session = await api("/api/session"); await startSession(session);
  } catch (error) {
    $("login-error").textContent = error.status === 401 ? "Email or password is incorrect." : error.message;
    $("login-error").hidden = false; $("signin").disabled = false;
  }
});
$("composer-form").addEventListener("submit", (event) => { event.preventDefault(); submit(); });
$("draft").addEventListener("input", () => { saveDraft(); autosize(); });
$("attach-photo").addEventListener("click", () => $("photo-input").click());
$("change-photo").addEventListener("click", () => $("photo-input").click());
$("photo-input").addEventListener("change", () => { const file = $("photo-input").files?.[0]; if (file) preparePhoto(file); });
$("remove-photo").addEventListener("click", () => { clearPhoto(); photoError(); $("attach-photo").focus({ preventScroll: true }); announce("Photo removed."); });
$("draft").addEventListener("compositionstart", () => { state.composing = true; });
$("draft").addEventListener("compositionend", () => { state.composing = false; });
$("draft").addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey && !event.isComposing && !state.composing && event.keyCode !== 229 && matchMedia("(hover: hover) and (pointer: fine)").matches) {
    event.preventDefault(); submit();
  }
});
$("messages").addEventListener("scroll", () => {
  if (atBottom()) { state.unread = 0; $("jump-bottom").hidden = true; }
  if ($("messages").scrollTop < 60 && state.hasMore && !state.loadingOlder) loadOlder();
  scheduleRead();
}, { passive: true });
$("load-older").addEventListener("click", loadOlder);
$("jump-bottom").addEventListener("click", scrollBottom);
let noticeFocus = null;
for (const id of ["notice-view", "notice-dismiss"]) $(id).addEventListener("pointerdown", () => { noticeFocus = rememberFocus(); });
$("notice-view").addEventListener("click", () => { const saved = noticeFocus || rememberFocus(); noticeFocus = null; dismissNotice(); scrollBottom(); if (saved.element === $("draft")) { saved.scroll = $("messages").scrollTop; restoreFocus(saved); } });
$("notice-dismiss").addEventListener("click", () => { const saved = noticeFocus || rememberFocus(); noticeFocus = null; dismissNotice(); if (saved.element === $("draft")) restoreFocus(saved); });
$("settings-button").addEventListener("click", () => openDialog($("settings-dialog")));
document.querySelectorAll(".close-dialog").forEach((button) => button.addEventListener("click", () => button.closest("dialog").close()));
$("copy-dialog").addEventListener("close", () => { const saved = state.copyRestore; state.copyRestore = null; state.copyTarget = null; $("copy-text").value = ""; restoreFocus(saved); });
$("photo-dialog").addEventListener("close", () => { const saved = state.viewer?.restore; closeViewer(); restoreFocus(saved); });
$("save-photo").addEventListener("click", (event) => {
  const message = state.messages.find((item) => item.id === state.viewer?.id);
  if (!message || !retained(message) || !imageSource(message)) { event.preventDefault(); closeViewer(); prune(); }
});
$("select-copy").addEventListener("click", () => { $("copy-text").focus({ preventScroll: true }); $("copy-text").select(); $("copy-text").setSelectionRange(0, $("copy-text").value.length); });
document.querySelectorAll(".install-help").forEach((button) => button.addEventListener("click", () => {
  $("app-address").value = `${location.origin}/`; openDialog($("install-dialog"));
}));
$("copy-address").addEventListener("click", () => {
  const address = `${location.origin}/`;
  const done = () => { $("copy-address").textContent = "Copied"; announce("OurChat address copied."); setTimeout(() => { $("copy-address").textContent = "Copy address"; }, 2000); };
  const fallback = () => { $("app-address").focus(); $("app-address").select(); announce("Address selected. Use your device’s Copy command."); };
  if (navigator.clipboard?.writeText) { try { navigator.clipboard.writeText(address).then(done, fallback); } catch { fallback(); } }
  else fallback();
});
$("theme-button").addEventListener("click", () => {
  const dark = !document.documentElement.classList.contains("dark"); theme(dark);
  try { localStorage.setItem("ourchat-theme", dark ? "dark" : "light"); } catch { /* Appearance remains usable without storage. */ }
});
$("sound-button").addEventListener("click", async () => {
  state.sound = !state.sound; soundLabel();
  try { localStorage.setItem("ourchat-sound", state.sound ? "on" : "off"); } catch { /* An opt-in still works for this visit. */ }
  await resumeAudio();
});
document.addEventListener("pointerdown", resumeAudio, { passive: true });
document.addEventListener("keydown", resumeAudio, { passive: true });
$("push-button").addEventListener("click", togglePush);
$("signout").addEventListener("click", async () => {
  const button = $("signout"); button.disabled = true;
  try {
    // Optional browser push support or a failed unsubscribe must never block signing out.
    try {
      if (state.registration?.pushManager) {
        const subscription = await state.registration.pushManager.getSubscription();
        if (subscription) {
          await api("/api/push/subscribe", { method: "DELETE", body: JSON.stringify({ endpoint: subscription.endpoint }) });
          await subscription.unsubscribe();
        }
      }
    } catch { /* The server session is still revoked below. Notifications never contain message text. */ }
    await post("/api/auth/signout", {}); endSession();
  } catch (error) { setError(error.message); $("settings-dialog").close(); }
  finally { button.disabled = false; }
});
document.addEventListener("visibilitychange", () => {
  prune();
  if (visiblePage()) { sync(); presence(true); scheduleRead(); }
  else { clearTimeout(state.pollTimer); clearTimeout(state.readTimer); dismissNotice(); presence(false); }
});
window.addEventListener("focus", () => { prune(); if (state.member && visiblePage()) { sync(); scheduleRead(); } });
window.addEventListener("online", () => { prune(); if (state.member) { sync(); presence(visiblePage()); } });
window.addEventListener("offline", () => { if (state.member) { setConnection("You’re offline. Your draft stays here."); $("peer-status").textContent = "Offline"; } });
window.addEventListener("pageshow", () => { prune(); if (state.member && visiblePage()) sync(); });
window.addEventListener("pagehide", () => {
  saveDraft();
  // A same-origin credentialed keepalive uses JSON and preserves the server's Origin checks.
  if (state.member) fetch("/api/presence", { method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json" }, body: '{"online":false}', keepalive: true }).catch(() => {});
});
window.visualViewport?.addEventListener("resize", viewport);
window.visualViewport?.addEventListener("scroll", viewport);
window.addEventListener("resize", () => { viewport(); autosize(); });
try {
  theme(localStorage.getItem("ourchat-theme") === "dark");
  state.sound = localStorage.getItem("ourchat-sound") === "on";
} catch { theme(false); }
soundLabel(); viewport(); $("send").disabled = true;
if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("/sw.js", { scope: "/", updateViaCache: "none" }).then((registration) => { state.registration = registration; }).catch(() => {});
}
boot();
