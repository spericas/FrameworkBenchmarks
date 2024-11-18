package io.helidon.benchmark.nima.models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import static io.helidon.benchmark.nima.models.DbRepository.randomWorldNumber;

public class PgClientRepository implements DbRepository {
    private static final Logger LOGGER = Logger.getLogger(PgClientRepository.class.getName());
    private static final int UPDATE_QUERIES = 500;

    private final PreparedQuery<RowSet<Row>> getFortuneQuery;
    private final PreparedQuery<RowSet<Row>> getWorldQuery;
    private final PreparedQuery<RowSet<Row>>[] updateWorldSingleQuery;

    @SuppressWarnings("unchecked")
    public PgClientRepository(Config config) {
        VertxOptions vertxOptions = new VertxOptions()
                .setPreferNativeTransport(true)
                .setBlockedThreadCheckInterval(100000)
                .setUseDaemonThread(true);
        Vertx vertx = Vertx.vertx(vertxOptions);

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(config.get("port").asInt().orElse(5432))
                .setCachePreparedStatements(config.get("cache-prepared-statements").asBoolean().orElse(true))
                .setHost(config.get("host").asString().orElse("tfb-database"))
                .setDatabase(config.get("db").asString().orElse("hello_world"))
                .setUser(config.get("username").asString().orElse("benchmarkdbuser"))
                .setPassword(config.get("password").asString().orElse("benchmarkdbpass"))
                .setTcpNoDelay(true)
                .setTcpQuickAck(true)
                .setTcpKeepAlive(true)
                .setPipeliningLimit(100000);

        int sqlPoolSize = config.get("sql-pool-size").asInt().orElse(defaultPoolSize());
        int eventLoopSize = config.get("event-loop-size").asInt().orElse(defaultPoolSize());
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(sqlPoolSize)
                .setEventLoopSize(eventLoopSize)
                .setPoolCleanerPeriod(-1);
        LOGGER.info("sql-pool-size is " + sqlPoolSize);
        LOGGER.info("event-loop-size is " + eventLoopSize);

        SqlClient queryClient = PgBuilder
                .client()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(vertx)
                .build();
        getWorldQuery = queryClient.preparedQuery("SELECT id, randomnumber FROM world WHERE id = $1");
        getFortuneQuery = queryClient.preparedQuery("SELECT id, message FROM fortune");

        SqlClient updateClient = PgBuilder
                .client()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .build();
        updateWorldSingleQuery = new PreparedQuery[UPDATE_QUERIES];
        for (int i = 0; i < UPDATE_QUERIES; i++) {
            updateWorldSingleQuery[i] = updateClient.preparedQuery(singleUpdate(i + 1));
        }
    }

    @Override
    public World getWorld(int id) {
        try {
            return getWorldQuery.execute(Tuple.of(id))
                    .map(rows -> {
                        Row r = rows.iterator().next();
                        return new World(r.getInteger(0), r.getInteger(1));
                    }).toCompletionStage().toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<World> getWorlds(int count) {
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(getWorldQuery.execute(Tuple.of(randomWorldNumber()))
                        .map(rows -> {
                            Row r = rows.iterator().next();
                            return new World(r.getInteger(0), r.getInteger(1));
                        }));
            }
            return Future.all(futures).toCompletionStage().toCompletableFuture().get().list();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<World> updateWorlds(int count) {
        List<World> worlds = getWorlds(count);
        try {
            return updateWorlds(worlds, count);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Fortune> getFortunes() {
        try {
            return getFortuneQuery.execute()
                    .map(rows -> {
                        List<Fortune> fortunes = new ArrayList<>(rows.size() + 1);
                        for (Row r : rows) {
                            fortunes.add(new Fortune(r.getInteger(0), r.getString(1)));
                        }
                        return fortunes;
                    }).toCompletionStage().toCompletableFuture().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<World> updateWorlds(List<World> worlds, int count)
            throws ExecutionException, InterruptedException {
        int size = worlds.size();
        List<Integer> updateParams = new ArrayList<>(size * 2);
        for (World world : worlds) {
            updateParams.add(world.id);
            world.randomNumber = randomWorldNumber();
            updateParams.add(world.randomNumber);
        }
        return updateWorldSingleQuery[count - 1].execute(Tuple.wrap(updateParams))
                .toCompletionStage()
                .thenApply(rows -> worlds)
                .toCompletableFuture()
                .get();
    }

    private static String singleUpdate(int count) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE WORLD SET RANDOMNUMBER = CASE ID");
        for (int i = 0; i < count; i++) {
            int k = i * 2 + 1;
            sql.append(" WHEN $").append(k).append(" THEN $").append(k + 1);
        }
        sql.append(" ELSE RANDOMNUMBER");
        sql.append(" END WHERE ID IN ($1");
        for (int i = 1; i < count; i++) {
            int k = i * 2 + 1;
            sql.append(",$").append(k);
        }
        sql.append(")");
        return sql.toString();
    }

    private static int defaultPoolSize() {
        int processors = Runtime.getRuntime().availableProcessors();
        return Math.max(processors, 7 * processors - 26);
    }
}
