package me.prexorjustin.serverlistplus.core.view;

import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.query.Query;
import me.prexorjustin.serverlistplus.core.query.ServerRef;

import java.util.Optional;

/**
 * Which servers belong to a tab.
 */
public final class Tabs {
    private Tabs() {
    }

    /**
     * @param tab  {@link ViewSettings#TAB_ALL} or a folder id
     * @param meta extra data of the server, null if none exists
     */
    public static boolean contains(Store store, String tab, ServerRef server, ServerMeta meta) {
        if (ViewSettings.TAB_ALL.equals(tab)) return meta == null || !meta.hiddenInAll();
        Optional<Folder> folder = store.folder(tab);
        if (folder.isEmpty()) return true;
        if (folder.get().isSmart()) {
            return Query.parse(folder.get().query()).matches(server, meta, id -> store.folder(id).map(Folder::name).orElse(null));
        }
        return meta != null && meta.isInFolder(tab);
    }

    /** Whether servers can be dropped onto a tab to move them there (smart folders fill themselves). */
    public static boolean acceptsDrop(Store store, String tab) {
        if (ViewSettings.TAB_ALL.equals(tab)) return true;
        return store.folder(tab).map(folder -> !folder.isSmart()).orElse(false);
    }
}
