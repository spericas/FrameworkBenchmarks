package io.helidon.techempower.se;

import java.util.List;

import io.helidon.common.reactive.Single;
import io.helidon.techempower.se.models.Fortune;

public interface DbRepository {
    Single<World> getWorld(int id);

    Single<World> updateWorld(World world);

    Single<List<Fortune>> getFortunes();
}