package me.prexorjustin.serverlistplus.ui.text;

import me.prexorjustin.serverlistplus.core.text.MiniText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server names with MiniMessage tags as formatted text. Names are shown every frame, so the
 * result is kept for the names seen recently.
 */
public final class NameText {
    private static final int CACHE_SIZE = 256;
    private static final Map<String, Parsed> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Parsed> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private record Parsed(Component text, boolean styled) {
    }

    private NameText() {
    }

    /** True if the name contains tags, so vanilla's plain drawing of it must be replaced. */
    public static boolean isStyled(String name) {
        return name != null && name.indexOf('<') >= 0 && parsed(name).styled();
    }

    public static Component of(String name) {
        if (name == null) return Component.empty();
        if (name.indexOf('<') < 0) return Component.literal(name);
        return parsed(name).text();
    }

    private static Parsed parsed(String name) {
        return CACHE.computeIfAbsent(name, NameText::build);
    }

    /** The name as shown, without tags, e.g. for narration. */
    public static String plain(String name) {
        return name == null ? "" : MiniText.plain(name);
    }

    private static Parsed build(String name) {
        MiniText.Result result = MiniText.parse(name);
        if (!result.styled()) return new Parsed(Component.literal(name), false);
        MutableComponent text = Component.empty();
        for (MiniText.Span span : result.spans()) {
            MiniText.Style style = span.style();
            Style mc = Style.EMPTY
                .withBold(style.bold())
                .withItalic(style.italic())
                .withUnderlined(style.underlined())
                .withStrikethrough(style.strikethrough())
                .withObfuscated(style.obfuscated());
            if (style.color() != null) mc = mc.withColor(TextColor.fromRgb(style.color()));
            text.append(Component.literal(span.text()).setStyle(mc));
        }
        return new Parsed(text, true);
    }
}
