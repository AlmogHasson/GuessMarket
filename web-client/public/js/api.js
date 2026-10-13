/*
 * All calls to the Guess Market server - the web twin of the desktop client's
 * HttpClientUtil. The endpoints, parameters and JSON bodies are exactly the ones
 * the JavaFX client uses; the browser keeps the session cookie by itself.
 *
 * Requests go to /guess-market/... on this same origin; server.js forwards them
 * to Tomcat on localhost:8080.
 */

export const BASE_PATH = "/guess-market";
export const SERVER_URL = "http://localhost:8080" + BASE_PATH;

/** Thrown for every failed call; status is 0 when the server could not be reached. */
export class ServerError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

/**
 * Turns an error response into one readable sentence. The servlets answer
 * either with plain text or, when they use sendError, with Tomcat's HTML error
 * page - in that case the "Message" line of the page is the useful part.
 */
function readableMessage(status, text) {
    const body = (text || "").trim();

    if (/^<!doctype html|^<html/i.test(body)) {
        const page = new DOMParser().parseFromString(body, "text/html");
        const message = [...page.querySelectorAll("p")]
            .find((p) => p.querySelector("b")?.textContent.trim() === "Message");
        if (message) {
            return message.textContent.replace(/^\s*Message\s*/, "").trim();
        }
        return `The server returned ${status}.`;
    }
    if (status === 401 && !body) {
        return "You are not logged in.";
    }
    return body || `The server returned ${status}.`;
}

async function request(method, path, { params, json, keepalive } = {}) {
    let url = BASE_PATH + path;
    if (params) {
        url += "?" + new URLSearchParams(params).toString();
    }

    const options = { method, credentials: "same-origin", keepalive: Boolean(keepalive), headers: {} };
    if (json !== undefined) {
        options.headers["Content-Type"] = "application/json; charset=utf-8";
        options.body = JSON.stringify(json);
    }

    let response;
    try {
        response = await fetch(url, options);
    } catch {
        throw new ServerError(0, "Could not communicate with the server.");
    }

    const text = await response.text();
    if (!response.ok) {
        throw new ServerError(response.status, readableMessage(response.status, text));
    }
    return text;
}

async function getJson(path, params) {
    const text = await request("GET", path, { params });
    if (!text.trim()) {
        // /events answers an empty 200 when the session is gone
        throw new ServerError(401, "You are not logged in.");
    }
    return JSON.parse(text);
}

async function postJson(path, body) {
    const text = await request("POST", path, { json: body });
    return text.trim() ? JSON.parse(text) : null;
}

// ---------------- session ----------------

export const login = (userName) => request("POST", "/login", { params: { username: userName } });
export const logout = () => request("POST", "/logout");
export const getCurrentUser = () => getJson("/user");
export const getVersion = async () => Number((await request("GET", "/version")).trim());

// ---------------- events ----------------

export const getEvents = () => getJson("/events");
export const getEventTradingStatus = (id) => getJson("/event/status", { id });
export const getOrderBookStats = (id) => getJson("/event/stats", { id });
export const getEventParticipants = (id) => getJson("/event/participants", { id });
export const activateEvent = (id) => request("POST", "/event/activate", { params: { id } });
export const closeEvent = (eventId, winningOption) => postJson("/event/close", { eventId, winningOption });

/** side is "BUY" or "SELL"; price is null for LMSR. */
export const participateInEvent = (eventId, optionNumber, shares, side, price) =>
    postJson("/event/participate", { eventId, optionNumber, shares, side, price });

// ---------------- users ----------------

export const getUsers = () => getJson("/users");
export const getUserEvents = () => getJson("/user/events");
export const getUserOrderBookPosition = (id) => getJson("/user/order-book-position", { id });
export const getAccountTransactions = () => getJson("/user/transactions");
export const deposit = (amount) => postJson("/user/deposit", { amount });

// ---------------- chat (bonus) ----------------

export const getChatMessages = (since) => getJson("/chat", { since });
export const sendChatMessage = (text) => postJson("/chat", { text });
