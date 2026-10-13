/*
 * Shared building blocks that stand in for the JavaFX controls the desktop
 * client uses: TableView (with the row fade-in and sliding selection
 * animations), Alert / ChoiceDialog / the deposit dialog, TextFormatter-style
 * input filters, SplitPane dividers, and number formatting.
 */

export const EMPTY = "—"; // em dash, as in the desktop client

// ---------------- animations flag (top bar checkbox) ----------------

let animationsEnabled = false;

export function setAnimationsEnabled(enabled) {
    animationsEnabled = enabled;
    document.documentElement.classList.toggle("animations-on", enabled);
}

export function areAnimationsEnabled() {
    return animationsEnabled;
}

// ---------------- formatting ----------------

export const fmt2 = (value) => (value === null || value === undefined || Number.isNaN(Number(value)))
    ? EMPTY
    : Number(value).toFixed(2);

export const fmtSigned = (value) => (value >= 0 ? "+" : "") + Number(value).toFixed(2);

export function statusText(status) {
    switch (status) {
        case "OPEN": return "Open";
        case "CLOSED": return "Closed";
        default: return "Not Started";
    }
}

export const isLmsr = (event) => event?.method?.type === "lmsr";

// ---------------- element helpers ----------------

export function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
}

export function show(node, visible) {
    node.hidden = !visible;
}

/** Fills a select; options are strings or {value, label}. Keeps the value when still present. */
export function setOptions(select, options, { prompt } = {}) {
    const previous = select.value;
    select.replaceChildren();
    if (prompt) {
        const placeholder = el("option", null, prompt);
        placeholder.value = "";
        placeholder.disabled = true;
        placeholder.hidden = true;
        select.append(placeholder);
    }
    for (const option of options) {
        const item = typeof option === "string" ? { value: option, label: option } : option;
        const node = el("option", null, item.label);
        node.value = item.value;
        select.append(node);
    }
    const values = options.map((o) => (typeof o === "string" ? o : o.value));
    select.value = values.includes(previous) ? previous : (prompt ? "" : values[0] ?? "");
    markPrompt(select);
}

/** A select still showing its prompt text is drawn in the muted colour, like ComboBox.promptText. */
export function markPrompt(select) {
    select.classList.toggle("prompt", select.value === "");
}

// ---------------- TextFormatter-style input filters ----------------

/**
 * Rejects any edit that would leave the field holding text that fails the
 * test, like the desktop client's TextFormatters: invalid text never appears.
 */
export function restrictInput(input, isAcceptable) {
    let lastGood = input.value;
    input.addEventListener("input", () => {
        if (input.value === "" || isAcceptable(input.value)) {
            lastGood = input.value;
        } else {
            const caret = Math.max(0, input.selectionStart - (input.value.length - lastGood.length));
            input.value = lastGood;
            input.setSelectionRange(caret, caret);
        }
    });
}

/** Positive whole number that fits in a Java int (restrictToPositiveInteger). */
export const positiveInteger = (text) => /^\d{1,9}$/.test(text) && Number(text) >= 1;

/** Up to 6 digits and 2 decimals (restrictToPrice). */
export const priceText = (text) => /^\d{0,6}(\.\d{0,2})?$/.test(text);

/** Enter in a field presses the given button, if it is enabled. */
export function enterPresses(input, button) {
    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !button.disabled) {
            event.preventDefault();
            button.click();
        }
    });
}

// ---------------- split panes ----------------

/** Makes a .split-pane's divider draggable; data-split is the initial left share. */
export function initSplitPane(pane) {
    const [left, divider, right] = pane.children;
    let ratio = Number(pane.dataset.split) || 0.5;

    const apply = () => {
        left.style.flex = `0 0 calc(${(ratio * 100).toFixed(2)}% - 4.5px)`;
        right.style.flex = "1 1 0";
    };
    apply();

    divider.addEventListener("pointerdown", (event) => {
        event.preventDefault();
        divider.setPointerCapture(event.pointerId);
        divider.classList.add("dragging");
        const bounds = pane.getBoundingClientRect();

        const move = (e) => {
            ratio = Math.min(0.8, Math.max(0.2, (e.clientX - bounds.left) / bounds.width));
            apply();
        };
        const stop = () => {
            divider.classList.remove("dragging");
            divider.removeEventListener("pointermove", move);
            divider.removeEventListener("pointerup", stop);
            window.dispatchEvent(new Event("resize"));
        };
        divider.addEventListener("pointermove", move);
        divider.addEventListener("pointerup", stop);
    });
}

// ---------------- TableView ----------------

// RowAnimations timings: first time a view is shown, rows arrive slowly one by one;
// later refreshes only fade in the rows that are new, and quicker.
const SLOW = { fade: 500, gap: 80, batch: 1500 };
const FAST = { fade: 220, gap: 30, batch: 600 };

