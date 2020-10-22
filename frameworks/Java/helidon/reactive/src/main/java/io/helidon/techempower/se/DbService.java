package io.helidon.techempower.se;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbColumn;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * Service handling database tests using Helidon Db Client.
 */
class DbService implements Service {

    private final DbClient dbClient;
    private final JsonBuilderFactory jsonBuilderFactory;

    DbService(DbClient dbClient) {
        this.dbClient = dbClient;
        this.jsonBuilderFactory = Json.createBuilderFactory(Map.of());
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::db);
        rules.get("/queries", this::queries);
        rules.get("/updates", this::updates);
    }

    private void db(ServerRequest req, ServerResponse res) {
        nextWorld().thenAccept(res::send)
                .exceptionally(res::send);
    }

    private void queries(ServerRequest req, ServerResponse res) {
        int count = parseQueryCount(req.queryParams().first("queries").orElse("1"));
        JsonArrayBuilder builder = jsonBuilderFactory.createArrayBuilder();

        /*
        try {
            for (int i = 0; i < count; i++) {
                Single<JsonObject> single = nextWorld();
                JsonObject obj = single.get();
                builder.add(obj);
            }
            res.send(builder.build());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
         */

        /*
        Single<JsonObject> last = nextWorld();

        // one less, as we already selected one
        for (int i = 1; i < count; i++) {
            last = last.flatMapSingle(it -> {
                builder.add(it);
                return nextWorld();
            });
        }

        last.thenAccept(it -> {
            builder.add(it);
            res.send(builder.build());
        }).exceptionally(res::send);
        */

        Single<DbRow> last = nextWorldDbRow();

        // one less, as we already selected one
        for (int i = 1; i < count; i++) {
            last = last.flatMapSingle(it -> {
                JsonObjectBuilder obj = jsonBuilderFactory.createObjectBuilder();
                obj.add("id", it.column("id").as(Integer.class));
                obj.add("randomNumber", it.column("randomnumber").as(Integer.class));
                builder.add(obj.build());
                return nextWorldDbRow();
            });
        }

        last.thenAccept(it -> {
            JsonObjectBuilder obj = jsonBuilderFactory.createObjectBuilder();
            obj.add("id", it.column("id").as(Integer.class));
            obj.add("randomNumber", it.column("randomnumber").as(Integer.class));
            builder.add(obj.build());
            res.send(builder.build());
        }).exceptionally(res::send);
    }

    private void updates(ServerRequest req, ServerResponse res) {
        int count = parseQueryCount(req.queryParams().first("updates").orElse("1"));
        JsonArrayBuilder builder = jsonBuilderFactory.createArrayBuilder();

        // the following section is convoluted, as we need to execute a big number of queries and updates, but due to
        // unknown limit, we execute them sequentially rather than in parallel
        Single<JsonObject> last = updateWorld();

        // one less, as we already selected one
        for (int i = 1; i < count; i++) {
            last = last.flatMapSingle(it -> {
                builder.add(it);
                return updateWorld();
            });
        }

        last.thenAccept(it -> {
            builder.add(it);
            res.send(builder.build());
        }).exceptionally(res::send);
    }

    private Single<JsonObject> nextWorld() {
        return dbClient.execute(it -> it.namedGet("get-world", randomWorldNumber()))
                .map(Optional::get)
                .map(it -> it.as(JsonObject.class));
    }

    private Single<DbRow> nextWorldDbRow() {
        return dbClient.execute(it -> it.namedGet("get-world", randomWorldNumber()))
                .map(Optional::get);
    }

    private Single<JsonObject> updateWorld() {
        return nextWorld().flatMapSingle(it -> {
            int id = it.getInt("id");
            int random = randomWorldNumber();
            return dbClient.execute(exec -> exec.namedUpdate("update-world", id, random))
                    .map(dbResult -> jsonBuilderFactory.createObjectBuilder()
                            .add("id", id)
                            .add("randomNumber", random)
                            .build());

        });
    }

    private int randomWorldNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(10000);
    }

    private int parseQueryCount(String param) {
        try {
            int count = Integer.parseInt(param);
            return Math.min(500, Math.max(1, count));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
