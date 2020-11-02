/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.techempower.se;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

/**
 * TechEmpower benchmark test.
 * Implements all tests except for caching.
 */
public final class Main {

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    public static void main(String[] args) {
        LogConfig.initClass();

        Config config = Config.create();

        WebServer.builder()
                .config(config.get("server"))
                .routing(createRouting(config))
                .addMediaSupport(JsonpSupport.create())
                .build()
                .start()
                .await(10, TimeUnit.SECONDS);
    }

    /**
     * Creates new {@link Routing}.
     *
     * @return the new instance
     */
    private static Routing createRouting(Config config) {
        // Initialize dbRepository or dbClient based on config
        DbClient dbClient = null;
        DbRepository dbRepository = null;
        Config dataSourceConfig = config.get("dataSource");
        if (dataSourceConfig.exists()) {
            dbRepository = new JdbcRepository(getDataSource(dataSourceConfig));
            System.out.println("Using JdbcRepository ...");
        } else {
            Config dbClientConfig = config.get("dbclient");
            Objects.requireNonNull(dbClientConfig);
            dbClient = DbClient.create(dbClientConfig);
            System.out.println("Using DbClient ...");
        }

        return Routing.builder()
                .any((req, res) -> {
                    // required header for each response
                    res.headers().add("Server", "Helidon");
                    req.next();
                })
                .get("/json", new JsonHandler())
                .get("/plaintext", new PlainTextHandler())
                .register("/db", new DbService(dbClient, dbRepository))
                .get("/fortunes", new FortuneHandler(dbClient, getTemplate()))
                .build();
    }

    private static Mustache getTemplate() {
        MustacheFactory mf = new DefaultMustacheFactory();
        return mf.compile("fortunes.mustache");
    }

    private static DataSource getDataSource(Config config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.get("jdbcUrl").asString().get());
        hikariConfig.setUsername(config.get("username").asString().get());
        hikariConfig.setPassword(config.get("password").asString().get());
        hikariConfig.setMaximumPoolSize(config.get("maximumPoolSize").asInt().get());
        return new HikariDataSource(hikariConfig);
    }
}
