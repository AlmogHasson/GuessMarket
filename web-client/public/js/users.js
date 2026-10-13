/*
 * The Users tab - the web twin of UsersTabController, UsersLeftController and
 * UsersRightController (without Load File, which the exercise leaves to the
 * desktop client): every other user, the logged-in user's account rows,
 * balance and Deposit, and the user's own events with per-event details.
 */
import * as api from "./api.js";
import {
    EMPTY, Table, fmt2, fmtSigned, statusText, showErrorAlert, showDepositDialog,
} from "./ui.js";

const $ = (id) => document.getElementById(id);

export class UsersTab {
    constructor(app, refreshActiveUser) {
        this.app = app;
        this.refreshActiveUser = refreshActiveUser;
        this.detailsToken = 0;

        this.initLeft();
        this.initRight();

        app.onUserChanged((user) => this.showAccount(user));
        $("deposit-button").addEventListener("click", () => this.deposit());
    }

    // =====================================================================
    // left: users + account
    // =====================================================================

    initLeft() {
        this.usersTable = new Table($("users-table"), {
            columns: [
                { title: "Name", value: (u) => u.name, width: 50 },
                { title: "Account Balance", value: (u) => fmt2(u.accountBalance), width: 140 },
                { title: "Is Market Maker", value: (u) => (u.eventsIDs.length ? "Yes" : "No"), width: 110 },
            ],
            key: (u) => u.name,
            placeholder: "No other users",
        });

        this.accountTable = new Table($("account-table"), {
            columns: [
                { title: "Event", value: (t) => (t.eventName && t.eventName.trim() ? t.eventName : "-"), width: 70 },
                { title: "Activity", value: (t) => t.activity, width: 110 },
                { title: "Amount", value: (t) => fmtSigned(t.amount), width: 60 },
                { title: "Balance", value: (t) => fmt2(t.balanceAfter), width: 70 },
            ],
            key: (t) => t.id,
            placeholder: "No activity yet",
        });
    }

    showAccount(user) {
        $("account-balance").textContent = user ? fmt2(user.accountBalance) : "0.00";
        $("deposit-button").disabled = !user;
    }

    async loadUsers() {
        const users = await api.getUsers();
        const me = this.app.user?.name?.toLowerCase();
        const others = users.filter((u) => u.name.toLowerCase() !== me);
        this.usersTable.setRows(others, { scope: "users" });
    }

    async loadAccountTransactions() {
        const rows = (await api.getAccountTransactions()).slice().reverse();     // newest first
        this.accountTable.setPlaceholder("No account transactions");
        this.accountTable.setRows(rows, { scope: "account-" + this.app.user?.name });
    }

    async deposit() {
        const amount = await showDepositDialog();
        if (amount === null) return;
        try {
            const updated = await api.deposit(amount);
            this.app.setUser(updated);
            await this.loadAccountTransactions();
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            showErrorAlert("Could not deposit", error.message);
        }
    }

    // =====================================================================
    // right: the user's events and one event's details
    // =====================================================================

    initRight() {
        this.userEventsTable = new Table($("user-events-table"), {
            columns: [
                { title: "Event", value: (r) => r.eventName, width: 180 },
                { title: "Role", value: (r) => r.role, width: 100 },
                { title: "Event Status", value: (r) => statusText(r.eventStatus), width: 110 },
                { title: "Shares", value: (r) => r.totalShares, width: 90 },
            ],
            key: (r) => r.eventId,
            selectable: true,
            placeholder: "Select a user",
            onSelect: (row) => this.showParticipation(row),
        });

        this.lmsrTable = new Table($("user-lmsr-table"), {
            columns: [
                { title: "Option", value: (t) => t.optionName, width: 140 },
                { title: "Shares", value: (t) => t.sharesBought, width: 90 },
                { title: "Total Paid", value: (t) => fmt2(t.pricePaid), width: 100 },
                { title: "Total Commission Paid", value: (t) => fmt2(t.commissionPaid), width: 110 },
            ],
            key: (t) => t.id,
            placeholder: "Select an event",
        });

        this.orderBookTable = new Table($("user-ob-table"), {
            columns: [
                { title: "Option", value: (t) => t.optionName, width: 180 },
                { title: "Side", value: (t) => t.side, width: 65 },
                { title: "Shares", value: (t) => t.sharesBought, width: 120 },
                { title: "Total Paid / Received", value: (t) => fmt2(t.pricePaid), width: 120 },
                { title: "Commission Paid", value: (t) => fmt2(t.commissionPaid), width: 110 },
            ],
            key: (t) => t.id,
            placeholder: "Select an event",
        });
    }

