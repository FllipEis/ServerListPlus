package me.prexorjustin.serverlistplus.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extra data ServerListPlus keeps for one server, stored separately from the vanilla server list.
 */
public final class ServerMeta {
    private String notes = "";
    /** Show the notes in a toast when joining the server. */
    private boolean notesOnJoin;
    private List<String> tags = new ArrayList<>();
    /** Folders the server is in, in the order it was added to them. */
    private List<String> folderIds = new ArrayList<>();
    /** Tabs in which this server stays on top: {@link ViewSettings#TAB_ALL} or folder ids. */
    private List<String> pinnedTabs = new ArrayList<>();
    /** Left out of the "All" tab; still shown in its folder and when searching {@code is:hidden}. */
    private boolean hiddenInAll;
    /** Epoch millis since which no server in the list uses this entry, null while in use. */
    private Long orphanedSince;

    public String notes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public boolean notesOnJoin() {
        return notesOnJoin;
    }

    public void setNotesOnJoin(boolean notesOnJoin) {
        this.notesOnJoin = notesOnJoin;
    }

    /** Whether there are notes to show when joining. */
    public boolean remindsOnJoin() {
        return notesOnJoin && !notes.isBlank();
    }

    public List<String> tags() {
        return List.copyOf(tags);
    }

    /** Stores trimmed, non-blank tags, dropping case-insensitive duplicates. */
    public void setTags(List<String> tags) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (tags != null) {
            for (String tag : tags) {
                if (tag == null || tag.isBlank()) continue;
                String trimmed = tag.trim();
                unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        this.tags = new ArrayList<>(unique.values());
    }

    public List<String> folderIds() {
        return List.copyOf(folderIds);
    }

    public boolean isInFolder(String folderId) {
        return folderId != null && folderIds.contains(folderId);
    }

    public void addToFolder(String folderId) {
        if (folderId != null && !folderIds.contains(folderId)) folderIds.add(folderId);
    }

    public void removeFromFolder(String folderId) {
        folderIds.remove(folderId);
    }

    public void clearFolders() {
        folderIds.clear();
    }

    public boolean isPinnedIn(String tab) {
        return tab != null && pinnedTabs.contains(tab);
    }

    public boolean isPinnedAnywhere() {
        return !pinnedTabs.isEmpty();
    }

    public void setPinnedIn(String tab, boolean pinned) {
        if (tab == null) return;
        pinnedTabs.remove(tab);
        if (pinned) pinnedTabs.add(tab);
    }

    public List<String> pinnedTabs() {
        return List.copyOf(pinnedTabs);
    }

    public boolean hiddenInAll() {
        return hiddenInAll;
    }

    public void setHiddenInAll(boolean hiddenInAll) {
        this.hiddenInAll = hiddenInAll;
    }

    public Long orphanedSince() {
        return orphanedSince;
    }

    void setOrphanedSince(Long orphanedSince) {
        this.orphanedSince = orphanedSince;
    }

    public boolean isEmpty() {
        return notes.isEmpty() && tags.isEmpty() && folderIds.isEmpty() && pinnedTabs.isEmpty() && !hiddenInAll;
    }

    /**
     * Folds {@code other} into this entry, used when two keys collapse into one.
     * Folders, pins and tags are united and differing notes are kept both.
     */
    void absorb(ServerMeta other) {
        for (String tab : other.pinnedTabs) setPinnedIn(tab, true);
        hiddenInAll |= other.hiddenInAll;
        notesOnJoin |= other.notesOnJoin;
        for (String folder : other.folderIds) addToFolder(folder);

        List<String> merged = new ArrayList<>(tags);
        merged.addAll(other.tags);
        setTags(merged);

        if (notes.isEmpty()) notes = other.notes;
        else if (!other.notes.isEmpty() && !notes.equals(other.notes)) notes = notes + "\n" + other.notes;
    }

    /** An independent copy, for a second server that starts out with the same data. */
    ServerMeta copy() {
        ServerMeta copy = new ServerMeta();
        copy.absorb(this);
        copy.orphanedSince = orphanedSince;
        return copy;
    }

    /** Repairs fields Gson may have left null when reading old or hand-edited files. */
    void sanitize() {
        if (notes == null) notes = "";
        setTags(tags);
        List<String> folders = folderIds == null ? List.of() : folderIds;
        folderIds = new ArrayList<>(folders.stream().filter(id -> id != null && !id.isBlank()).distinct().toList());
        List<String> pins = pinnedTabs == null ? List.of() : pinnedTabs;
        pinnedTabs = new ArrayList<>(pins.stream().filter(tab -> tab != null && !tab.isBlank()).distinct().toList());
    }
}
