package me.prexorjustin.serverlistplus.core.model;

/**
 * Where a server row shows when it was last played and for how long.
 */
public enum DetailsMode {
    /** As text after the server name. */
    INLINE,
    /** As a clock symbol after the name; the text appears when hovering it. */
    ICON,
    /** Hidden until the row is hovered. */
    HOVER,
    /** Not shown in the row at all. */
    OFF
}
