/*
 * Guess Market web client - local web server.
 *
 * Serves the web client from ./public and forwards every request under
 * /guess-market/ unchanged to the Guess Market server (Tomcat, port 8080).
 *
 * Why a forwarder: the browser loads the client from http://localhost:3000.
 * If the pages called http://localhost:8080 directly, the browser would treat
 * that as another site - it would block the responses (CORS) and would not keep
 * the server's session cookie. Going through this server, the browser only ever
 * talks to localhost:3000, so it is one site and the existing server needs no
 * change at all.
 *
 * No dependencies: only Node's built-in modules, so there is nothing to install.
 */
"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");
const { exec } = require("child_process");

const PORT = Number(process.env.PORT) || 3000;
const SERVER_HOST = "localhost";
const SERVER_PORT = 8080;
const CONTEXT_PATH = "/guess-market";
const PUBLIC_DIR = path.join(__dirname, "public");

const CONTENT_TYPES = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".ico": "image/x-icon",
    ".ttf": "font/ttf",
};

/** Forwards one request to the Guess Market server and streams the answer back. */
function forwardToServer(req, res) {
    const headers = { ...req.headers, host: `${SERVER_HOST}:${SERVER_PORT}` };
    delete headers.origin;
    delete headers.referer;

    const upstream = http.request(
        { host: SERVER_HOST, port: SERVER_PORT, method: req.method, path: req.url, headers },
        (answer) => {
            res.writeHead(answer.statusCode, answer.headers);
            answer.pipe(res);
        }
    );

    upstream.on("error", () => {
        if (res.headersSent) {
            res.end();
            return;
        }
        res.writeHead(502, { "Content-Type": "text/plain; charset=utf-8" });
        res.end(
            `The Guess Market server is not reachable at http://${SERVER_HOST}:${SERVER_PORT}${CONTEXT_PATH}. ` +
            "Make sure Tomcat is running and guess-market.war is deployed."
        );
    });

    req.pipe(upstream);
}

/** Serves a file from ./public; unknown paths get the single page (index.html). */
function serveStatic(req, res) {
    const urlPath = decodeURIComponent(new URL(req.url, "http://localhost").pathname);
    let file = path.normalize(path.join(PUBLIC_DIR, urlPath));

    if (!file.startsWith(PUBLIC_DIR)) {
        res.writeHead(403);
        res.end();
        return;
    }
    if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) {
        file = path.join(PUBLIC_DIR, "index.html");
    }

    const type = CONTENT_TYPES[path.extname(file).toLowerCase()] || "application/octet-stream";
    res.writeHead(200, { "Content-Type": type, "Cache-Control": "no-cache" });
    fs.createReadStream(file).pipe(res);
}

const server = http.createServer((req, res) => {
    if (req.url === CONTEXT_PATH || req.url.startsWith(CONTEXT_PATH + "/") || req.url.startsWith(CONTEXT_PATH + "?")) {
        forwardToServer(req, res);
    } else if (req.method === "GET" || req.method === "HEAD") {
        serveStatic(req, res);
    } else {
        res.writeHead(405);
        res.end();
    }
});

server.on("error", (error) => {
    if (error.code === "EADDRINUSE") {
        console.error(`Port ${PORT} is already in use. Close the program using it, or run: set PORT=3001 && node server.js`);
    } else {
        console.error(error.message);
    }
    process.exit(1);
});

server.listen(PORT, () => {
    const url = `http://localhost:${PORT}`;
    console.log("Guess Market web client is running.");
    console.log(`  Open:   ${url}`);
    console.log(`  Server: http://${SERVER_HOST}:${SERVER_PORT}${CONTEXT_PATH} (Tomcat must be running)`);
    console.log("  Keep this window open while using the client. Close it to stop.");

    if (process.argv.includes("--open")) {
        const command = process.platform === "win32" ? `start "" "${url}"`
            : process.platform === "darwin" ? `open "${url}"` : `xdg-open "${url}"`;
        exec(command, () => { /* opening the browser is best effort */ });
    }
});
