package com.snl.compiler.controller;

import com.snl.compiler.service.CompileRequest;
import com.snl.compiler.service.CompileResponse;
import com.snl.compiler.service.CompilerService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class CompilerHttpServer {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_STATIC_ROOT = "../SnlCompiler-Frontend/dist";

    private final CompilerController controller;
    private final Path staticRoot;

    public CompilerHttpServer(CompilerController controller) {
        this(controller, resolveStaticRoot(DEFAULT_STATIC_ROOT));
    }

    public CompilerHttpServer(CompilerController controller, Path staticRoot) {
        this.controller = controller;
        this.staticRoot = staticRoot;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Path staticRoot = args.length > 1 ? resolveStaticRoot(args[1]) : resolveStaticRoot(DEFAULT_STATIC_ROOT);
        CompilerHttpServer server = new CompilerHttpServer(new CompilerController(new CompilerService()), staticRoot);
        server.start(port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/compile/lexical", exchange -> handle(exchange, "lexical"));
        server.createContext("/api/compile/grammar", exchange -> handle(exchange, "grammar"));
        server.createContext("/api/compile/recursive", exchange -> handle(exchange, "recursive"));
        server.createContext("/api/compile/semantic", exchange -> handle(exchange, "semantic"));
        server.createContext("/", this::serveStatic);
        server.setExecutor(null);
        server.start();
        System.out.println("SNL compiler listening on http://localhost:" + port);
        System.out.println("Static root: " + staticRoot);
    }

    private void handle(HttpExchange exchange, String stage) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 204, "");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 405, "{\"error\":\"Only POST is supported\"}");
            return;
        }

        CompileRequest request = parseRequest(readBody(exchange));
        CompileResponse response;
        if ("lexical".equals(stage)) {
            response = controller.lexical(request);
        } else if ("grammar".equals(stage)) {
            response = controller.grammar(request);
        } else if ("recursive".equals(stage)) {
            response = controller.recursive(request);
        } else {
            response = controller.semantic(request);
        }
        write(exchange, 200, JsonSupport.toJson(response));
    }

    private CompileRequest parseRequest(String body) {
        CompileRequest request = new CompileRequest();
        request.source = JsonSupport.extractString(body, "source");
        request.tokenView = JsonSupport.extractString(body, "tokenView");
        return request;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        byte[] buffer = new byte[4096];
        StringBuilder body = new StringBuilder();
        int count;
        while ((count = input.read(buffer)) != -1) {
            body.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    private void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream output = exchange.getResponseBody();
        output.write(bytes);
        output.close();
    }

    private void serveStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            write(exchange, 405, "{\"error\":\"Only GET and HEAD are supported\"}");
            return;
        }

        Path file = resolveStaticFile(exchange.getRequestURI().getPath());
        if (!Files.exists(file) || Files.isDirectory(file)) {
            file = staticRoot.resolve("index.html").normalize();
        }
        if (!file.startsWith(staticRoot) || !Files.exists(file)) {
            write(exchange, 404, "{\"error\":\"Static asset not found\"}");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().add("Content-Type", contentType(file));
        exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(exchange.getRequestMethod()) ? -1 : bytes.length);
        if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            OutputStream output = exchange.getResponseBody();
            output.write(bytes);
            output.close();
        } else {
            exchange.close();
        }
    }

    private Path resolveStaticFile(String requestPath) {
        String relativePath = requestPath == null || "/".equals(requestPath)
                ? "index.html"
                : requestPath.substring(1);
        return staticRoot.resolve(relativePath).normalize();
    }

    private static Path resolveStaticRoot(String staticRoot) {
        Path configured = Paths.get(staticRoot).toAbsolutePath().normalize();
        if (Files.exists(configured)) {
            return configured;
        }
        return Paths.get("SnlCompiler-Frontend/dist").toAbsolutePath().normalize();
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (name.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }
}
