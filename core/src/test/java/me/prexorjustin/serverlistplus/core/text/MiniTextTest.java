package me.prexorjustin.serverlistplus.core.text;

import me.prexorjustin.serverlistplus.core.text.MiniText.Span;
import me.prexorjustin.serverlistplus.core.text.MiniText.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniTextTest {
    private static final Style RED = Style.NONE.withColor(0xFF5555);

    @ParameterizedTest
    @ValueSource(strings = {"Hypixel", "a < b", "<3 SMP", "<unknown>Server", "Lobby <foo:bar>", ""})
    void textWithoutTagsStaysAsTyped(String name) {
        MiniText.Result result = MiniText.parse(name);
        assertFalse(result.styled());
        assertEquals(name, result.plain());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "<red>Hypixel</red> Lobby                | Hypixel Lobby",
        "<gradient:#ff5555:#ffaa00>Hypixel       | Hypixel",
        "<bold><rainbow>SMP</rainbow></bold>     | SMP",
        "<#00ff00>Green <unknown>x               | Green <unknown>x",
        "<red>a \\<b>                             | a <b>",
        "</red>Lone closing tag                  | Lone closing tag",
    })
    void plainText(String name, String plain) {
        assertEquals(plain, MiniText.plain(name));
    }

    @Test
    void colorsAndClosingTags() {
        assertEquals(List.of(new Span("Hy", RED), new Span("pixel", Style.NONE)), MiniText.parse("<red>Hy</red>pixel").spans());
        assertEquals(List.of(new Span("Hy", RED), new Span("pixel", Style.NONE)), MiniText.parse("<color:red>Hy</color>pixel").spans());
        assertEquals(List.of(new Span("x", Style.NONE.withColor(0x12ABEF))), MiniText.parse("<#12ABEF>x").spans());
    }

    @Test
    void decorationsCanBeNegated() {
        Style bold = Style.NONE.with(MiniText.Decoration.BOLD, true);
        Style notBold = Style.NONE.with(MiniText.Decoration.BOLD, false);
        assertEquals(List.of(new Span("a", bold), new Span("b", notBold), new Span("c", bold)),
            MiniText.parse("<b>a<!bold>b</!bold>c").spans());
    }

    @Test
    void closingATagClosesTagsOpenedInside() {
        Style redBold = RED.with(MiniText.Decoration.BOLD, true);
        assertEquals(List.of(new Span("a", redBold), new Span("b", Style.NONE)), MiniText.parse("<red><bold>a</red>b").spans());
    }

    @Test
    void resetClosesEverything() {
        assertEquals(List.of(new Span("a", RED.with(MiniText.Decoration.ITALIC, true)), new Span("b", Style.NONE)),
            MiniText.parse("<red><i>a<reset>b").spans());
    }

    @Test
    void gradientRunsFromFirstToLastColor() {
        List<Span> spans = MiniText.parse("<gradient:#000000:#ffffff>abc</gradient>").spans();
        assertEquals(3, spans.size());
        assertEquals(0x000000, spans.get(0).style().color());
        assertEquals(0x808080, spans.get(1).style().color());
        assertEquals(0xFFFFFF, spans.get(2).style().color());
    }

    @Test
    void colorInsideGradientWins() {
        List<Span> spans = MiniText.parse("<gradient:#000000:#ffffff>a<red>b</red>c</gradient>").spans();
        assertEquals(new Span("b", RED), spans.get(1));
    }

    @Test
    void rainbowStartsAtRed() {
        assertEquals(0xFF0000, MiniText.parse("<rainbow>abc").spans().get(0).style().color());
    }

    @Test
    void tagsAreCaseInsensitive() {
        assertTrue(MiniText.parse("<RED>x").styled());
    }
}
