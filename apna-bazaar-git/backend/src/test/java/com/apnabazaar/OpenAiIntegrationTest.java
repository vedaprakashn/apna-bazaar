package com.apnabazaar;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiIntegrationTest {
    @Test
    void sendsAuthenticatedOpenAiRequestAndReadsResponse() throws Exception {
        var request = new AtomicReference<String>();
        var authorization = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = """
                {"id":"test","object":"chat.completion","created":1,"model":"gpt-4o-mini",
                 "choices":[{"index":0,"message":{"role":"assistant","content":"Local test response"},"finish_reason":"stop"}],
                 "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var api = OpenAiApi.builder().apiKey("local-test-only")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
            var model = OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").build()).build();
            assertEquals("Local test response", model.call("Find idli"));
            assertEquals("Bearer local-test-only", authorization.get());
            assertTrue(request.get().contains("gpt-4o-mini"));
            assertTrue(request.get().contains("Find idli"));
        } finally {
            server.stop(0);
        }
    }
}