    async showUserEvents() {
        const events = await api.getUserEvents();
        const previous = this.userEventsTable.selectedKey;

        if (events.length === 0) {
            this.userEventsTable.setPlaceholder("No events");
            this.userEventsTable.setRows([], { scope: "user-events" });
            this.userEventsTable.select(null);
            this.clearParticipation("Select an event");
            return;
        }

        this.userEventsTable.setRows(events, { scope: "user-events-" + this.app.user?.name });
        const keep = previous !== null && events.some((e) => String(e.eventId) === previous);
        this.userEventsTable.select(keep ? previous : events[0].eventId);
        await this.showParticipation(this.userEventsTable.selectedRow);
    }

    showTable(lmsr) {
        $("user-lmsr-table").hidden = !lmsr;
        $("user-ob-table").hidden = lmsr;
    }

    clearParticipation(placeholder) {
        this.detailsToken++;
        this.showTable(true);
        this.lmsrTable.setPlaceholder(placeholder);
        this.lmsrTable.setRows([], { scope: "none", animate: false });
        this.orderBookTable.setPlaceholder(placeholder);
        this.orderBookTable.setRows([], { scope: "none", animate: false });
        this.setSummary(null, null);
    }

    async showParticipation(row) {
        if (!row || !this.app.user || row.eventStatus === "NOT_STARTED") {
            this.clearParticipation(row && row.eventStatus === "NOT_STARTED" ? "Event not started yet" : "Select an event");
            return;
        }

        const token = ++this.detailsToken;
        const userName = this.app.user.name;
        const scope = "participation-" + userName + "-" + row.eventId;

        try {
            if (row.methodType === "order book") {
                const [position, status] = await Promise.all([
                    api.getUserOrderBookPosition(row.eventId),
                    api.getEventTradingStatus(row.eventId),
                ]);
                if (token !== this.detailsToken) return;

                this.showTable(false);
                const history = status.tradingHistory.filter((t) => t.userName === userName);
                this.orderBookTable.setPlaceholder("No transactions in this event");
                this.orderBookTable.setRows(history, { scope });

                let closedText = null;
                if (position.eventClosed) {
                    const winner = status.optionTradingStatus.find((o) => o.isWinner)?.optionName ?? EMPTY;
                    closedText = `Winning Option: ${winner}   —   Profit / Loss: ${fmt2(position.profitLoss)}`;
                }
                this.setSummary(`Total commission Paid: ${fmt2(position.commissionPaid)}`, closedText);
            } else {
                const status = await api.getEventTradingStatus(row.eventId);
                if (token !== this.detailsToken) return;

                this.showTable(true);
                const trades = status.tradingHistory.filter((t) => t.userName === userName);
                if (trades.length === 0) {
                    this.lmsrTable.setPlaceholder(({
                        NOT_STARTED: "Event not started yet",
                        OPEN: "No participation yet",
                        CLOSED: "No participation in this event",
                    })[status.status]);
                }
                this.lmsrTable.setRows(trades, { scope });

                let closedText = null;
                if (status.status === "CLOSED") {
                    const perOption = status.optionTradingStatus
                        .map((o) => `${o.optionName}: ${o.totalSharesBought}`).join(", ");
                    const winner = status.optionTradingStatus.find((o) => o.isWinner)?.optionName ?? EMPTY;
                    closedText = `Total shares per option: ${perOption}  —  Winning Option: ${winner}`;
                }
                this.setSummary(null, closedText);
            }
        } catch (error) {
            if (error.status === 401) return this.app.sessionLost();
            if (token === this.detailsToken) showErrorAlert("Could not load participation", error.message);
        }
    }

    setSummary(commissionText, closedText) {
        const commission = $("commission-summary");
        const closed = $("closed-summary");
        commission.textContent = commissionText || "";
        commission.hidden = !commissionText;
        closed.textContent = closedText || "";
        closed.hidden = !closedText;
        $("participation-summary").hidden = !commissionText && !closedText;
    }

    // =====================================================================

    reset() {
        this.usersTable.setRows([], { scope: "users", animate: false });
        this.accountTable.setPlaceholder("No activity yet");
        this.accountTable.setRows([], { scope: "account", animate: false });
        this.userEventsTable.select(null);
        this.userEventsTable.setPlaceholder("Select a user");
        this.userEventsTable.setRows([], { scope: "user-events", animate: false });
        this.clearParticipation("Select an event");
        this.showAccount(this.app.user);
    }

    /** Reloads everything on the tab; one refresh at a time, a request during one runs once after it. */
    async refresh() {
        if (this.refreshing) {
            this.refreshPending = true;
            return;
        }
        this.refreshing = true;
        try {
            await this.refreshActiveUser();
            if (!this.app.user) return;
            await Promise.all([this.loadUsers(), this.loadAccountTransactions(), this.showUserEvents()]);
        } catch (error) {
            if (error.status === 401) this.app.sessionLost();
            else showErrorAlert("Could not refresh user data", error.message);
        } finally {
            this.refreshing = false;
            if (this.refreshPending) {
                this.refreshPending = false;
                this.refresh();
            }
        }
    }
}
