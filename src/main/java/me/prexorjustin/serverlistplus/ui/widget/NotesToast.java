package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.ui.text.NameText;
import me.prexorjustin.serverlistplus.ui.text.NotePreview;
import me.prexorjustin.serverlistplus.compat.Gfx;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.util.FormattedCharSequence;
//? if >=1.21.2 {
import net.minecraft.client.gui.components.toasts.ToastManager;
//?} else
/*import net.minecraft.client.gui.components.toasts.ToastComponent;*/

import java.util.List;

/**
 * A server's notes in the corner of the screen when joining it: the server's name above the
 * start of its notes, with a bar running out until the toast leaves. Longer notes stay longer.
 */
public final class NotesToast implements Toast {
    private static final int WIDTH = 220;
    private static final int PAD = 8;
    /** Where text starts, right of the accent stripe. */
    private static final int INSET = PAD + 4;
    private static final int LINE = 10;
    private static final int MAX_LINES = 6;
    private static final long BASE_MILLIS = 6_000;
    private static final long PER_LINE_MILLIS = 800;

    private static final int BACKGROUND = 0xF0121418;
    private static final int EDGE = 0x24FFFFFF;
    private static final int ACCENT = 0xFFE8C468;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFC9CDD4;
    private static final int TRACK = 0x22FFFFFF;
    private static final int BAR = 0xB0E8C468;

    private final FormattedCharSequence titleLine;
    private final List<FormattedCharSequence> lines;
    private final long millis;
    private long shownFor;
    //? if >=1.21.2
    private Toast.Visibility visibility = Toast.Visibility.SHOW;

    /** @param serverName the name as saved, MiniMessage tags are shown formatted */
    public NotesToast(String serverName, String notes) {
        Font font = Minecraft.getInstance().font;
        int titleRoom = WIDTH - INSET - Glyph.NOTE.width() - 5 - PAD;
        List<FormattedCharSequence> titleLines = font.split(NameText.of(serverName).copy().withStyle(ChatFormatting.BOLD), titleRoom);
        this.titleLine = titleLines.isEmpty() ? FormattedCharSequence.EMPTY : titleLines.getFirst();
        this.lines = NotePreview.lines(font, notes, WIDTH - INSET - PAD, MAX_LINES);
        this.millis = BASE_MILLIS + PER_LINE_MILLIS * lines.size();
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return PAD + 9 + 6 + lines.size() * LINE + PAD + 2;
    }

    private void draw(Gfx gfx, Font font, long duration) {
        int height = height();
        gfx.roundedRect(0, 0, WIDTH, height, 3, BACKGROUND);
        // A faint top edge lifts the card off bright backgrounds
        gfx.fill(3, 0, WIDTH - 3, 1, EDGE);
        gfx.roundedRect(PAD - 3, PAD, PAD - 1, height - PAD - 2, ACCENT);

        Glyph.NOTE.draw(gfx, INSET, PAD + 1, ACCENT);
        gfx.text(font, titleLine, INSET + Glyph.NOTE.width() + 5, PAD, TITLE);
        int y = PAD + 9 + 6;
        for (FormattedCharSequence line : lines) {
            gfx.text(font, line, INSET, y, TEXT);
            y += LINE;
        }

        // Runs out from right to left while the toast is shown
        int barLeft = INSET;
        int barRight = WIDTH - PAD;
        int barY = height - 5;
        float left = 1 - Math.min(1f, (float) shownFor / duration);
        gfx.fill(barLeft, barY, barRight, barY + 1, TRACK);
        gfx.fill(barLeft, barY, barLeft + Math.round((barRight - barLeft) * left), barY + 1, BAR);
    }

    //? if >=1.21.2 {
    private long duration = BASE_MILLIS;

    @Override
    public Toast.Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleFor) {
        duration = Math.round(millis * manager.getNotificationDisplayTimeMultiplier());
        shownFor = fullyVisibleFor;
        visibility = fullyVisibleFor >= duration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleFor) {
        draw(new Gfx(graphics), font, duration);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, Font font, long fullyVisibleFor) {
        draw(new Gfx(graphics), font, duration);
    }
    *///?}
    //?} else {
    /*@Override
    public Toast.Visibility render(GuiGraphicsExtractor graphics, ToastComponent component, long timeSinceVisible) {
        long duration = Math.round(millis * component.getNotificationDisplayTimeMultiplier());
        shownFor = timeSinceVisible;
        draw(new Gfx(graphics), Minecraft.getInstance().font, duration);
        return timeSinceVisible >= duration ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
    *///?}
}
