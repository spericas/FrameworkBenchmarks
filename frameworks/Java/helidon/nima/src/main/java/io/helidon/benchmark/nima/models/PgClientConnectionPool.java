
package io.helidon.benchmark.nima.models;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

class PgClientConnectionPool implements AutoCloseable {

    private final Vertx vertx;
    private final PgConnectOptions options;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, PgClientConnection> connectionMap = new HashMap<>();

    public PgClientConnectionPool(Vertx vertx, PgConnectOptions options) {
        this.vertx = vertx;
        this.options = options;
    }

    public PgClientConnection clientConnection() {
        String carrierThread = carrierThread();
        PgClientConnection connection = connectionMap.get(carrierThread);
        if (connection == null) {
            try {
                lock.lock();
                connection = connectionMap.get(carrierThread);
                if (connection == null) {
                    connection = newConnection();
                    connectionMap.put(carrierThread, connection);
                }
            } finally {
                lock.unlock();
            }
        }
        return connection;
    }

    @Override
    public void close() {
        try {
            for (PgClientConnection connection : connectionMap.values()) {
                connection.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PgClientConnection newConnection() {
        try {
            long start = System.currentTimeMillis();
            PgConnection conn = PgConnection.connect(vertx, options)
                    .toCompletionStage().toCompletableFuture().get();
            System.out.println("### connect time " + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            PgClientConnection clientConn = new PgClientConnection(conn);
            clientConn.prepare();
            System.out.println("### prepare time " + (System.currentTimeMillis() - start));
            return clientConn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String carrierThread() {
        String threadName = Thread.currentThread().toString();
        return threadName.substring(threadName.indexOf('@') + 1);
    }

    public static class PgClientConnection implements AutoCloseable {
        static final int UPDATE_QUERIES = 500;
        private static String SELECT_WORLD = "SELECT id, randomnumber from WORLD where id=$1";
        private static String SELECT_FORTUNE = "SELECT * from FORTUNE";

        private CompletableFuture<PreparedStatement> worldQuery;
        private CompletableFuture<PreparedStatement> fortuneQuery;
        private CompletableFuture<PreparedStatement>[] updateQuery;

        private final PgConnection conn;

        PgClientConnection(PgConnection conn) {
            this.conn = conn;
        }

        public PgConnection pgConnection() {
            return conn;
        }

        @Override
        public void close() {
            conn.close();
        }

        public PreparedQuery<RowSet<Row>> worldQuery() throws ExecutionException, InterruptedException {
            return worldQuery.get().query();
        }

        public PreparedQuery<RowSet<Row>> fortuneQuery() throws ExecutionException, InterruptedException {
            return fortuneQuery.get().query();
        }

        public PreparedQuery<RowSet<Row>> updateQuery(int queryCount) throws ExecutionException, InterruptedException {
            return updateQuery[queryCount - 1].get().query();
        }

        @SuppressWarnings("unchecked")
        void prepare() {
            try {
                worldQuery = conn.prepare(SELECT_WORLD).toCompletionStage().toCompletableFuture();
                worldQuery.get();
                fortuneQuery = conn.prepare(SELECT_FORTUNE).toCompletionStage().toCompletableFuture();
                fortuneQuery.get();
                updateQuery = (CompletableFuture<PreparedStatement>[]) new CompletableFuture<?>[UPDATE_QUERIES];
                for (int i = 0; i < UPDATE_QUERIES; i++) {
                    updateQuery[i] = conn.prepare(singleUpdate(i + 1))
                                         .toCompletionStage().toCompletableFuture();
                    updateQuery[i].get();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
    }
}
