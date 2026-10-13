/*
 * The Events tab - the web twin of EventTabController, EventLeftController and
 * EventRightController: the filtered list of all events on the left, the
 * selected event's details and trading on the right, and the Activate / Close
 * button for the event's market maker.
 */
import * as api from "./api.js";
import {
    EMPTY, Table, el, fmt2, statusText, isLmsr, setOptions, markPrompt,
    restrictInput, positiveInteger, priceText, enterPresses,
    showErrorAlert, showChoiceDialog,
} from "./ui.js";

const $ = (id) => document.getElementById(id);
const ALL = "All";

export class EventsTab {
    constructor(app) {
        this.app = app;
        this.allEvents = [];
        this.current = null;          // EventSummaryDTO on the right, a snapshot
        this.loadToken = 0;           // newest request wins
        this.listToken = 0;

        this.initFilters();
        this.initEventsTable();
        this.initLmsrView();
        this.initOrderBookView();

        $("event-status-button").addEventListener("click", () => this.manageEvent());
        app.onUserChanged(() => {
            this.updateManagementButton();
            this.updateTradingControls();
        });
        this.showPlaceholders();
    }

    // =====================================================================
    // left: filters + events table
    // =====================================================================

    initFilters() {
        this.methodFilter = $("filter-method");
        this.statusFilter = $("filter-status");
        this.commissionFilter = $("filter-commission");
        this.commissionTypeFilter = $("filter-commission-type");

        setOptions(this.methodFilter, [ALL, "Lmsr", "Order Book"], { prompt: "Method" });
        setOptions(this.statusFilter, [ALL, "Not Started", "Open", "Closed"], { prompt: "Status" });
        setOptions(this.commissionFilter, [ALL], { prompt: "Commission" });
        setOptions(this.commissionTypeFilter, [ALL, "On-Close", "On-Purchase"], { prompt: "Commission Type" });

        for (const filter of [this.methodFilter, this.statusFilter, this.commissionFilter, this.commissionTypeFilter]) {
            filter.addEventListener("change", () => {
                markPrompt(filter);
                this.showFiltered({ animate: true });
            });
        }
    }

    initEventsTable() {
        this.eventsTable = new Table($("events-table"), {
            columns: [
                { title: "#", value: (e) => e.id, width: 35 },
                { title: "Event", value: (e) => e.name, width: 148 },
                { title: "Method", value: (e) => (isLmsr(e) ? "Lmsr" : "Order Book"), width: 94 },
                { title: "Status", value: (e) => statusText(e.status), width: 82 },
                { title: "Commission", value: (e) => e.commission.value, width: 94 },
                { title: "Commission Type", value: (e) => e.commission.commissionType, width: 126 },
            ],
            key: (e) => e.id,
            selectable: true,
            placeholder: "No content in table",
            onSelect: (event) => this.showEvent(event, { animate: true }),
        });
    }

    /** Back to the first-login state (called after every login). */
    reset() {
        this.allEvents = [];
        this.eventsTable.select(null);
        this.eventsTable.setRows([], { scope: "events", animate: false });
        this.current = null;
        this.showPlaceholders();
    }

    /** Loads the list from the server; keeps and re-shows the selected event if it is still listed. */
    async loadEvents({ animate = true, reloadSelected = true } = {}) {
        const token = ++this.listToken;
        let events;
        try {
            events = await api.getEvents();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            return showErrorAlert("Events", error.message || "Could not load events.");
        }
        if (token !== this.listToken) return;

        this.allEvents = events;
        this.refreshCommissionValues(events);
        this.showFiltered({ animate });

        const selectedId = this.eventsTable.selectedKey;
        const updated = this.eventsTable.selectedRow;
        if (selectedId !== null && updated && reloadSelected) {
            this.showEvent(updated, { animate });
        } else if (selectedId === null && this.current) {
            this.clear();
        }
    }

    /** Called when the server data changed, or when the tab is opened. */
    refresh() {
        return this.loadEvents({ animate: true });
    }

    /** The Commission drop-down lists the percentages that actually exist. */
    refreshCommissionValues(events) {
        const values = [...new Set(events.map((e) => e.commission.value))].sort((a, b) => a - b);
        setOptions(this.commissionFilter, [ALL, ...values.map(String)], { prompt: "Commission" });
    }

