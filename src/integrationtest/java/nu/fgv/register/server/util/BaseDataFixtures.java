package nu.fgv.register.server.util;

import nu.fgv.register.server.sync.FeedChange;
import nu.fgv.register.server.sync.SyncService;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Base data for integration tests, made as Spexipedia's change feed would make it: only the sync writes base data.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class BaseDataFixtures {

    private final SyncService sync;
    private long sequence;

    public BaseDataFixtures(final SyncService sync) {
        this.sync = sync;
    }

    private String next() {
        return String.valueOf(++sequence);
    }

    public UUID spexCategory(final String name, final String firstYear) {
        final UUID id = UUID.randomUUID();

        sync.applyAll(List.of(new FeedChange(next(), "SPEX_CATEGORY", id, "UPSERT", null,
                new FeedChange.SpexCategory(id, name, firstYear, null, false), null, null, null, null)));
        return id;
    }

    public UUID spex(final String year, final String title, final @Nullable UUID categoryId) {
        final UUID id = UUID.randomUUID();

        sync.applyAll(List.of(new FeedChange(next(), "SPEX", id, "UPSERT", null, null,
                new FeedChange.Spex(id, year, title, categoryId, null, null, false), null, null, null)));
        return id;
    }

    public UUID revival(final UUID parentId, final String year, final String title) {
        final UUID id = UUID.randomUUID();

        sync.applyAll(List.of(new FeedChange(next(), "SPEX", id, "UPSERT", null, null,
                new FeedChange.Spex(id, year, title, null, parentId, null, false), null, null, null)));
        return id;
    }

    public UUID taskCategory(final String name, final boolean actorPresent) {
        final UUID id = UUID.randomUUID();

        sync.applyAll(List.of(new FeedChange(next(), "TASK_CATEGORY", id, "UPSERT", null, null, null,
                new FeedChange.TaskCategory(id, name, actorPresent, false), null, null)));
        return id;
    }

    public UUID task(final String name, final @Nullable UUID categoryId) {
        final UUID id = UUID.randomUUID();

        sync.applyAll(List.of(new FeedChange(next(), "TASK", id, "UPSERT", null, null, null, null,
                new FeedChange.Task(id, name, categoryId, false, false), null)));
        return id;
    }

    public UUID character(final UUID spexId, final String name, final int position) {
        final UUID id = UUID.randomUUID();

        sync.applyAll(List.of(new FeedChange(next(), "CHARACTER", id, "UPSERT", null, null, null, null, null,
                new FeedChange.Character(id, spexId, name, position, false))));
        return id;
    }
}
