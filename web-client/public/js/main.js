/*
 * Entry point and "MainController" of the web client: the login page, the top
 * bar (active user, Log out, Animations, theme), the two tabs, and the pull loop
 * that keeps every screen in step with the server - the same 500 ms version
 * polling the desktop client uses.
 */
import * as api from "./api.js";
import {
    setOptions, markPrompt, showErrorAlert, initSplitPane,
    setAnimationsEnabled, areAnimationsEnabled,
} from "./ui.js";
import { EventsTab } from "./events.js";
import { UsersTab } from "./users.js";
import { Chat } from "./chat.js";

const REFRESH_RATE_MILLIS = 500;
const THEMES = ["Light", "Dark", "Japanese"];
const DEFAULT_THEME = "Dark";

const $ = (id) => document.getElementById(id);

/** Shared state the tabs read, like MainController's properties in the desktop client. */
export const app = {
    user: null,                 // UserDTO of the logged-in user
    listeners: new Set(),       // called with the user whenever it changes

    setUser(user) {
        this.user = user;
        this.listeners.forEach((listener) => listener(user));
    },

    onUserChanged(listener) {
        this.listeners.add(listener);
    },

    isMarketMakerOf(eventId) {
        return Boolean(this.user?.eventsIDs?.includes(eventId));
    },

    sessionLost() {
        if (this.user) {
            leaveMain("Your session has ended. Please log in again.");
        }
    },
};

// ---------------- theme ----------------

let currentTheme = DEFAULT_THEME;

function readStoredTheme() {
    try {
        const stored = localStorage.getItem("gm-theme");
        return THEMES.includes(stored) ? stored : DEFAULT_THEME;
    } catch {
        return DEFAULT_THEME;
    }
}

function applyTheme(theme) {
    currentTheme = theme;
    document.documentElement.dataset.theme = theme.toLowerCase();
    for (const select of document.querySelectorAll(".theme-select")) {
        select.value = theme;
    }
    try {
        localStorage.setItem("gm-theme", theme);
    } catch {
        /* storage unavailable - the theme just is not remembered */
    }
}

/** Theme swap; with Animations on, the screen fades out and back in (TopController.setTheme). */
function changeTheme(theme) {
    if (theme === currentTheme) return;
    if (!areAnimationsEnabled() || $("main-view").hidden) {
        applyTheme(theme);
        return;
    }
    document.body.classList.add("theme-fading");
    setTimeout(() => {
        applyTheme(theme);
        document.body.classList.remove("theme-fading");
    }, 250);
}

function initThemeSelectors() {
    for (const select of document.querySelectorAll(".theme-select")) {
        setOptions(select, THEMES);
        select.addEventListener("change", () => changeTheme(select.value));
    }
    applyTheme(readStoredTheme());
}

// ---------------- login page ----------------

/** English letters, digits, space, underscore and hyphen - input is English only. */
const USER_NAME_CHARS = /^[A-Za-z0-9 _-]*$/;

function showLoginError(message) {
    const label = $("login-error");
    label.textContent = message || "";
    label.hidden = !message;
}

function initLogin() {
    const field = $("login-name");
    const button = $("login-button");
    let lastGood = "";

    $("server-label").textContent = "Server: " + api.SERVER_URL;

    field.addEventListener("input", () => {
        if (!USER_NAME_CHARS.test(field.value)) {
            field.value = lastGood;
        }
        lastGood = field.value;
        button.disabled = field.value.trim() === "";
        showLoginError("");
    });

    $("login-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const userName = field.value.trim();
        if (!userName) {
            showLoginError("Please enter a user name.");
            return;
        }

        field.disabled = true;
        button.disabled = true;
        showLoginError("");

        try {
            enterMain(await loginAs(userName));
        } catch (error) {
            showLoginError(loginErrorText(error));
            field.disabled = false;
            button.disabled = field.value.trim() === "";
            field.focus();
            field.select();
        }
    });
}

function loginErrorText(error) {
    if (error.status === 409) return "This user is already logged in.";
    if (error.status === 0) return "Could not connect to the server.";
    return error.message || "Login failed.";
}

/**
 * Logs in, minding that all tabs of one browser share one session cookie.
 * If this browser is already logged in (in another tab), only that same user
 * can continue here; logging in as someone else would silently take the other
 * tab's session, so it is refused with an explanation instead.
 */