    showFiltered({ animate }) {
        const method = this.methodFilter.value;
        const status = this.statusFilter.value;
        const commission = this.commissionFilter.value;
        const commissionType = this.commissionTypeFilter.value;

        const filtered = this.allEvents.filter((e) =>
            (!commission || commission === ALL || String(e.commission.value) === commission)
            && (!method || method === ALL || (method === "Lmsr") === isLmsr(e))
            && (!status || status === ALL || statusText(e.status) === status)
            && (!commissionType || commissionType === ALL
                || commissionType.toLowerCase() === e.commission.commissionType.toLowerCase()));

        const wasSelected = this.eventsTable.selectedKey;
        this.eventsTable.setRows(filtered, { scope: "events", animate });

        // the selected event was filtered out: nothing is selected any more
        if (wasSelected !== null && this.eventsTable.selectedKey === null) {
            this.clear();
        }
    }

    // =====================================================================
    // right: event details
    // =====================================================================

    initLmsrView() {
        this.lmsr = {
            name: $("lmsr-name"),
            status: $("lmsr-status"),
            balance: $("lmsr-balance"),
            commission: $("lmsr-commission"),
            description: $("lmsr-description"),
            options: [...document.querySelectorAll(".lmsr-option")].map((box) => ({
                title: box.querySelector(".option-title"),
                table: new Table(box.querySelector('[data-role="option-table"]'), {
                    columns: [
                        { title: "Current Value", value: (o) => fmt2(o.currentValue), width: 110 },
                        { title: "Total Shares Bought", value: (o) => o.totalSharesBought, width: 160 },
                    ],
                    key: (o) => o.optionName,
                    placeholder: EMPTY,
                }),
                field: box.querySelector('[data-role="bet-field"]'),
                button: box.querySelector('[data-role="bet-button"]'),
                number: Number(box.dataset.option),
            })),
            participations: new Table($("lmsr-participations"), {
                columns: [
                    { title: "User", value: (t) => t.userName, width: 130 },
                    { title: "Option", value: (t) => t.optionName, width: 180 },
                    { title: "Shares", value: (t) => t.sharesBought, width: 100 },
                    { title: "Paid", value: (t) => fmt2(t.pricePaid), width: 110 },
                ],
                key: (t) => t.id,
            }),
        };

        for (const option of this.lmsr.options) {
            restrictInput(option.field, positiveInteger);
            option.field.addEventListener("input", () => this.updateTradingControls());
            enterPresses(option.field, option.button);
            option.button.addEventListener("click", () => this.placeLmsrBet(option));
        }
    }

    initOrderBookView() {
        this.ob = {
            pane: $("ob-pane"),
            balance: $("ob-event-balance"),
            commission: $("ob-commission"),
            participations: new Table($("ob-participations"), {
                columns: [
                    { title: "User", value: (p) => p.userName, width: 100 },
                    { title: "Option", value: (p) => p.optionName, width: 120 },
                    { title: "Shares", value: (p) => p.shares, width: 80 },
                    { title: "Value", value: (p) => fmt2(p.totalSharesValue), width: 90 },
                ],
                key: (p) => p.userName + "\u0000" + p.optionName,
            }),
            options: [...document.querySelectorAll(".ob-option")].map((box) => this.buildOrderBookOption(box)),
        };
    }

    /** One option column of the order-book view (option title, book, order controls, stats). */
    buildOrderBookOption(box) {
        const title = el("div", "option-title", "Option " + box.dataset.option);
        const card = el("div", "card");

        const valueLine = el("div", "ob-value-line");
        const value = el("span", "strong-label", "0.00");
        const shares = el("span", "strong-label", "0");
        valueLine.append(el("span", "muted-label", "Value:"), value, el("span", "muted-label", "Shares:"), shares);

        const tableHost = el("div", "table-view fixed-210");

        const orderRow = el("div", "ob-order-row");
        const side = el("select", "combo-box side-box");
        setOptions(side, ["Bid", "Ask"]);
        side.setAttribute("aria-label", "Side");
        const sharesField = el("input", "text-field bet-field");
        sharesField.placeholder = "Shares";
        sharesField.inputMode = "numeric";
        const priceField = el("input", "text-field bet-field");
        priceField.placeholder = "Price";
        priceField.inputMode = "decimal";
        const button = el("button", "button keycap bet-button", "Place order");
        button.type = "button";
        orderRow.append(side, sharesField, priceField, button);

        const stats = el("div", "ob-stats");
        const statLabels = {};
        for (const name of ["LAST", "BID", "ASK", "MID", "SPREAD"]) {
            const line = el("div");
            statLabels[name] = el("span", "strong-label", EMPTY);
            line.append(el("span", "muted-label", name + ":"), statLabels[name]);
            stats.append(line);
        }

        card.append(valueLine, tableHost, orderRow, stats);
        box.append(title, card);

        const option = {
            number: Number(box.dataset.option),
            title, value, shares, side, sharesField, priceField, button, stats: statLabels,
            table: new Table(tableHost, {
                columns: [
                    { title: "User", value: (o) => o.userName, width: 100 },
                    { title: "Side", value: (o) => (o.side === "BUY" ? "Buy" : "Sell"), width: 55 },
                    { title: "Shares", value: (o) => o.quantity, width: 80 },
                    { title: "Price (per share)", value: (o) => fmt2(o.price), width: 110 },
                ],
                key: (o) => o.id,
            }),
        };

        restrictInput(sharesField, positiveInteger);
        restrictInput(priceField, priceText);
        for (const field of [sharesField, priceField]) {
            field.addEventListener("input", () => this.updateTradingControls());
            enterPresses(field, button);
        }
        button.addEventListener("click", () => this.placeOrder(option));
        return option;
    }

