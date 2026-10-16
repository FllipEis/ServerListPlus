package me.prexorjustin.serverlistplus.ui.text;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Shortened views of a server's notes, for the tooltip in the server list and the toast on joining.
 */
public final class NotePreview {
    private static final int TOOLTIP_WIDTH = 220;
    private static final int TOOLTIP_LINES = 12;

    private NotePreview() {
    }

    /** The notes wrapped to tooltip width, cut after a dozen lines, with a hint how to edit them. */
    public static List<FormattedCharSequence> tooltip(Font font, String notes) {
        List<FormattedCharSequence> lines = lines(font, notes, TOOLTIP_WIDTH, TOOLTIP_LINES);
        lines.add(FormattedCharSequence.EMPTY);
        lines.add(Component.translatable("serverlistplus.notes.edit_hint").withStyle(ChatFormatting.GRAY).getVisualOrderText());
        return lines;
    }

    /** The notes wrapped to {@code width}, keeping empty lines; after {@code maxLines} the rest becomes "…". */
    public static List<FormattedCharSequence> lines(Font font, String notes, int width, int maxLines) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : notes.strip().split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(font.split(Component.literal(paragraph), width));
            }
        }
        if (lines.size() > maxLines) {
            lines.subList(maxLines - 1, lines.size()).clear();
            lines.add(Component.literal("…").getVisualOrderText());
        }
        return lines;
    }
}
