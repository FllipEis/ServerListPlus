package me.prexorjustin.serverlistplus.core.model;

import me.prexorjustin.serverlistplus.core.style.Palette;

import java.util.UUID;

/**
 * A separator line in the server list of one tab, optionally with a title.
 * It sits directly above the server it is anchored to, or at the end of the list without anchor.
 */
public final class Divider {
    private String id;
    private String tab;
    /** Address key of the server below the divider, null for the end of the list. */
    private String beforeKey;
    private String label;
    /** Palette key, null to use the color of the tab. */
    private String color;

    public Divider(String tab, String beforeKey) {
        this.id = UUID.randomUUID().toString();
        this.tab = tab;
        this.beforeKey = beforeKey;
        this.label = "";
    }

    public String id() {
        return id;
    }

    public String tab() {
        return tab;
    }

    public String beforeKey() {
        return beforeKey;
    }

    public void setBeforeKey(String beforeKey) {
        this.beforeKey = beforeKey;
    }

    public String label() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label == null ? "" : label.trim();
    }

    /** Own color of the divider, null if it uses the color of its tab. */
    public Palette.Color color() {
        return color == null ? null : Palette.byKey(color).orElse(null);
    }

    public void setColor(Palette.Color color) {
        this.color = color == null ? null : color.key();
    }

    boolean isValid() {
        return id != null && tab != null;
    }

    void sanitize() {
        if (label == null) label = "";
    }
}