async function loginAs(userName) {
    let existing = null;
    try {
        existing = await api.getCurrentUser();
    } catch (error) {
        if (error.status !== 401) throw error;
    }

    if (existing) {
        if (existing.name.toLowerCase() === userName.toLowerCase()) {
            return existing;
        }
        const error = new Error(
            `This browser is already logged in as ${existing.name} in another tab or window. ` +
            "Log out there first, or use another browser (or a private window) for a second user.");
        error.status = "other-tab";
        throw error;
    }

    await api.login(userName);
    return api.getCurrentUser();
}

function showLogin(message) {
    document.title = "Guess Market - Login";
    $("main-view").hidden = true;
    $("login-view").hidden = false;
    const field = $("login-name");
    field.disabled = false;
    $("login-button").disabled = field.value.trim() === "";
    showLoginError(message || "");
    field.focus();
}

// ---------------- main screen ----------------

let eventsTab;
let usersTab;
let chat;
let selectedTab = "events";
let pollTimer = null;
let lastVersion = -1;
let pollInFlight = false;

function showActiveUser(user) {
    const label = $("active-user");
    const none = !user || !user.name;
    label.textContent = none ? "No active user" : "Acting as: " + user.name.toUpperCase();
    label.classList.toggle("active-user-none", none);
}

function enterMain(user) {
    $("login-view").hidden = true;
    $("main-view").hidden = false;
    document.title = "Guess Market - " + user.name;
    $("logout-button").disabled = false;

    rememberTabUser(user.name);
    app.setUser(user);
    announceTab();
    eventsTab.reset();
    usersTab.reset();
    selectTab("events", false);
    eventsTab.loadEvents({ animate: true });
    chat.start();
    startPolling();
}

function leaveMain(message) {
    stopPolling();
    chat.stop();
    app.setUser(null);
    rememberTabUser(null);
    announceTab();
    $("login-name").value = "";
    showLogin(message);
}

async function logout() {
    const button = $("logout-button");
    button.disabled = true;
    try {
        await api.logout();
        leaveMain();
    } catch (error) {
        if (error.status === 401) {
            leaveMain();        // the session was already gone - nothing left to log out of
            return;
        }
        button.disabled = false;
        showErrorAlert("Logout Failed", error.message);
    }
}

// ---------------- tabs (with the sliding indicator) ----------------

function placeTabIndicator() {
    const tab = document.querySelector(`.tab[data-tab="${selectedTab}"]`);
    const indicator = $("tab-indicator");
    if (!tab || !indicator) return;
    indicator.style.transform = `translateX(${tab.offsetLeft}px)`;
    indicator.style.width = `${tab.offsetWidth}px`;
}

function selectTab(name, refresh = true) {
    selectedTab = name;
    for (const tab of document.querySelectorAll(".tab")) {
        const selected = tab.dataset.tab === name;
        tab.classList.toggle("selected", selected);
        tab.setAttribute("aria-selected", String(selected));
    }
    $("events-tab").hidden = name !== "events";
    $("users-tab").hidden = name !== "users";
    placeTabIndicator();

    // like the desktop's onSelectionChanged: the tab that opens is refreshed
    if (refresh) {
        if (name === "events") eventsTab.refresh();
        else usersTab.refresh();
    }
}

// ---------------- pull: version polling ----------------

function startPolling() {
    stopPolling();
    lastVersion = -1;
    pollTimer = setInterval(poll, REFRESH_RATE_MILLIS);
    poll();
}

function stopPolling() {
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = null;
}

async function poll() {
    if (pollInFlight || !app.user) return;
    pollInFlight = true;
    try {
        const version = await api.getVersion();
        const changed = lastVersion !== -1 && version !== lastVersion;
        lastVersion = version;
        if (changed) {
            onServerChanged();
        }
    } catch (error) {
        if (error.status === 401 && app.user) {
            app.sessionLost();
        }
        // anything else (server briefly unreachable): try again on the next tick
    } finally {
        pollInFlight = false;
    }
}

