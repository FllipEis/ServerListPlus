package me.prexorjustin.serverlistplus.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The part of MiniMessage that makes sense in a server name: colors, decorations, gradients,
 * rainbows and reset, e.g. {@code <gradient:#ff5555:#ffaa00>Hypixel</gradient> <gray>Lobby}.
 * <p>
 * Tags that are not understood stay in the text as typed, {@code \<} writes a literal {@code <}.
 */
public final class MiniText {
    private static final Map<String, Integer> NAMED_COLORS = Map.ofEntries(
        Map.entry("black", 0x000000), Map.entry("dark_blue", 0x0000AA),
        Map.entry("dark_green", 0x00AA00), Map.entry("dark_aqua", 0x00AAAA),
        Map.entry("dark_red", 0xAA0000), Map.entry("dark_purple", 0xAA00AA),
        Map.entry("gold", 0xFFAA00), Map.entry("gray", 0xAAAAAA), Map.entry("grey", 0xAAAAAA),
        Map.entry("dark_gray", 0x555555), Map.entry("dark_grey", 0x555555),
        Map.entry("blue", 0x5555FF), Map.entry("green", 0x55FF55), Map.entry("aqua", 0x55FFFF),
        Map.entry("red", 0xFF5555), Map.entry("light_purple", 0xFF55FF),
        Map.entry("yellow", 0xFFFF55), Map.entry("white", 0xFFFFFF));
    private static final Map<String, Decoration> DECORATIONS = Map.ofEntries(
        Map.entry("bold", Decoration.BOLD), Map.entry("b", Decoration.BOLD),
        Map.entry("italic", Decoration.ITALIC), Map.entry("i", Decoration.ITALIC), Map.entry("em", Decoration.ITALIC),
        Map.entry("underlined", Decoration.UNDERLINED), Map.entry("u", Decoration.UNDERLINED),
        Map.entry("strikethrough", Decoration.STRIKETHROUGH), Map.entry("st", Decoration.STRIKETHROUGH),
        Map.entry("obfuscated", Decoration.OBFUSCATED), Map.entry("obf", Decoration.OBFUSCATED));

    private MiniText() {
    }

    public enum Decoration {
        BOLD, ITALIC, UNDERLINED, STRIKETHROUGH, OBFUSCATED
    }

    /**
     * Formatting of a piece of text. Null means inherited from the surrounding text.
     *
     * @param color RGB color, null for the default
     */
    public record Style(Integer color, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough, Boolean obfuscated) {
        public static final Style NONE = new Style(null, null, null, null, null, null);

        Style withColor(Integer color) {
            return new Style(color, bold, italic, underlined, strikethrough, obfuscated);
        }

        Style with(Decoration decoration, boolean value) {
            return switch (decoration) {
                case BOLD -> new Style(color, value, italic, underlined, strikethrough, obfuscated);
                case ITALIC -> new Style(color, bold, value, underlined, strikethrough, obfuscated);
                case UNDERLINED -> new Style(color, bold, italic, value, strikethrough, obfuscated);
                case STRIKETHROUGH -> new Style(color, bold, italic, underlined, value, obfuscated);
                case OBFUSCATED -> new Style(color, bold, italic, underlined, strikethrough, value);
            };
        }
    }

    public record Span(String text, Style style) {
    }

    /**
     * @param spans  the text in pieces of the same formatting
     * @param styled false if the text contains no tag at all, so it can be shown as it is
     */
    public record Result(List<Span> spans, boolean styled) {
        public String plain() {
            StringBuilder plain = new StringBuilder();
            spans.forEach(span -> plain.append(span.text()));
            return plain.toString();
        }
    }

    /** The text without tags, as it is shown. */
    public static String plain(String text) {
        return parse(text).plain();
    }

    public static Result parse(String text) {
        if (text == null || text.isEmpty()) return new Result(List.of(), false);
        if (text.indexOf('<') < 0) return new Result(List.of(new Span(text, Style.NONE)), false);
        Node root = new Node(null, null);
        boolean styled = new Parser(text, root).run();
        if (!styled) return new Result(List.of(new Span(text, Style.NONE)), false);
        List<Span> spans = new ArrayList<>();
        render(root, Style.NONE, null, spans);
        return new Result(merge(spans), true);
    }

    // ---- Parsing into a tree ----

    /** A tag with the text and tags inside it, or a piece of text. */
    private static final class Node {
        final Tag tag;
        final String text;
        final List<Node> children = new ArrayList<>();

        Node(Tag tag, String text) {
            this.tag = tag;
            this.text = text;
        }

        int length() {
            if (text != null) return text.codePointCount(0, text.length());
            int length = 0;
            for (Node child : children) length += child.length();
            return length;
        }
    }

    private sealed interface Tag permits ColorTag, DecorationTag, GradientTag, RainbowTag {
        /** Name used to match the closing tag. */
        String name();
    }

    private record ColorTag(String name, int rgb) implements Tag {
    }

    private record DecorationTag(String name, Decoration decoration, boolean value) implements Tag {
    }

    private record GradientTag(String name, int[] colors) implements Tag {
    }

    private record RainbowTag(String name, boolean reversed, double phase) implements Tag {
    }

    private static final class Parser {
        private final String input;
        private final List<Node> open = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private boolean styled;

        Parser(String input, Node root) {
            this.input = input;
            open.add(root);
        }

        /** @return true if at least one tag was understood */
        boolean run() {
            int i = 0;
            while (i < input.length()) {
                char c = input.charAt(i);
                if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '<') {
                    text.append('<');
                    i += 2;
                    continue;
                }
                int end = c == '<' ? input.indexOf('>', i + 1) : -1;
                if (end < 0 || !tag(input.substring(i + 1, end))) {
                    text.append(c);
                    i++;
                    continue;
                }
                i = end + 1;
            }
            flush();
            return styled;
        }

        private Node current() {
            return open.get(open.size() - 1);
        }

        private void flush() {
            if (text.isEmpty()) return;
            current().children.add(new Node(null, text.toString()));
            text.setLength(0);
        }

        /** Applies a tag; false if it is not one, so it stays as text. */
        private boolean tag(String content) {
            if (content.isEmpty()) return false;
            String lower = content.toLowerCase(Locale.ROOT);
            if (lower.equals("reset")) {
                flush();
                while (open.size() > 1) open.remove(open.size() - 1);
                return styled = true;
            }
            if (lower.startsWith("/")) return close(lower.substring(1));
            Tag tag = parseTag(lower);
            if (tag == null) return false;
            flush();
            Node node = new Node(tag, null);
            current().children.add(node);
            open.add(node);
            return styled = true;
        }

        /** Closes the innermost open tag of that name and all tags opened after it. */
        private boolean close(String content) {
            String name = canonical(content.split(":", 2)[0]);
            if (name == null) return false;
            for (int i = open.size() - 1; i > 0; i--) {
                if (!open.get(i).tag.name().equals(name)) continue;
                flush();
                while (open.size() > i) open.remove(open.size() - 1);
                break;
            }
            // A known closing tag without an open one is dropped, as MiniMessage does
            return styled = true;
        }
    }

    /** The name a closing tag must use to close a tag, or null if the tag is unknown. */
    private static String canonical(String name) {
        boolean negated = name.startsWith("!");
        String bare = negated ? name.substring(1) : name;
        Decoration decoration = DECORATIONS.get(bare);
        if (decoration != null) return decoration.name();
        if (negated) return null;
        if (bare.equals("gradient") || bare.equals("rainbow")) return bare;
        if (bare.equals("color") || bare.equals("colour") || bare.equals("c") || NAMED_COLORS.containsKey(bare) || parseHex(bare) != null) {
            return "color";
        }
        return null;
    }

    private static Tag parseTag(String content) {
        String[] parts = content.split(":", -1);
        String name = parts[0];
        if (name.startsWith("!")) {
            Decoration decoration = DECORATIONS.get(name.substring(1));
            return decoration == null || parts.length > 1 ? null : new DecorationTag(decoration.name(), decoration, false);
        }
        Decoration decoration = DECORATIONS.get(name);
        if (decoration != null) {
            if (parts.length == 1) return new DecorationTag(decoration.name(), decoration, true);
            if (parts.length == 2 && (parts[1].equals("true") || parts[1].equals("false"))) {
                return new DecorationTag(decoration.name(), decoration, parts[1].equals("true"));
            }
            return null;
        }
        Integer color = parts.length == 1 ? color(name) : null;
        if (color != null) return new ColorTag("color", color);
        switch (name) {
            case "color", "colour", "c" -> {
                Integer value = parts.length == 2 ? color(parts[1]) : null;
                return value == null ? null : new ColorTag("color", value);
            }
            case "gradient" -> {
                return gradient(parts);
            }
            case "rainbow" -> {
                return rainbow(parts);
            }
            default -> {
                return null;
            }
        }
    }

    /** {@code <gradient[:color...][:phase]>}, white to black without colors. */
    private static GradientTag gradient(String[] parts) {
        List<Integer> colors = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            Integer color = color(parts[i]);
            if (color != null) {
                colors.add(color);
                continue;
            }
            // A trailing phase is accepted, though a static name has nothing to animate
            if (i < parts.length - 1 || phase(parts[i]) == null) return null;
        }
        if (colors.isEmpty()) colors = List.of(0xFFFFFF, 0x000000);
        if (colors.size() == 1) return null;
        return new GradientTag("gradient", colors.stream().mapToInt(Integer::intValue).toArray());
    }

    /** {@code <rainbow[:!][phase]>}, the {@code !} reverses the direction. */
    private static RainbowTag rainbow(String[] parts) {
        if (parts.length == 1) return new RainbowTag("rainbow", false, 0);
        if (parts.length > 2) return null;
        String arg = parts[1];
        boolean reversed = arg.startsWith("!");
        if (reversed) arg = arg.substring(1);
        if (arg.isEmpty()) return new RainbowTag("rainbow", reversed, 0);
        Double phase = phase(arg);
        return phase == null ? null : new RainbowTag("rainbow", reversed, phase);
    }

    private static Double phase(String text) {
        try {
            double value = Double.parseDouble(text);
            return value >= -1 && value <= 1 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer color(String text) {
        Integer named = NAMED_COLORS.get(text);
        return named != null ? named : parseHex(text);
    }

    private static Integer parseHex(String text) {
        if (text.length() != 7 || text.charAt(0) != '#') return null;
        try {
            return Integer.parseInt(text.substring(1), 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- Turning the tree into spans ----

    /** Hands out one color per character for a gradient or rainbow. */
    private interface Colorizer {
        int next();
    }

    private static void render(Node node, Style style, Colorizer colorizer, List<Span> out) {
        if (node.text != null) {
            if (colorizer == null) {
                out.add(new Span(node.text, style));
            } else {
                for (int cp : node.text.codePoints().toArray()) {
                    out.add(new Span(Character.toString(cp), style.withColor(colorizer.next())));
                }
            }
            return;
        }
        if (node.tag instanceof ColorTag color) {
            style = style.withColor(color.rgb());
            colorizer = null;
        } else if (node.tag instanceof DecorationTag decoration) {
            style = style.with(decoration.decoration(), decoration.value());
        } else if (node.tag instanceof GradientTag gradient) {
            colorizer = gradient(gradient, node.length());
        } else if (node.tag instanceof RainbowTag rainbow) {
            colorizer = rainbow(rainbow, node.length());
        }
        for (Node child : node.children) render(child, style, colorizer, out);
    }

    private static Colorizer gradient(GradientTag tag, int length) {
        int[] colors = tag.colors();
        int[] index = {0};
        return () -> {
            double t = length <= 1 ? 0 : (double) index[0]++ / (length - 1);
            double scaled = t * (colors.length - 1);
            int from = Math.min((int) scaled, colors.length - 2);
            return lerp(colors[from], colors[from + 1], scaled - from);
        };
    }

    private static Colorizer rainbow(RainbowTag tag, int length) {
        int[] index = {0};
        return () -> {
            int i = index[0]++;
            double hue = (length == 0 ? 0 : (double) (tag.reversed() ? length - 1 - i : i) / length) + tag.phase();
            return hue(hue - Math.floor(hue));
        };
    }

    /** Fully saturated, full brightness color of a hue from 0 to 1. */
    private static int hue(double hue) {
        double h = hue * 6;
        int sector = (int) h % 6;
        int rising = (int) Math.round((h - Math.floor(h)) * 255);
        int falling = 255 - rising;
        return switch (sector) {
            case 0 -> 0xFF0000 | rising << 8;
            case 1 -> falling << 16 | 0x00FF00;
            case 2 -> 0x00FF00 | rising;
            case 3 -> falling << 8 | 0x0000FF;
            case 4 -> rising << 16 | 0x0000FF;
            default -> 0xFF0000 | falling;
        };
    }

    private static int lerp(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Joins neighbouring spans with the same formatting. */
    private static List<Span> merge(List<Span> spans) {
        List<Span> merged = new ArrayList<>();
        for (Span span : spans) {
            if (span.text().isEmpty()) continue;
            int last = merged.size() - 1;
            if (last >= 0 && merged.get(last).style().equals(span.style())) {
                merged.set(last, new Span(merged.get(last).text() + span.text(), span.style()));
            } else {
                merged.add(span);
            }
        }
        return List.copyOf(merged);
    }
}
