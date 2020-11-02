package io.helidon.techempower.se;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import io.helidon.common.reactive.Single;
import io.helidon.faulttolerance.Async;
import io.helidon.techempower.se.models.Fortune;

public class JdbcRepository implements DbRepository {
    private final DataSource dataSource;

    public JdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Single<World> getWorld(int id) {
        return Async.create().invoke(() -> {
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT id, randomnumber FROM world WHERE id = ?");
                statement.setInt(1, id);
                ResultSet rs = statement.executeQuery();
                rs.next();
                World world = new World(rs.getInt(1), rs.getInt(2));
                statement.close();
                return world;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Single<World> updateWorld(World world) {
        return Async.create().invoke(() -> {
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement statement = connection.prepareStatement("UPDATE world SET randomnumber = ? WHERE id = ?");
                statement.setInt(1, world.randomNumber);
                statement.setInt(2, world.id);
                statement.execute();
                statement.close();
                return world;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Single<List<Fortune>> getFortunes() {
        return Async.create().invoke(() -> {
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT id, message FROM fortune");
                ResultSet rs = statement.executeQuery();

                List<Fortune> fortunes = new ArrayList<>();
                while (rs.next()) {
                    fortunes.add(new Fortune(rs.getInt(1), rs.getString(2)));
                }
                statement.close();
                return fortunes;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