/** Something changed on the server: refresh the visible tab and the logged-in user. */
function onServerChanged() {
    if (selectedTab === "events") {
        eventsTab.refresh();
        refreshActiveUser();
    } else {
        usersTab.refresh();     // also refreshes the user
    }
}

async function refreshActiveUser() {
    try {
        const user = await api.getCurrentUser();
        if (app.user && user.name.toLowerCase() !== app.user.name.toLowerCase()) {
            // another tab of this browser logged in as someone else with the shared cookie
            leaveMain(`This browser is now logged in as ${user.name} in another tab.`);
            return;
        }
        app.setUser(user);
    } catch (error) {
        if (error.status === 401) app.sessionLost();
    }
}

// ---------------- the session of this browser tab ----------------

/** sessionStorage is per tab and survives a reload, so a reload can log the same user back in. */
function rememberTabUser(name) {
    try {
        if (name) sessionStorage.setItem("gm-user", name);
        else sessionStorage.removeItem("gm-user");
    } catch {
        /* storage unavailable: a reload simply shows the login page */
    }
}

function rememberedTabUser() {
    try {
        return sessionStorage.getItem("gm-user");
    } catch {
        return null;
    }
}

/*
 * All tabs of one browser share one session. The tabs tell each other who they
 * are logged in as, so closing one tab does not log out another tab that is
 * still using the same session.
 */
const tabId = Math.random().toString(36).slice(2) + Date.now().toString(36);
const otherTabs = new Map();            // tab id -> user name (null on its login page)
const tabChannel = "BroadcastChannel" in window ? new BroadcastChannel("guess-market-tabs") : null;

function announceTab(type = "hello") {
    tabChannel?.postMessage({ type, id: tabId, user: app.user?.name ?? null });
}

tabChannel?.addEventListener("message", ({ data }) => {
    if (data.type === "bye") {
        otherTabs.delete(data.id);
        return;
    }
    otherTabs.set(data.id, data.user);
    if (data.type === "hello") announceTab("here");     // let a new tab know about this one
});

function anotherTabUses(name) {
    return [...otherTabs.values()].some((user) => user && user.toLowerCase() === name.toLowerCase());
}

/**
 * Closing the tab logs the user out, like closing the desktop window does, so
 * the name is free again - unless another tab of this browser still uses the
 * session. A reload also passes through here; the new page then logs the same
 * user straight back in (resumeAfterReload).
 */
function logoutOnClose() {
    tabChannel?.postMessage({ type: "bye", id: tabId });
    if (app.user && !anotherTabUses(app.user.name)) {
        navigator.sendBeacon(api.BASE_PATH + "/logout");
    }
}

const pause = (millis) => new Promise((resolve) => setTimeout(resolve, millis));

async function resumeAfterReload(name) {
    for (let attempt = 0; attempt < 4; attempt++) {
        try {
            enterMain(await loginAs(name));
            return;
        } catch (error) {
            if (error.status !== 409) {       // anything but "still logged in" will not fix itself
                $("login-name").value = name;
                showLogin(loginErrorText(error));
                return;
            }
            await pause(400);                 // the previous page's logout may still be on its way
        }
    }
    $("login-name").value = name;
    showLogin("This user is already logged in.");
}

// ---------------- start-up ----------------

async function start() {
    initThemeSelectors();
    initLogin();
    document.querySelectorAll(".split-pane").forEach(initSplitPane);

    eventsTab = new EventsTab(app);
    usersTab = new UsersTab(app, refreshActiveUser);
    chat = new Chat(app);

    app.onUserChanged(showActiveUser);

    $("logout-button").addEventListener("click", logout);
    $("animations-check").addEventListener("change", (e) => setAnimationsEnabled(e.target.checked));
    for (const tab of document.querySelectorAll(".tab")) {
        tab.addEventListener("click", () => {
            if (tab.dataset.tab !== selectedTab) selectTab(tab.dataset.tab);
        });
    }
    window.addEventListener("resize", placeTabIndicator);
    for (const select of document.querySelectorAll("select.combo-box")) {
        select.addEventListener("change", () => markPrompt(select));
    }

    window.addEventListener("pagehide", logoutOnClose);
    announceTab();

    const remembered = rememberedTabUser();
    if (remembered) {
        await resumeAfterReload(remembered);
    } else {
        showLogin();
    }
}

start();
