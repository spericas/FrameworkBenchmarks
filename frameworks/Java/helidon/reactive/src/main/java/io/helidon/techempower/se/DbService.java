package io.helidon.techempower.se;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
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
    private final DbRepository dbRepository;
    private final boolean useDbClient;

    /**
     * Create a {@code DbService}. If {@code dbRepository} is null then {@code dbClient}
     * will be used.
     *
     * @param dbClient Instance of {@code DbClient}, may be null
     * @param dbRepository Instance of {@code dbRepository}, may be null
     */
    DbService(DbClient dbClient, DbRepository dbRepository) {
        this.dbClient = dbClient;
        this.dbRepository = dbRepository;
        this.jsonBuilderFactory = Json.createBuilderFactory(Map.of());
        this.useDbClient = (dbRepository == null);
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::db);
        rules.get("/queries", this::queries);
        rules.get("/updates", this::updates);
    }

    private void db(ServerRequest req, ServerResponse res) {
        queryDb().thenAccept(res::send).exceptionally(res::send);
    }


    private void queries(ServerRequest req, ServerResponse res) {
        int count = parseQueryCount(req.queryParams().first("queries").orElse("1"));
        JsonArrayBuilder builder = jsonBuilderFactory.createArrayBuilder();

        Single<JsonObject> last = queryDb();

        // one less, as we already selected one
        for (int i = 1; i < count; i++) {
            last = last.flatMapSingle(it -> {
                builder.add(it);
                return queryDb();
            });
        }

        last.thenAccept(it -> {
            builder.add(it);
            res.send(builder.build());
        }).exceptionally(res::send);
    }

    private void updates(ServerRequest req, ServerResponse res) {
        int count = parseQueryCount(req.queryParams().first("updates").orElse("1"));
        JsonArrayBuilder builder = jsonBuilderFactory.createArrayBuilder();

        Single<JsonObject> last = updateDb();

        // one less, as we already selected one
        for (int i = 1; i < count; i++) {
            last = last.flatMapSingle(it -> {
                builder.add(it);
                return updateDb();
            });
        }

        last.thenAccept(it -> {
            builder.add(it);
            res.send(builder.build());
        }).exceptionally(res::send);
    }

    private Single<JsonObject> queryDb() {
        return useDbClient ? queryDbClient() : queryDbRepository();
    }

    private Single<JsonObject> queryDbClient() {
        return dbClient.execute(it -> it.namedGet("get-world", randomWorldNumber()))
                .map(Optional::get)
                .map(it -> it.as(JsonObject.class));
    }

    private Single<JsonObject> queryDbRepository() {
        return dbRepository.getWorld(randomWorldNumber()).map(World::toJson);
    }

    private Single<JsonObject> updateDb() {
        return useDbClient ? updateDbClient() : updateDbRepository();
    }

    private Single<JsonObject> updateDbClient() {
        return queryDbClient().flatMapSingle(it -> {
            int id = it.getInt("id");
            int random = randomWorldNumber();
            return dbClient.execute(exec -> exec.namedUpdate("update-world", id, random))
                    .map(dbResult -> jsonBuilderFactory.createObjectBuilder()
                            .add("id", id)
                            .add("randomNumber", random)
                            .build());

        });
    }

    private Single<JsonObject> updateDbRepository() {
        return queryDbRepository().flatMapSingle(it -> {
            int id = it.getInt("id");
            int random = randomWorldNumber();
            return dbRepository.updateWorld(new World(id, random))
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