    /** A row was selected on the left (or the selection was cleared). */
    showEvent(summary, { animate = true } = {}) {
        const sameEvent = this.current && summary && this.current.id === summary.id;
        this.current = summary;
        this.updateManagementButton();
        this.updateTradingControls();

        if (!summary) {
            this.clear();
            return;
        }
        if (!sameEvent) {
            this.lmsr.options.forEach((o) => { o.field.value = ""; });
            this.updateTradingControls();
        }
        this.loadAndShow(summary, animate);
    }

    /** Fetches everything the right pane needs for one event, then shows it in one go. */
    async loadAndShow(summary, animate) {
        const token = ++this.loadToken;
        const lmsr = isLmsr(summary);
        try {
            const [status, stats, participants] = await Promise.all([
                api.getEventTradingStatus(summary.id),
                lmsr ? [] : api.getOrderBookStats(summary.id),
                lmsr ? [] : api.getEventParticipants(summary.id),
            ]);
            if (token !== this.loadToken || this.current?.id !== summary.id) return;   // another row was clicked

            if (lmsr) this.showLmsrView(status, animate);
            else this.showOrderBookView(status, stats, participants, animate);
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            if (token === this.loadToken) showErrorAlert("Could not load the event", error.message);
        }
    }

    refreshRight() {
        if (!this.current) {
            this.clear();
            return;
        }
        this.loadAndShow(this.current, true);
        this.updateManagementButton();
    }

    /** No event selected: the full structure stays on screen with placeholder values. */
    clear() {
        this.current = null;
        this.loadToken++;
        this.updateManagementButton();
        this.updateTradingControls();
        this.showPlaceholders();
    }

    showPlaceholders() {
        $("lmsr-pane").hidden = false;
        this.ob.pane.hidden = true;
        $("event-details-title").textContent = "Event details and trade";
        this.lmsr.name.textContent = EMPTY;
        this.lmsr.status.textContent = EMPTY;
        this.lmsr.balance.textContent = "0.00";
        this.lmsr.commission.textContent = "0.00";
        this.lmsr.description.textContent = "";
        this.lmsr.options.forEach((option, i) => {
            option.title.textContent = "Option " + (i + 1);
            option.table.setRows([], { scope: "none", animate: false });
            option.field.value = "";
        });
        this.lmsr.participations.setRows([], { scope: "none", animate: false });
    }

    showLmsrView(status, animate) {
        const scope = "event-" + this.current.id;
        $("lmsr-pane").hidden = false;
        this.ob.pane.hidden = true;

        this.lmsr.name.textContent = status.eventName;
        this.lmsr.status.textContent = ({ NOT_STARTED: "Not started", OPEN: "Open", CLOSED: "Closed" })[status.status];
        this.lmsr.balance.textContent = fmt2(status.accountBalance);
        this.lmsr.commission.textContent = fmt2(status.totalCommissionPaid);
        this.lmsr.description.textContent = this.current.description || "";

        status.optionTradingStatus.forEach((option, i) => {
            const view = this.lmsr.options[i];
            if (!view) return;
            view.title.textContent = option.optionName;
            view.table.setRows([option], { scope, animate });
        });
        this.lmsr.participations.setRows(status.tradingHistory, { scope, animate });
    }

    showOrderBookView(status, stats, participants, animate) {
        const scope = "event-" + this.current.id;
        $("lmsr-pane").hidden = true;
        this.ob.pane.hidden = false;

        status.optionTradingStatus.forEach((option, i) => {
            const view = this.ob.options[i];
            if (!view) return;
            view.title.textContent = option.optionName;
            view.value.textContent = fmt2(option.currentValue);
            view.shares.textContent = option.totalSharesBought;
            view.table.setRows(option.restingOrders || [], { scope, animate });

            const s = stats[i] || {};
            view.stats.LAST.textContent = fmt2(s.last);
            view.stats.BID.textContent = fmt2(s.bestBid);
            view.stats.ASK.textContent = fmt2(s.bestAsk);
            view.stats.MID.textContent = fmt2(s.mid);
            view.stats.SPREAD.textContent = fmt2(s.spread);
        });

        this.ob.participations.setRows(participants, { scope, animate });
        this.ob.balance.textContent = fmt2(status.accountBalance);
        this.ob.commission.textContent = fmt2(status.totalCommissionPaid);
    }

