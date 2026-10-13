package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.core.model.Activity;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A card over the server list with a server's history, its tags and notes.
 * It draws the card and the tag chips itself; the input fields are regular widgets on top of it,
 * placed by {@link #layout()}.
 */
public final class ServerInfoPanel extends SlpWidget {
    public static final int WIDTH = 280;
    private static final int PAD = 10;
    private static final int LINE = 12;
    private static final int CHIP_HEIGHT = 12;
    private static final int CHIP_GAP = 3;
    private static final int NOTES_HEIGHT = 64;
    private static final int BACKGROUND = 0xF2161616;
    private static final int BORDER = 0xFF484848;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int LABEL = 0xFFA0A0A0;
    private static final int TEXT = 0xFFE0E0E0;

    private record Chip(String tag, int x, int y, int width) {
        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + CHIP_HEIGHT;
        }
    }

    private final Font font;
    private final ServerData server;
    private final ServerMeta meta;
    private final EditBox tagInput;
    private final MultiLineEditBox notes;
    private final Button done;
    private final List<Chip> chips = new ArrayList<>();
    private int tagsLabelY;
    private int notesLabelY;

    public ServerInfoPanel(Font font, ServerData server, ServerMeta meta, int x, int y, Runnable onDone) {
        super(x, y, WIDTH, 0, Component.literal(NameText.plain(server.name)));
        this.font = font;
        this.server = server;
        this.meta = meta;

        int innerWidth = WIDTH - 2 * PAD;
        tagInput = new EditBox(font, 0, 0, innerWidth, 18, Component.translatable("serverlistplus.info.tag_hint"));
        tagInput.setMaxLength(32);
        tagInput.setHint(Component.translatable("serverlistplus.info.tag_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));

        Component notesHint = Component.translatable("serverlistplus.info.notes_hint").withStyle(ChatFormatting.GRAY);
        //? if >=1.21.6 {
        notes = MultiLineEditBox.builder().setPlaceholder(notesHint).build(font, innerWidth, NOTES_HEIGHT, Component.translatable("serverlistplus.info.notes"));
        //?} else
        /*notes = new MultiLineEditBox(font, 0, 0, innerWidth, NOTES_HEIGHT, notesHint, Component.translatable("serverlistplus.info.notes"));*/
        notes.setCharacterLimit(1000);
        notes.setValue(meta.notes());

        done = Button.builder(CommonComponents.GUI_DONE, b -> onDone.run()).bounds(0, 0, 80, 20).build();
        layout();
    }

    /** The input widgets, to be added to the screen after this panel so they draw on top of it. */
    public List<AbstractWidget> children() {
        return List.of(tagInput, notes, done);
    }

    public EditBox tagInput() {
        return tagInput;
    }

    /** Stores the typed notes and any tags still left in the input. */
    public void save() {
        if (!tagInput.getValue().isBlank()) addTypedTags();
        meta.setNotes(notes.getValue());
    }

    /** Positions chips and input fields; call after the tags changed. */
    public void layout() {
        int left = this.getX() + PAD;
        int right = this.getX() + WIDTH - PAD;
        int y = this.getY() + PAD + LINE * 3 + 4;

        tagsLabelY = y;
        y += LINE;
        chips.clear();
        int x = left;
        for (String tag : meta.tags()) {
            int width = font.width(tag + " ✕") + 8;
            if (x + width > right && x > left) {
                x = left;
                y += CHIP_HEIGHT + CHIP_GAP;
            }
            chips.add(new Chip(tag, x, y, width));
            x += width + CHIP_GAP;
        }
        if (!chips.isEmpty()) y += CHIP_HEIGHT + CHIP_GAP + 2;
        tagInput.setPosition(left, y);
        y += 18 + 8;

        notesLabelY = y;
        y += LINE;
        notes.setPosition(left, y);
        y += NOTES_HEIGHT + 8;

        done.setPosition(right - done.getWidth(), y);
        y += 20 + PAD;
        this.setHeight(y - this.getY());
    }

    /** Removes a tag when its chip is clicked. @return true if a chip was hit */
    public boolean clickChip(double x, double y) {
        for (Chip chip : chips) {
            if (!chip.contains(x, y)) continue;
            List<String> tags = new ArrayList<>(meta.tags());
            tags.remove(chip.tag());
            meta.setTags(tags);
            layout();
            return true;
        }
        return false;
    }

    /** Adds the typed tags (comma separated) and clears the input. */
    public void addTypedTags() {
        List<String> tags = new ArrayList<>(meta.tags());
        for (String tag : tagInput.getValue().split(",")) {
            if (!tag.isBlank()) tags.add(tag.trim());
        }
        meta.setTags(tags);
        tagInput.setValue("");
        layout();
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        int x = this.getX();
        int y = this.getY();
        gfx.overlay();
        gfx.roundedRect(x, y, x + WIDTH, y + this.getHeight(), BACKGROUND);
        gfx.outline(x, y, x + WIDTH, y + this.getHeight(), BORDER);

        int left = x + PAD;
        gfx.text(font, NameText.of(server.name).copy().withStyle(ChatFormatting.BOLD), left, y + PAD, TITLE);
        Activity activity = ServerView.activity(server);
        long last = activity == null ? 0 : activity.lastPlayed();
        Component lastPlayed = last > 0 ? InfoText.ago(last) : Component.translatable("serverlistplus.info.never");
        gfx.text(font, Component.translatable("serverlistplus.info.last_played", lastPlayed), left, y + PAD + LINE + 2, TEXT);
        gfx.text(font, Component.translatable("serverlistplus.info.playtime", InfoText.playtime(activity == null ? 0 : activity.playtimeMs())),
            left, y + PAD + LINE * 2 + 2, TEXT);

        gfx.text(font, Component.translatable("serverlistplus.info.tags"), left, tagsLabelY, LABEL);
        for (Chip chip : chips) {
            int color = InfoText.tagColor(chip.tag());
            if (chip.contains(mouseX, mouseY)) color |= 0xFF000000;
            gfx.roundedRect(chip.x(), chip.y(), chip.x() + chip.width(), chip.y() + CHIP_HEIGHT, color);
            gfx.text(font, Component.literal(chip.tag() + " ✕"), chip.x() + 4, chip.y() + 2, TEXT);
        }
        gfx.text(font, Component.translatable("serverlistplus.info.notes"), left, notesLabelY, LABEL);
    }
}