/**
 * A read-only table drawn like the JavaFX TableView in the desktop client.
 *
 * columns: [{ title, value: row => text, width }]  (width is a relative share)
 * key:     row => stable id, used for selection and the fade-in animation
 */
export class Table {
    constructor(container, { columns, key, placeholder = "No content in table", selectable = false, onSelect } = {}) {
        this.container = container;
        this.columns = columns;
        this.key = key;
        this.selectable = selectable;
        this.onSelect = onSelect;
        this.rows = [];
        this.selectedKey = null;
        this.placeholderText = placeholder;
        this.seenScopes = new Set();
        this.shownKeys = new Set();
        this.scope = null;

        container.classList.toggle("selectable", selectable);
        container.replaceChildren();

        this.table = el("table");
        const colgroup = el("colgroup");
        const total = columns.reduce((sum, c) => sum + (c.width || 1), 0);
        const headRow = el("tr");
        for (const column of columns) {
            const col = el("col");
            col.style.width = `${((column.width || 1) / total) * 100}%`;
            colgroup.append(col);
            const th = el("th", null, column.title);
            th.title = column.title;
            headRow.append(th);
        }
        const thead = el("thead");
        thead.append(headRow);
        this.body = el("tbody");
        this.table.append(colgroup, thead, this.body);

        this.band = el("div", "selection-band");
        this.band.hidden = true;
        this.placeholder = el("div", "table-placeholder", placeholder);

        container.append(this.band, this.table, this.placeholder);

        if (selectable) {
            this.body.addEventListener("click", (event) => {
                const tr = event.target.closest("tr");
                if (tr) this.select(tr.dataset.key, { notify: true });
            });
        }

        new ResizeObserver(() => this.placeBand(false)).observe(container);
    }

    setPlaceholder(text) {
        this.placeholderText = text;
        this.placeholder.textContent = text;
    }

    /**
     * Shows the rows. scope names what is being shown (an event, a user...);
     * a scope seen for the first time gets the slow fade-in, as in the desktop.
     */
    setRows(rows, { scope = "default", animate = true } = {}) {
        this.rows = rows;
        const keys = rows.map((row) => String(this.key(row)));

        const firstTime = !this.seenScopes.has(scope);
        const scopeChanged = scope !== this.scope;
        this.seenScopes.add(scope);
        if (scopeChanged) {
            this.shownKeys = new Set();
            this.scope = scope;
        }

        const fadeKeys = (animate && areAnimationsEnabled())
            ? keys.filter((k) => !this.shownKeys.has(k))
            : [];
        const timing = firstTime ? SLOW : FAST;
        const gap = fadeKeys.length <= 1 ? 0
            : Math.min(timing.gap, Math.max(0, timing.batch - timing.fade) / (fadeKeys.length - 1));

        const fragment = document.createDocumentFragment();
        let fadeIndex = 0;
        rows.forEach((row, i) => {
            const tr = el("tr");
            tr.dataset.key = keys[i];
            for (const column of this.columns) {
                const text = String(column.value(row) ?? "");
                const td = el("td", null, text);
                td.title = text;
                tr.append(td);
            }
            if (fadeKeys.includes(keys[i])) {
                tr.classList.add("fade-in");
                tr.style.setProperty("--fade", `${timing.fade}ms`);
                tr.style.setProperty("--delay", `${Math.round(fadeIndex++ * gap)}ms`);
            }
            fragment.append(tr);
        });
        this.body.replaceChildren(fragment);
        this.shownKeys = new Set(keys);

        this.placeholder.textContent = this.placeholderText;
        show(this.placeholder, rows.length === 0);

        if (this.selectedKey !== null && !keys.includes(this.selectedKey)) {
            this.selectedKey = null;
        }
        this.markSelected();
        this.placeBand(false);
    }

    clear() {
        this.setRows([], { scope: this.scope ?? "default", animate: false });
    }

    get selectedRow() {
        return this.rows.find((row) => String(this.key(row)) === this.selectedKey) ?? null;
    }

    select(key, { notify = false } = {}) {
        const next = key === null || key === undefined ? null : String(key);
        const changed = next !== this.selectedKey;
        this.selectedKey = next;
        this.markSelected();
        this.placeBand(true);
        if (notify && changed && this.onSelect) {
            this.onSelect(this.selectedRow);
        }
    }

    markSelected() {
        for (const tr of this.body.children) {
            tr.classList.toggle("selected", tr.dataset.key === this.selectedKey);
        }
    }