    // =====================================================================
    // Activate / Close (market maker only)
    // =====================================================================

    /** Green "Activate Event", red "Close Event", or a disabled status for everyone else. */
    updateManagementButton() {
        const button = $("event-status-button");
        const event = this.current;
        button.classList.remove("success-button", "danger-button", "status-open-button");

        if (!event) {
            button.textContent = "Close Event";
            button.classList.add("danger-button");
            button.disabled = true;
            return;
        }

        const isMaker = this.app.isMarketMakerOf(event.id);
        switch (event.status) {
            case "NOT_STARTED":
                button.textContent = isMaker ? "Activate Event" : "Not Started";
                button.classList.add("success-button");
                button.disabled = !isMaker;
                break;
            case "OPEN":
                button.textContent = isMaker ? "Close Event" : "Event is open";
                button.classList.add(isMaker ? "danger-button" : "status-open-button");
                button.disabled = !isMaker;
                break;
            default:
                button.textContent = "Event Closed";
                button.disabled = true;
        }
    }

    async manageEvent() {
        const event = this.current;
        if (!event) return;
        if (event.status === "NOT_STARTED") await this.activateEvent(event);
        else if (event.status === "OPEN") await this.closeEvent(event);
    }

    async activateEvent(event) {
        try {
            await api.activateEvent(event.id);
            await this.onEventChanged();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            showErrorAlert("Could not activate the event", error.message);
        }
    }

    async closeEvent(event) {
        const optionNames = event.options.map((o) => o.optionName);
        const chosen = await showChoiceDialog("Close Event", `Closing "${event.name}"`, "Winning option:", optionNames);
        if (chosen === null) return;

        try {
            await api.closeEvent(event.id, optionNames.indexOf(chosen) + 1);
            await this.onEventChanged();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            showErrorAlert("Could not close the event", error.message);
        }
    }

    /** After activate/close: reload the list (fresh status) and re-show the same event. */
    async onEventChanged() {
        await this.loadEvents({ animate: true });
    }

    // =====================================================================
    // trading
    // =====================================================================

    /** Fields work only for an open event; a button also needs its fields filled. */
    updateTradingControls() {
        const tradingAvailable = Boolean(this.current && this.current.status === "OPEN" && this.app.user);

        for (const option of this.lmsr.options) {
            option.field.disabled = !tradingAvailable;
            option.button.disabled = !tradingAvailable || option.field.value.trim() === "";
        }
        for (const option of this.ob.options) {
            option.side.disabled = !tradingAvailable;
            option.sharesField.disabled = !tradingAvailable;
            option.priceField.disabled = !tradingAvailable;
            option.button.disabled = !tradingAvailable
                || option.sharesField.value.trim() === ""
                || option.priceField.value.trim() === ""
                || !option.side.value;
        }
    }

    async placeLmsrBet(option) {
        const event = this.current;
        if (!event) return;

        const shares = Number.parseInt(option.field.value.trim(), 10);
        if (!Number.isInteger(shares)) {
            return showErrorAlert("Could not place the bet", "Enter the number of shares to buy.");
        }

        try {
            await api.participateInEvent(event.id, option.number, shares, "BUY", null);
            option.field.value = "";
            this.updateTradingControls();
            await this.onParticipationChanged();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            showErrorAlert("Could not place the bet", error.message);
        }
    }

    async placeOrder(option) {
        const event = this.current;
        if (!event || !this.app.user) {
            return showErrorAlert("Could not place the order", "Select an event and a user first.");
        }

        const shares = Number.parseInt(option.sharesField.value.trim(), 10);
        const price = Number.parseFloat(option.priceField.value.trim());
        if (!Number.isInteger(shares) || Number.isNaN(price)) {
            return showErrorAlert("Could not place the order", "Enter both a quantity and a price per share.");
        }

        const side = option.side.value === "Bid" ? "BUY" : "SELL";
        try {
            await api.participateInEvent(event.id, option.number, shares, side, price);
            option.sharesField.value = "";
            option.priceField.value = "";
            this.updateTradingControls();
            await this.onParticipationChanged();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            showErrorAlert("Could not place the order", error.message);
        }
    }

    /** After a trade: list and details reload without the fade-in (reloadEventsWithoutAnimation). */
    async onParticipationChanged() {
        await this.loadEvents({ animate: false });
    }
}
