package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ui.text.InfoText;
import me.prexorjustin.serverlistplus.ui.text.NameText;
import me.prexorjustin.serverlistplus.ui.widget.Glyph;
import me.prexorjustin.serverlistplus.ui.widget.IconButton;
import me.prexorjustin.serverlistplus.ui.widget.SlpWidget;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.core.model.Activity;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * A card with a server's history, its notes and tags: docked next to the server list when there
 * is room, otherwise over it. It draws the card and the tag chips itself; the input fields are
 * regular widgets on top of it, placed by {@link #layout()}.
 */
public final class ServerInfoPanel extends SlpWidget {
    public static final int WIDTH = 280;
    public static final int MIN_WIDTH = 200;
    private static final int PAD = 10;
    private static final int LINE = 12;
    private static final int CHIP_HEIGHT = 12;
    private static final int CHIP_GAP = 3;
    private static final int MIN_NOTES_HEIGHT = 40;
    private static final int MIN_DOCKED_NOTES_HEIGHT = 28;
    private static final int MAX_NOTES_HEIGHT = 110;
    /** Everything but the notes box in the card over the list, with one row of tag chips. */
    private static final int REST_HEIGHT = 180;
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
    /** Docked next to the list: exactly {@link #maxHeight} tall, closed by a symbol instead of a button row. */
    private final boolean docked;
    private final int maxHeight;
    private final EditBox tagInput;
    private final MultiLineEditBox notes;
    private final Checkbox notesOnJoin;
    private final AbstractWidget close;
    private final List<Chip> chips = new ArrayList<>();
    private int tagsLabelY;
    private int notesLabelY;

    /**
     * @param maxHeight room below {@code y}; the notes box shrinks to fit
     * @param docked    next to the list: the card is {@code maxHeight} tall, the notes box takes what is left
     */
    public ServerInfoPanel(Font font, ServerData server, ServerMeta meta, int x, int y, int width, int maxHeight, boolean docked, Runnable onDone) {
        super(x, y, width, 0, Component.literal(NameText.plain(server.name)));
        this.font = font;
        this.server = server;
        this.meta = meta;
        this.docked = docked;
        this.maxHeight = maxHeight;

        int innerWidth = width - 2 * PAD;
        tagInput = new EditBox(font, 0, 0, innerWidth, 18, Component.translatable("serverlistplus.info.tag_hint"));
        tagInput.setMaxLength(32);
        tagInput.setHint(Component.translatable("serverlistplus.info.tag_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));

        // Docked, layout() sets the height once it knows how many rows the tags take
        int notesHeight = docked ? MIN_DOCKED_NOTES_HEIGHT : Math.clamp(maxHeight - REST_HEIGHT, MIN_NOTES_HEIGHT, MAX_NOTES_HEIGHT);
        Component notesHint = Component.translatable("serverlistplus.info.notes_hint").withStyle(ChatFormatting.GRAY);
        //? if >=1.21.6 {
        notes = MultiLineEditBox.builder().setPlaceholder(notesHint).build(font, innerWidth, notesHeight, Component.translatable("serverlistplus.info.notes"));
        //?} else
        /*notes = new MultiLineEditBox(font, 0, 0, innerWidth, notesHeight, notesHint, Component.translatable("serverlistplus.info.notes"));*/
        notes.setCharacterLimit(2000);
        notes.setValue(meta.notes());
        // Kept up to date while typing, so joining straight from the card loses nothing
        notes.setValueListener(meta::setNotes);
        notesOnJoin = Checkbox.builder(Component.translatable("serverlistplus.info.notes_on_join"), font)
            .selected(meta.notesOnJoin())
            .onValueChange((checkbox, selected) -> meta.setNotesOnJoin(selected))
            .build();
        notesOnJoin.setTooltip(Tooltip.create(Component.translatable("serverlistplus.info.notes_on_join.tooltip")));

        close = docked
            ? new IconButton(Glyph.CLOSE, CommonComponents.GUI_DONE, b -> onDone.run(), () -> false)
            : Button.builder(CommonComponents.GUI_DONE, b -> onDone.run()).bounds(0, 0, 80, 20).build();
        layout();
    }

    /** The input widgets, to be added to the screen after this panel so they draw on top of it. */
    public List<AbstractWidget> children() {
        return List.of(notes, notesOnJoin, tagInput, close);
    }

    public EditBox tagInput() {
        return tagInput;
    }

    /** Where typing starts when the card opens. */
    public AbstractWidget initialFocus() {
        return notes;
    }

    /** Whether the point is over one of the input widgets, which then take the click themselves. */
    public boolean isOverInput(double x, double y) {
        return children().stream().anyMatch(widget -> widget.visible && widget.isMouseOver(x, y));
    }

    /** Adds tags still left in the input; notes and the checkbox are stored as they change. */
    public void save() {
        if (!tagInput.getValue().isBlank()) addTypedTags();
    }

    /** Positions chips and input fields; call after the tags changed. */
    public void layout() {
        int left = this.getX() + PAD;
        int right = this.getRight() - PAD;
        if (docked) {
            close.setPosition(this.getRight() - IconButton.SIZE - 2, this.getY() + 2);
            // Everything but the notes box, so the box can take the rest
            int fixed = PAD + LINE * 3 + 4 + LINE + 4 + notesOnJoin.getHeight() + 8 + LINE + placeChips(left, right, 0) + 18 + PAD;
            notes.setHeight(Math.max(maxHeight - fixed, MIN_DOCKED_NOTES_HEIGHT));
        }
        int y = this.getY() + PAD + LINE * 3 + 4;

        notesLabelY = y;
        y += LINE;
        notes.setPosition(left, y);
        y += notes.getHeight() + 4;
        notesOnJoin.setPosition(left, y);
        y += notesOnJoin.getHeight() + 8;

        tagsLabelY = y;
        y += LINE;
        y += placeChips(left, right, y);
        tagInput.setPosition(left, y);
        y += 18;

        if (docked) {
            y += PAD;
        } else {
            y += 8;
            close.setPosition(right - close.getWidth(), y);
            y += 20 + PAD;
        }
        this.setHeight(y - this.getY());
    }

    /** Lays the tag chips out in rows from {@code top}; returns the height they take. */
    private int placeChips(int left, int right, int top) {
        chips.clear();
        int x = left;
        int y = top;
        for (String tag : meta.tags()) {
            int width = font.width(tag + " ✕") + 8;
            if (x + width > right && x > left) {
                x = left;
                y += CHIP_HEIGHT + CHIP_GAP;
            }
            chips.add(new Chip(tag, x, y, width));
            x += width + CHIP_GAP;
        }
        return chips.isEmpty() ? 0 : y - top + CHIP_HEIGHT + CHIP_GAP + 2;
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
        gfx.roundedRect(x, y, this.getRight(), this.getBottom(), BACKGROUND);
        gfx.outline(x, y, this.getRight(), this.getBottom(), BORDER);

        int left = x + PAD;
        // Long names are cut at the edge of the card
        Component title = NameText.of(server.name).copy().withStyle(ChatFormatting.BOLD);
        int titleRoom = this.getWidth() - 2 * PAD - (docked ? IconButton.SIZE - 4 : 0);
        List<FormattedCharSequence> titleLines = font.split(title, titleRoom);
        if (!titleLines.isEmpty()) gfx.text(font, titleLines.getFirst(), left, y + PAD, TITLE);
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