    /** Moves the selection band under the selected row (slides when animations are on). */
    placeBand(slide) {
        const tr = this.selectedKey === null ? null
            : [...this.body.children].find((row) => row.dataset.key === this.selectedKey);
        if (!tr) {
            this.band.hidden = true;
            return;
        }
        const wasHidden = this.band.hidden;
        const top = tr.getBoundingClientRect().top - this.container.getBoundingClientRect().top + this.container.scrollTop;
        if (!slide || wasHidden) {
            this.band.style.transition = "none";
        }
        this.band.hidden = false;
        this.band.style.transform = `translateY(${top}px)`;
        this.band.style.height = `${tr.offsetHeight}px`;
        if (!slide || wasHidden) {
            this.band.getBoundingClientRect(); // apply the jump before re-enabling transitions
            this.band.style.transition = "";
        }
    }
}

// ---------------- dialogs (Alert, ChoiceDialog, deposit) ----------------

const dialogLayer = () => document.getElementById("dialog-layer");

/**
 * Builds a modal dialog. Resolves with the value given to close(); Escape and
 * clicking outside resolve with null, like closing the JavaFX dialog window.
 */
function openDialog({ title, header, icon, build }) {
    return new Promise((resolve) => {
        const previousFocus = document.activeElement;
        const backdrop = el("div", "backdrop");
        const pane = el("div", "dialog-pane");
        pane.setAttribute("role", "dialog");
        pane.setAttribute("aria-modal", "true");

        pane.append(el("div", "dialog-title", title));
        if (header) {
            const head = el("div", "dialog-header");
            if (icon) head.append(el("span", `dialog-icon ${icon}`, icon === "error" ? "!" : icon === "info" ? "i" : "?"));
            head.append(el("span", null, header));
            pane.append(head);
        }

        const content = el("div", "dialog-content");
        const buttons = el("div", "dialog-buttons");
        pane.append(content, buttons);
        backdrop.append(pane);

        const close = (value) => {
            backdrop.remove();
            document.removeEventListener("keydown", onKey, true);
            previousFocus?.focus?.();
            resolve(value);
        };
        const onKey = (event) => {
            if (event.key === "Escape") {
                event.preventDefault();
                close(null);
            }
        };
        document.addEventListener("keydown", onKey, true);
        backdrop.addEventListener("mousedown", (event) => {
            if (event.target === backdrop) close(null);
        });

        const focusTarget = build(content, buttons, close);
        dialogLayer().append(backdrop);
        (focusTarget || buttons.querySelector("button"))?.focus();
    });
}

function dialogButton(text, onClick, primary = true) {
    const button = el("button", primary ? "button keycap" : "button keycap secondary", text);
    button.type = "button";
    button.addEventListener("click", onClick);
    return button;
}

export function showErrorAlert(title, message) {
    return openDialog({
        title, header: "Error", icon: "error",
        build: (content, buttons, close) => {
            content.textContent = message || "Something went wrong.";
            const ok = dialogButton("OK", () => close(true));
            buttons.append(ok);
            return ok;
        },
    });
}

export function showInfoAlert(title, message) {
    return openDialog({
        title, header: title, icon: "info",
        build: (content, buttons, close) => {
            content.textContent = message;
            const ok = dialogButton("OK", () => close(true));
            buttons.append(ok);
            return ok;
        },
    });
}

/** ChoiceDialog: header, a label and a drop-down; resolves with the chosen text or null. */
export function showChoiceDialog(title, header, label, options) {
    return openDialog({
        title, header, icon: "question",
        build: (content, buttons, close) => {
            const row = el("label", "choice-row");
            const select = el("select", "combo-box");
            setOptions(select, options);
            row.append(el("span", null, label), select);
            content.append(row);
            buttons.append(
                dialogButton("OK", () => close(select.value)),
                dialogButton("Cancel", () => close(null), false),
            );
            select.addEventListener("keydown", (e) => {
                if (e.key === "Enter") close(select.value);
            });
            return select;
        },
    });
}

/** The deposit popup: Deposit stays disabled until the text is a positive amount (max 2 decimals). */
export function showDepositDialog() {
    const parse = (text) => {
        const t = text.trim();
        if (!/^\d+(\.\d{1,2})?$/.test(t)) return null;
        const value = Number(t);
        return value > 0 ? value : null;
    };
    return openDialog({
        title: "Deposit", header: "Load funds into your account",
        build: (content, buttons, close) => {
            const field = el("input", "text-field");
            field.placeholder = "0.00";
            field.inputMode = "decimal";
            const hint = el("div", "muted-label");
            content.append(el("span", null, "Amount"), field, hint);

            const depositButton = dialogButton("Deposit", () => close(parse(field.value)));
            depositButton.disabled = true;
            buttons.append(depositButton, dialogButton("Cancel", () => close(null), false));

            field.addEventListener("input", () => {
                const valid = parse(field.value) !== null;
                depositButton.disabled = !valid;
                hint.textContent = field.value.trim() === "" || valid
                    ? "" : "Enter a positive amount with up to 2 decimals.";
            });
            enterPresses(field, depositButton);
            return field;
        },
    });
}
