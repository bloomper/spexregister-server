package nu.fgv.register.server.sync;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Spexipedia's change feed as it answers {@code baseDataChanges}: each change with the record as it was right after it.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public record FeedChange(String sequence, String kind, UUID id, String change, @Nullable UUID mergedIntoId,
                         @Nullable SpexCategory spexCategory, @Nullable Spex spex, @Nullable TaskCategory taskCategory,
                         @Nullable Task task, @Nullable Character character) {

    public record SpexCategory(UUID id, String name, String firstYear, @Nullable String logoUrl, boolean retired) {
    }

    public record Spex(UUID id, String year, String title, @Nullable UUID categoryId, @Nullable UUID parentId,
                       @Nullable String posterUrl, boolean retired) {
    }

    public record TaskCategory(UUID id, String name, boolean actorPresent, boolean board, boolean retired) {
    }

    public record Task(UUID id, String name, @Nullable UUID categoryId, boolean retired) {
    }

    public record Character(UUID id, UUID spexId, String name, int position, boolean retired) {
    }

    public record Page(List<FeedChange> changes, @Nullable String cursor, boolean hasMore) {
    }
}
