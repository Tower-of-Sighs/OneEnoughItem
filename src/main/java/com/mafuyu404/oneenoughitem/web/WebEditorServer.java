package com.mafuyu404.oneenoughitem.web;

import com.google.gson.*;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.resources.language.I18n;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebEditorServer {
    private static volatile HttpServer server;
    private static volatile int port = -1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STATIC_PREFIX = "/web/";
    private static final String ASSET_BASE = "assets/oneenoughitem/web/";

    public static synchronized void startIfNeeded() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();
            server.createContext("/", WebEditorServer::handleIndex);
            server.createContext(STATIC_PREFIX, WebEditorServer::handleStatic);
            server.createContext("/api/load", WebEditorServer::handleApiLoad);
            server.createContext("/api/saveRules", WebEditorServer::handleApiSaveRules);
            server.createContext("/api/scanFiles", WebEditorServer::handleApiScanFiles);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            Oneenoughitem.LOGGER.info("Web editor server started at {}", getBaseUrl());
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to start web editor server", e);
        }
    }

    public static String getBaseUrl() {
        if (port <= 0) return "http://127.0.0.1:0/";
        return "http://127.0.0.1:" + port + "/";
    }

    private static boolean validateMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            if ("GET".equals(expectedMethod)) {
                sendText(exchange, 405, "Method Not Allowed");
            } else {
                sendJson(exchange, 405, error("Method Not Allowed"));
            }
            return false;
        }
        return true;
    }

    private static JsonObject validateReplacementFile(String datapackPath, String fileName) {
        if (isBlank(datapackPath) || isBlank(fileName)) {
            return error("datapackPath and fileName are required");
        }
        Path file = resolveReplacementFile(datapackPath, fileName);
        if (!Files.exists(file)) {
            return error("File not found: " + file);
        }
        return null;
    }

    private static JsonArray validateJsonArray(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement root = JsonParser.parseString(content);
        if (!root.isJsonArray()) {
            return null;
        }
        return root.getAsJsonArray();
    }

    private static void handleIndex(HttpExchange exchange) throws IOException {
        if (!validateMethod(exchange, "GET")) return;

        try (InputStream is = getResourceStream("index.html")) {
            if (is == null) {
                sendText(exchange, 500, "index.html not found");
                return;
            }
            sendStream(exchange, 200, "text/html; charset=utf-8", is);
        }
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        if (!validateMethod(exchange, "GET")) return;

        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(STATIC_PREFIX)) {
            sendText(exchange, 404, "Not Found");
            return;
        }
        String name = path.substring(STATIC_PREFIX.length());
        if (name.isEmpty()) {
            sendText(exchange, 404, "Not Found");
            return;
        }
        try (InputStream is = getResourceStream(name)) {
            if (is == null) {
                sendText(exchange, 404, "Not Found");
                return;
            }
            String mime = guessMime(name);
            sendStream(exchange, 200, mime, is);
        }
    }

    private static void handleApiLoad(HttpExchange exchange) throws IOException {
        try {
            if (!validateMethod(exchange, "GET")) return;

            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            String datapackPath = q.get("datapackPath");
            String fileName = q.get("fileName");

            JsonObject validationError = validateReplacementFile(datapackPath, fileName);
            if (validationError != null) {
                sendJson(exchange, 400, validationError);
                return;
            }

            Path file = resolveReplacementFile(datapackPath, fileName);
            JsonArray arr = validateJsonArray(file);
            if (arr == null) {
                sendJson(exchange, 400, error("File must contain a JSON array of replacements"));
                return;
            }

            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            out.addProperty("count", arr.size());
            out.add("replacements", arr);
            sendJson(exchange, 200, out);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("API /api/load failed", e);
            sendJson(exchange, 500, error("Internal error: " + e.getMessage()));
        }
    }

    private static void handleApiSaveRules(HttpExchange exchange) throws IOException {
        try {
            if (!validateMethod(exchange, "POST")) return;

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject req = JsonParser.parseString(body).getAsJsonObject();

            String datapackPath = optString(req, "datapackPath");
            String fileName = optString(req, "fileName");
            Integer index = optInt(req, "index");
            JsonObject rules = req.has("rules") && req.get("rules").isJsonObject()
                    ? req.getAsJsonObject("rules") : null;

            if (isBlank(datapackPath) || isBlank(fileName) || index == null || index < 0) {
                sendJson(exchange, 400, error("datapackPath, fileName and valid index are required"));
                return;
            }

            JsonObject validationError = validateReplacementFile(datapackPath, fileName);
            if (validationError != null) {
                sendJson(exchange, 404, validationError);
                return;
            }

            Path file = resolveReplacementFile(datapackPath, fileName);
            JsonArray arr = validateJsonArray(file);
            if (arr == null) {
                sendJson(exchange, 400, error("File must contain a JSON array of replacements"));
                return;
            }

            if (index >= arr.size()) {
                sendJson(exchange, 400, error("index out of range"));
                return;
            }
            JsonObject item = arr.get(index).getAsJsonObject();
            if (rules == null) {
                item.remove("rules");
            } else {
                item.add("rules", rules);
            }
            String pretty = GSON.toJson(arr);
            Files.writeString(file, pretty, StandardCharsets.UTF_8);
            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            sendJson(exchange, 200, out);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("API /api/saveRules failed", e);
            sendJson(exchange, 500, error("Internal error: " + e.getMessage()));
        }
    }

    private static void handleApiScanFiles(HttpExchange exchange) throws IOException {
        try {
            if (!validateMethod(exchange, "GET")) return;

            Map<String, String> q = parseQuery(exchange.getRequestURI().getRawQuery());
            String datapackPath = q.get("datapackPath");
            if (isBlank(datapackPath)) {
                sendJson(exchange, 400, error("datapackPath is required"));
                return;
            }

            Path replacementsDir = Paths.get(datapackPath);
            JsonArray files = new JsonArray();

            if (Files.exists(replacementsDir) && Files.isDirectory(replacementsDir)) {
                try {
                    Files.list(replacementsDir)
                            .filter(p -> p.toString().toLowerCase().endsWith(".json"))
                            .sorted()
                            .forEach(p -> {
                                String fileName = p.getFileName().toString();
                                JsonObject fileInfo = new JsonObject();
                                fileInfo.addProperty("name", fileName);
                                fileInfo.addProperty("displayName", fileName.replaceAll("\\.json$", ""));
                                try {
                                    String content = Files.readString(p, StandardCharsets.UTF_8);
                                    JsonElement root = JsonParser.parseString(content);
                                    if (root.isJsonArray()) {
                                        fileInfo.addProperty("count", root.getAsJsonArray().size());
                                    } else {
                                        fileInfo.addProperty("count", 0);
                                    }
                                } catch (Exception e) {
                                    fileInfo.addProperty("count", -1);
                                    Oneenoughitem.LOGGER.warn("Failed to parse JSON file: {}", p, e);
                                }
                                files.add(fileInfo);
                            });
                } catch (IOException e) {
                    Oneenoughitem.LOGGER.warn("Failed to scan directory: {}", replacementsDir, e);
                    sendJson(exchange, 500, error("Failed to scan directory: " + e.getMessage()));
                    return;
                }
            } else {
                Oneenoughitem.LOGGER.warn("Replacements directory not found or not a directory: {}", replacementsDir);
            }

            JsonObject out = new JsonObject();
            out.addProperty("ok", true);
            out.add("files", files);
            sendJson(exchange, 200, out);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("API /api/scanFiles failed", e);
            sendJson(exchange, 500, error("Internal error: " + e.getMessage()));
        }
    }

    private static Path resolveReplacementFile(String datapackPath, String fileName) {
        Path base = Paths.get(datapackPath);
        String normalized = fileName.endsWith(".json") ? fileName : fileName + ".json";
        return base.resolve(normalized);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static JsonObject error(String msg) {
        JsonObject obj = new JsonObject();
        obj.addProperty("ok", false);
        obj.addProperty("error", msg);
        return obj;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            if (i >= 0) {
                String k = urlDecode(part.substring(0, i));
                String v = urlDecode(part.substring(i + 1));
                map.put(k, v);
            } else {
                map.put(urlDecode(part), "");
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String guessMime(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static InputStream getResourceStream(String name) {
        String path = ASSET_BASE + name;
        ClassLoader cl = WebEditorServer.class.getClassLoader();
        return cl.getResourceAsStream(path);
    }

    private static void sendStream(HttpExchange ex, int code, String contentType, InputStream is) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", contentType);
        byte[] data = is.readAllBytes();
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private static void sendText(HttpExchange ex, int code, String text) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "text/plain; charset=utf-8");
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private static void sendJson(HttpExchange ex, int code, JsonElement json) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        byte[] data = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private static String optString(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    private static Integer optInt(JsonObject o, String k) {
        try {
            return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void copyToClipboard(String text) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "echo " + text + " | clip").start();
            } else if (os.contains("mac")) {
                Process p = new ProcessBuilder("pbcopy").start();
                p.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().close();
            } else if (os.contains("nix") || os.contains("nux")) {
                Process p = new ProcessBuilder("xclip", "-selection", "clipboard").start();
                p.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
                p.getOutputStream().close();
            } else {
                Oneenoughitem.LOGGER.info("Unknown OS, cannot auto copy. URL: {}", text);
            }
            Oneenoughitem.LOGGER.info("尝试自动复制 URL 到剪贴板: {}", text);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("自动复制失败, URL: {}", text, e);
        }
    }


    public static void openInBrowserFallback(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
            } else {
                Oneenoughitem.LOGGER.info("Unknown OS, please open manually: {}", url);
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to launch browser for {}", url, e);
        }
    }

    public static String openInBrowser() {
        startIfNeeded();
        String url = getBaseUrl();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return null;
            } else {
                copyToClipboard(url);
                openInBrowserFallback(url);
                return I18n.get("oneenoughitem.webeditor.open_browser", url);
            }
        } catch (Exception e) {
            copyToClipboard(url);
            openInBrowserFallback(url);
            Oneenoughitem.LOGGER.warn("Failed to open browser for {}", url, e);
            return I18n.get("oneenoughitem.webeditor.open_browser_manual", url);
        }
    }
}