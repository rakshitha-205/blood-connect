import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig, loadEnv } from "vite";

const FRONTEND = dirname(fileURLToPath(import.meta.url));
const PAGES = resolve(FRONTEND, "pages");
const HOME_PAGE = "index";
const MOVED_PAGES = {
  "matching-results": "/emergency-request?tab=matches",
  "request-tracking": "/emergency-request?tab=track",
  "donor-dashboard": "/donations",
};

const pageFor = (name) => (existsSync(resolve(PAGES, `${name}.html`)) ? `/pages/${name}.html` : null);

const bloodConnectPages = (apiUrl) => ({
  name: "bloodconnect-pages",
  configureServer: (server) => {
    server.watcher.on("change", () => server.ws.send({ type: "full-reload" }));
    server.middlewares.use((req, res, next) => {
      const [path, query = ""] = req.url.split("?");
      const search = query ? `?${query}` : "";
      if (path.startsWith("/assets/")) {
        // Vite marks any "?v=" URL as immutable, so browsers would never fetch edited files again.
        const params = new URLSearchParams(query);
        params.delete("v");
        req.url = params.size ? `${path}?${params}` : path;
        return next();
      }
      if (path.startsWith("/@") || path.startsWith("/node_modules/")) return next();

      if (path.endsWith(".html") && !query.includes("html-proxy")) {
        const name = path.split("/").pop().slice(0, -".html".length);
        res.statusCode = 301;
        res.setHeader("Location", `${name === HOME_PAGE ? "/" : `/${name}`}${search}`);
        res.end();
        return;
      }

      const name = path.replace(/^\/+|\/+$/g, "") || HOME_PAGE;
      if (MOVED_PAGES[name]) {
        res.statusCode = 301;
        res.setHeader("Location", MOVED_PAGES[name]);
        res.end();
        return;
      }
      const page = name.includes("/") ? null : pageFor(name);
      if (!page) {
        res.statusCode = 404;
        res.setHeader("Content-Type", "text/plain; charset=utf-8");
        res.end("Page not found");
        return;
      }
      req.url = `${page}${search}`;
      next();
    });
  },
  transformIndexHtml: () => [
    {
      tag: "script",
      children: `window.BLOODCONNECT_API_URL = ${JSON.stringify(apiUrl)};`,
      injectTo: "head-prepend",
    },
  ],
});

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, resolve(FRONTEND, "config"), "VITE_");
  return {
    root: FRONTEND,
    envDir: "config",
    appType: "mpa",
    publicDir: false,
    plugins: [bloodConnectPages(env.VITE_API_URL || "http://localhost:8080")],
    server: {
      host: "localhost",
      port: 5500,
      strictPort: true,
      open: "/",
    },
  };
});
