/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.benchmark.nima;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import io.helidon.benchmark.nima.models.DbRepository;
import io.helidon.benchmark.nima.models.HikariJdbcRepository;
import io.helidon.benchmark.nima.models.PgClientRepository;
import io.helidon.benchmark.nima.services.DbService;
import io.helidon.benchmark.nima.services.FortuneHandler;
import io.helidon.common.LogConfig;
import io.helidon.common.http.Http;
import io.helidon.common.http.Http.Header;
import io.helidon.common.http.Http.HeaderValue;
import io.helidon.common.http.Http.HeaderValues;
import io.helidon.config.Config;
import io.helidon.config.ConfigException;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.Handler;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Main class of the benchmark.
 * Opens server on localhost:8080 and exposes {@code /plaintext} and {@code /json} endpoints adhering to the
 * rules of TechEmpower benchmarking.
 */
public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static final Http.HeaderValue CONTENT_TYPE_HTML =
            Http.HeaderValue.createCached(Http.Header.CONTENT_TYPE, "text/html; charset=UTF-8");
    public static final Http.HeaderValue SERVER = Http.HeaderValue.createCached(Http.Header.SERVER, "Nima");

    private Main() {
    }

    /**
     * Start the server.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        // logging and config
        LogConfig.configureRuntime();

        WebServer.builder()
                .routing(Main::routing)
                .start();
    }

    // exposed for tests
    static void routing(HttpRules rules) {
        Config config = Config.create();

        DbRepository repository;
        String name = config.get("db-repository").asString().orElse("pgclient");
        LOGGER.info("Using '" + name + "' as DB repository");
        if (name.equalsIgnoreCase("hikari")) {
            repository = new HikariJdbcRepository(config);
        } else if (name.equalsIgnoreCase("pgclient")) {
            repository = new PgClientRepository(config);
        } else {
            throw new ConfigException("Allowed values for 'db-repository' are 'hikari' and 'pgclient'");
        }

        rules.get("/plaintext", new PlaintextHandler())
                .get("/json", new JsonHandler())
                .get("/10k", new JsonKHandler(10))
                .get("/fortunes", new FortuneHandler(repository))
                .register("/", new DbService(repository));
    }

    private static byte[] serializeMsg(Message obj) {
        return obj.toString().getBytes(StandardCharsets.US_ASCII);
    }

    static class PlaintextHandler implements Handler {
        static final HeaderValue CONTENT_TYPE = HeaderValue.createCached(Header.CONTENT_TYPE,
                "text/plain; charset=UTF-8");
        static final HeaderValue CONTENT_LENGTH = HeaderValue.createCached(Header.CONTENT_LENGTH, "13");

        private static final byte[] RESPONSE_BYTES = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        @Override
        public void handle(ServerRequest req, ServerResponse res) {
            res.header(CONTENT_LENGTH);
            res.header(CONTENT_TYPE);
            res.header(Main.SERVER);
            res.send(RESPONSE_BYTES);
        }
    }

    static class JsonHandler implements Handler {
        private static final byte[] WHOLE_MESSAGE =
                "{\"message\":\"Hello, World!\"}".getBytes(StandardCharsets.US_ASCII);
        private static final int JSON_LENGTH = WHOLE_MESSAGE.length;
        static final HeaderValue CONTENT_LENGTH = HeaderValue.createCached(Header.CONTENT_LENGTH,
                String.valueOf(JSON_LENGTH));

        @Override
        public void handle(ServerRequest req, ServerResponse res) {
            res.header(CONTENT_LENGTH);
            res.header(HeaderValues.CONTENT_TYPE_JSON);
            res.header(Main.SERVER);
            res.send(WHOLE_MESSAGE); // serializeMsg(new Message(MESSAGE)));
        }
    }

    static class JsonKHandler implements Handler {
        private final HeaderValue contentLength;
        private final String message;

        JsonKHandler(int kilobytes) {
            this.message = "a".repeat(1024 * kilobytes);
            int length = serializeMsg(new Message(message)).length;
            this.contentLength = HeaderValue.createCached(Header.CONTENT_LENGTH,
                    String.valueOf(length));
        }

        @Override
        public void handle(ServerRequest req, ServerResponse res) {
            res.header(contentLength);
            res.header(HeaderValues.CONTENT_TYPE_JSON);
            res.header(Main.SERVER);
            res.send(serializeMsg(newMsg()));
        }

        private Message newMsg() {
            return new Message(message);
        }
    }

    /**
     * Message to be serialized as JSON.
     */
    public static final class Message {

        private final JsonObject delegate;

        /**
         * Construct a new message.
         *
         * @param message message string
         */
        public Message(String message) {
            this.delegate = Json.createObjectBuilder()
                    .add("message", message)
                    .build();
        }

        /**
         * Get message string.
         *
         * @return message string
         */
        public String getMessage() {
            return delegate.getString("message");
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
