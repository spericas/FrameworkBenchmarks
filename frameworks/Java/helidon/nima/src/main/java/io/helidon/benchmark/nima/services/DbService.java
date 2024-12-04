
package io.helidon.benchmark.nima.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.jsoniter.output.JsonStream;
import io.helidon.benchmark.nima.JsonStreamSupplier;
import io.helidon.benchmark.nima.models.DbRepository;
import io.helidon.benchmark.nima.models.World;
import io.helidon.common.mapper.OptionalValue;
import io.helidon.common.parameters.Parameters;
import io.helidon.http.HeaderValues;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import static io.helidon.benchmark.nima.JsonSerializer.serialize;
import static io.helidon.benchmark.nima.Main.SERVER;
import static io.helidon.benchmark.nima.models.DbRepository.randomWorldNumber;

public class DbService implements HttpService {

    private final DbRepository repository;

    public DbService(DbRepository repository) {
        this.repository = repository;
    }

    @Override
    public void routing(HttpRules httpRules) {
        httpRules.get("/db", this::db);
        httpRules.get("/queries", this::queries);
        httpRules.get("/updates", this::updates);
    }

    private void db(ServerRequest req, ServerResponse res) {
        res.header(SERVER);
        res.header(HeaderValues.CONTENT_TYPE_JSON);
        res.send(serialize(repository.getWorld(randomWorldNumber())));
    }

    private void queries(ServerRequest req, ServerResponse res) {
        res.header(SERVER);
        res.header(HeaderValues.CONTENT_TYPE_JSON);
        int count = parseQueryCount(req.query());

        try (OutputStream os = res.outputStream()) {
            JsonStream jsonStream = JsonStreamSupplier.borrowJsonStream(os);
            serialize(repository.getWorlds(count), jsonStream);
            jsonStream.flush();
            JsonStreamSupplier.returnJsonStream(jsonStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updates(ServerRequest req, ServerResponse res) {
        res.header(SERVER);
        res.header(HeaderValues.CONTENT_TYPE_JSON);
        int count = parseQueryCount(req.query());
        List<World> worlds = repository.updateWorlds(count);
        res.send(serialize(worlds));
    }

    private int parseQueryCount(Parameters parameters) {
        OptionalValue<String> value = parameters.first("queries");
        if (value.isEmpty()) {
            return 1;
        }
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(value.get(), 10);
        } catch (NumberFormatException e) {
            return 1;
        }
        return Math.min(500, Math.max(1, parsedValue));
    }
}