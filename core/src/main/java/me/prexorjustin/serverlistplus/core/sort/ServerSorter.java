package me.prexorjustin.serverlistplus.core.sort;

import me.prexorjustin.serverlistplus.core.query.PlainText;
import me.prexorjustin.serverlistplus.core.query.ServerRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Orders servers: those pinned in the shown tab first, then by the chosen {@link SortMode}.
 * Sorting is stable, so ties keep the manual order. Unknown values (not pinged yet,
 * unreachable, never joined) always go last.
 */
public final class ServerSorter<T> {
    private final Function<T, ServerRef> ref;
    private final ToLongFunction<T> lastPlayed;
    private final Predicate<T> pinned;

    /**
     * @param ref        the searchable view of an item
     * @param lastPlayed epoch millis of the last join, 0 if never
     * @param pinned     whether an item is pinned in the tab being sorted
     */
    public ServerSorter(Function<T, ServerRef> ref, ToLongFunction<T> lastPlayed, Predicate<T> pinned) {
        this.ref = ref;
        this.lastPlayed = lastPlayed;
        this.pinned = pinned;
    }

    public List<T> sorted(List<T> items, SortMode mode) {
        List<T> result = new ArrayList<>(items);
        result.sort(comparator(mode));
        return result;
    }

    /** True if {@link #sorted} would return the items in their current order. */
    public boolean isSorted(List<T> items, SortMode mode) {
        Comparator<T> comparator = comparator(mode);
        for (int i = 1; i < items.size(); i++) {
            if (comparator.compare(items.get(i - 1), items.get(i)) > 0) return false;
        }
        return true;
    }

    public boolean isPinned(T item) {
        return pinned.test(item);
    }

    private Comparator<T> comparator(SortMode mode) {
        Comparator<T> pinnedFirst = Comparator.comparing(item -> !isPinned(item));
        return switch (mode) {
            case MANUAL -> pinnedFirst;
            case NAME -> pinnedFirst.thenComparing(item -> PlainText.of(ref.apply(item).name()));
            case PING -> pinnedFirst.thenComparing(unknownLast(item -> ref.apply(item).ping()));
            case PLAYERS -> pinnedFirst.thenComparing(unknownLast(item -> -(long) ref.apply(item).onlinePlayers(),
                item -> ref.apply(item).onlinePlayers() < 0));
            case LAST_PLAYED -> pinnedFirst.thenComparing(unknownLast(item -> -lastPlayed(item),
                item -> lastPlayed(item) <= 0));
        };
    }

    private long lastPlayed(T item) {
        return lastPlayed.applyAsLong(item);
    }

    /** Ascending by key, negative keys last. */
    private static <T> Comparator<T> unknownLast(Function<T, Long> key) {
        return unknownLast(key, item -> key.apply(item) < 0);
    }

    private static <T> Comparator<T> unknownLast(Function<T, Long> key, Function<T, Boolean> unknown) {
        return Comparator.<T, Boolean>comparing(unknown).thenComparing(key);
    }
}
