/*
 * The floating chat window (Exercise 3 bonus) - the web twin of ChatController:
 * collapsed to its header at the bottom-left, click to open, drag the header to
 * move it, an unread badge while collapsed, and a 1-second poll for messages
 * newer than the last one received. All messages live on the server.
 */
import * as api from "./api.js";
import { el, showErrorAlert } from "./ui.js";

const POLL_MILLIS = 1000;
const DRAG_THRESHOLD = 4;

const $ = (id) => document.getElementById(id);
const timeOf = (millis) => new Date(millis).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", hour12: false });

export class Chat {
    constructor(app) {
        this.app = app;
        this.root = $("chat");
        this.list = $("chat-messages");
        this.input = $("chat-input");
        this.timer = null;
        this.lastId = 0;
        this.expanded = false;
        this.historyLoaded = false;
        this.unread = 0;
        this.offset = { x: 0, y: 0 };

        this.initDragAndToggle();
        $("chat-form").addEventListener("submit", (event) => {
            event.preventDefault();
            this.send();
        });
        window.addEventListener("resize", () => this.clamp());
    }

    start() {
        this.stop();
        this.lastId = 0;
        this.historyLoaded = false;
        this.setUnread(0);
        this.list.replaceChildren(el("li", "chat-empty", "No messages yet"));
        this.timer = setInterval(() => this.poll(), POLL_MILLIS);
        this.poll();
    }

    stop() {
        if (this.timer) clearInterval(this.timer);
        this.timer = null;
        this.setExpanded(false);
    }

    async poll() {
        if (this.polling) return;
        this.polling = true;
        try {
            const messages = await api.getChatMessages(this.lastId);
            this.append(messages);
        } catch {
            /* the main poll reports a lost session; here we simply try again */
        } finally {
            this.polling = false;
        }
    }

    append(messages) {
        const first = !this.historyLoaded;
        this.historyLoaded = true;
        if (!messages || messages.length === 0) return;

        this.list.querySelector(".chat-empty")?.remove();
        for (const message of messages) {
            if (message.id <= this.lastId) continue;
            const item = el("li");
            item.textContent = `[${timeOf(message.timestamp)}] ${message.userName}: ${message.text}`;
            this.list.append(item);
            this.lastId = message.id;
        }

        if (this.expanded) {
            this.list.scrollTop = this.list.scrollHeight;
        } else if (!first) {
            const me = this.app.user?.name;
            const fromOthers = messages.filter((m) => m.userName !== me).length;
            if (fromOthers > 0) this.setUnread(this.unread + fromOthers);
        }
    }

    async send() {
        const text = this.input.value.trim();
        if (!text) return;
        const button = $("chat-send");
        button.disabled = true;
        try {
            await api.sendChatMessage(text);
            this.input.value = "";
            this.poll();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            showErrorAlert("Chat", error.message);
        } finally {
            button.disabled = false;
            this.input.focus();
        }
    }

    setUnread(count) {
        this.unread = count;
        const badge = $("chat-badge");
        badge.textContent = String(count);
        badge.hidden = count === 0;
    }

    setExpanded(expanded) {
        this.expanded = expanded;
        $("chat-body").hidden = !expanded;
        $("chat-chevron").textContent = expanded ? "▼" : "▲";
        if (expanded) {
            this.setUnread(0);
            this.list.scrollTop = this.list.scrollHeight;
            this.input.focus();
        }
        this.clamp();
    }

    // ---------------- click to toggle, drag to move ----------------

    initDragAndToggle() {
        const header = $("chat-header");
        let start = null;
        let dragged = false;

        header.addEventListener("pointerdown", (event) => {
            start = { x: event.clientX, y: event.clientY, ox: this.offset.x, oy: this.offset.y };
            dragged = false;
            header.setPointerCapture(event.pointerId);
        });

        header.addEventListener("pointermove", (event) => {
            if (!start) return;
            const dx = event.clientX - start.x;
            const dy = event.clientY - start.y;
            if (!dragged && Math.hypot(dx, dy) < DRAG_THRESHOLD) return;
            dragged = true;
            this.offset = { x: start.ox + dx, y: start.oy + dy };
            this.clamp();
        });

        header.addEventListener("pointerup", () => {
            if (start && !dragged) this.setExpanded(!this.expanded);   // a drag's release is not a click
            start = null;
        });
    }

    /** Keeps the whole window inside the main area; the un-moved spot is bottom-left. */
    clamp() {
        const parent = this.root.offsetParent;
        if (!parent) return;
        const home = 16;
        const maxX = parent.clientWidth - this.root.offsetWidth - home;
        const minY = -(parent.clientHeight - this.root.offsetHeight);
        this.offset.x = Math.min(Math.max(this.offset.x, -home), Math.max(-home, maxX));
        this.offset.y = Math.max(Math.min(this.offset.y, 0), Math.min(0, minY));
        this.root.style.transform = `translate(${this.offset.x}px, ${this.offset.y}px)`;
    }
}
