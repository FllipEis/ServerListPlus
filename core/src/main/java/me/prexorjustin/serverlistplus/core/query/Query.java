package me.prexorjustin.serverlistplus.core.query;

import me.prexorjustin.serverlistplus.core.model.ServerMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed search query. All terms must match (AND).
 * <ul>
 *     <li>{@code word} – name, address, MOTD or notes contain the word</li>
 *     <li>{@code "two words"} – quoted phrase</li>
 *     <li>{@code tag:pvp} – a tag starts with "pvp"</li>
 *     <li>{@code folder:friends} – the name of one of its folders contains "friends"</li>
 *     <li>{@code note:base} – the notes contain "base"</li>
 *     <li>{@code has:notes}, {@code has:tags}, {@code has:folder} – servers with notes, tags or a folder</li>
 *     <li>{@code is:pinned} – servers pinned in any tab</li>
 *     <li>{@code is:hidden} – servers hidden from "All"; searching for it shows them there</li>
 *     <li>{@code is:online}, {@code is:offline} – the last ping reached the server or failed</li>
 *     <li>{@code ping:<100}, {@code players:>10} – compare ping or online players ({@code <}, {@code >}, {@code =})</li>
 *     <li>{@code played:>2h} – compares the total playtime, {@code played:0} for never played</li>
 *     <li>{@code last:<7d} – last joined less than 7 days ago; servers never joined don't match.
 *         Units: {@code min}, {@code h}, {@code d}, {@code w}, {@code mo}, {@code y}</li>
 *     <li>{@code domain:example.com} – the address is that domain or a subdomain of it;
 *         {@code *} matches anything, e.g. {@code domain:*.example.com} for subdomains only</li>
 *     <li>{@code -term} – negates any of the above</li>
 * </ul>
 * Matching ignores case and Minecraft formatting codes.
 */
public final class Query {
    public static final Query EMPTY = new Query(List.of());

    private final List<Term> terms;

    private Query(List<Term> terms) {
        this.terms = terms;
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    /** Whether the query asks for servers hidden from "All", which then must be shown there. */
    public boolean wantsHidden() {
        return terms.stream().anyMatch(term -> term.kind() == Term.Kind.HIDDEN && !term.negated());
    }

    /**
     * @param meta       extra data for the server, null if none exists
     * @param folderName resolves a folder id to its display name, null if unknown
     */
    public boolean matches(ServerRef server, ServerMeta meta, Function<String, String> folderName) {
        return matches(server, meta, folderName, System.currentTimeMillis());
    }

    /** @param now epoch millis that {@code last:} counts back from */
    public boolean matches(ServerRef server, ServerMeta meta, Function<String, String> folderName, long now) {
        for (Term term : terms) {
            if (term.matches(server, meta, folderName, now) == term.negated) return false;
        }
        return true;
    }

    public static Query parse(String input) {
        if (input == null || input.isBlank()) return EMPTY;
        List<Term> terms = new ArrayList<>();
        for (String token : tokenize(input)) {
            boolean negated = token.length() > 1 && token.startsWith("-");
            String body = negated ? token.substring(1) : token;
            Term term = Term.of(body, negated);
            if (term != null) terms.add(term);
        }
        return terms.isEmpty() ? EMPTY : new Query(List.copyOf(terms));
    }

    /** Splits on whitespace, keeping {@code "quoted phrases"} (and {@code tag:"a b"}) together. */
    static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }

    static String normalize(String text) {
        return PlainText.of(text);
    }

    private record Term(Kind kind, String value, boolean negated) {
        enum Kind {
            TEXT, TAG, FOLDER, NOTE, HAS_NOTES, HAS_TAGS, HAS_FOLDER, PINNED, HIDDEN, ONLINE, OFFLINE,
            PING, PLAYERS, PLAYED, LAST, DOMAIN
        }

        static Term of(String body, boolean negated) {
            int colon = body.indexOf(':');
            if (colon > 0) {
                String prefix = body.substring(0, colon).toLowerCase(Locale.ROOT);
                String value = normalize(body.substring(colon + 1));
                switch (prefix) {
                    case "tag" -> { return value.isEmpty() ? null : new Term(Kind.TAG, value, negated); }
                    case "folder" -> { return value.isEmpty() ? null : new Term(Kind.FOLDER, value, negated); }
                    case "note", "notes" -> { return value.isEmpty() ? null : new Term(Kind.NOTE, value, negated); }
                    case "has" -> {
                        switch (value) {
                            case "notes", "note" -> { return new Term(Kind.HAS_NOTES, "", negated); }
                            case "tags", "tag" -> { return new Term(Kind.HAS_TAGS, "", negated); }
                            case "folder", "folders" -> { return new Term(Kind.HAS_FOLDER, "", negated); }
                            default -> {
                            }
                        }
                    }
                    case "is" -> {
                        switch (value) {
                            case "pinned" -> { return new Term(Kind.PINNED, "", negated); }
                            case "hidden" -> { return new Term(Kind.HIDDEN, "", negated); }
                            case "online" -> { return new Term(Kind.ONLINE, "", negated); }
                            case "offline" -> { return new Term(Kind.OFFLINE, "", negated); }
                            default -> {
                            }
                        }
                    }
                    case "domain", "host" -> {
                        return value.isEmpty() ? null : new Term(Kind.DOMAIN, value, negated);
                    }
                    case "ping" -> {
                        if (Comparison.parse(value) != null) return new Term(Kind.PING, value, negated);
                    }
                    case "players" -> {
                        if (Comparison.parse(value) != null) return new Term(Kind.PLAYERS, value, negated);
                    }
                    case "played", "playtime" -> {
                        if (Comparison.parseDuration(value) != null) return new Term(Kind.PLAYED, value, negated);
                    }
                    case "last" -> {
                        if (Comparison.parseDuration(value) != null) return new Term(Kind.LAST, value, negated);
                    }
                    default -> {
                        // Not a filter (e.g. "host:25566"), search it as text
                    }
                }
            }
            String value = normalize(body);
            return value.isEmpty() ? null : new Term(Kind.TEXT, value, negated);
        }

        boolean matches(ServerRef server, ServerMeta meta, Function<String, String> folderName, long now) {
            return switch (kind) {
                case TEXT -> normalize(server.name()).contains(value)
                    || normalize(server.address()).contains(value)
                    || normalize(server.motd()).contains(value)
                    || meta != null && normalize(meta.notes()).contains(value);
                case TAG -> meta != null && meta.tags().stream().anyMatch(tag -> normalize(tag).startsWith(value));
                case FOLDER -> meta != null && meta.folderIds().stream()
                    .map(folderName)
                    .anyMatch(name -> name != null && normalize(name).contains(value));
                case NOTE -> meta != null && normalize(meta.notes()).contains(value);
                case HAS_NOTES -> meta != null && !meta.notes().isBlank();
                case HAS_TAGS -> meta != null && !meta.tags().isEmpty();
                case HAS_FOLDER -> meta != null && !meta.folderIds().isEmpty();
                case PINNED -> meta != null && meta.isPinnedAnywhere();
                case HIDDEN -> meta != null && meta.hiddenInAll();
                case ONLINE -> server.status() == ServerRef.Status.ONLINE;
                case OFFLINE -> server.status() == ServerRef.Status.OFFLINE;
                case PING -> server.ping() >= 0 && Comparison.parse(value).test(server.ping());
                case PLAYERS -> server.onlinePlayers() >= 0 && Comparison.parse(value).test(server.onlinePlayers());
                case PLAYED -> Comparison.parseDuration(value).test(server.playtimeMs());
                case LAST -> server.lastPlayed() > 0 && Comparison.parseDuration(value).test(now - server.lastPlayed());
                case DOMAIN -> domainMatches(host(server.address()), value);
            };
        }
    }

    /** The host part of an address: lower case, without port and trailing dot. */
    static String host(String address) {
        String value = normalize(address).trim();
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            return end < 0 ? value : value.substring(0, end + 1);
        }
        if (value.indexOf(':') == value.lastIndexOf(':')) {
            int colon = value.indexOf(':');
            if (colon >= 0) value = value.substring(0, colon);
        }
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value;
    }

    /** Without {@code *}: the domain itself or any subdomain. With {@code *}: a wildcard pattern for the whole host. */
    static boolean domainMatches(String host, String pattern) {
        if (pattern.contains("*")) {
            StringBuilder regex = new StringBuilder();
            for (String part : pattern.split("\\*", -1)) {
                if (!regex.isEmpty()) regex.append(".*");
                regex.append(java.util.regex.Pattern.quote(part));
            }
            return host.matches(regex.toString());
        }
        return host.equals(pattern) || host.endsWith("." + pattern);
    }

    /** {@code <100}, {@code >5}, {@code =0} or a bare number (same as {@code =}). */
    private record Comparison(char operator, long number) {
        private static final long MINUTE = 60_000;
        private static final long HOUR = 60 * MINUTE;
        private static final long DAY = 24 * HOUR;
        private static final Pattern DURATION = Pattern.compile("(\\d+)\\s*(min|mo|m|h|d|w|y)?");

        static Comparison parse(String value) {
            if (value.isEmpty()) return null;
            char first = value.charAt(0);
            char operator = first == '<' || first == '>' || first == '=' ? first : '=';
            String digits = operator == first ? value.substring(1) : value;
            try {
                return new Comparison(operator, Long.parseLong(digits.trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /** Like {@link #parse} with a duration such as {@code 2h} or {@code 7d}, in millis. A unit is required except for 0. */
        static Comparison parseDuration(String value) {
            if (value.isEmpty()) return null;
            char first = value.charAt(0);
            char operator = first == '<' || first == '>' || first == '=' ? first : '=';
            Matcher matcher = DURATION.matcher(operator == first ? value.substring(1).trim() : value);
            if (!matcher.matches()) return null;
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
            String unit = matcher.group(2);
            if (unit == null) return amount == 0 ? new Comparison(operator, 0) : null;
            long size = switch (unit) {
                case "m", "min" -> MINUTE;
                case "h" -> HOUR;
                case "d" -> DAY;
                case "w" -> 7 * DAY;
                case "mo" -> 30 * DAY;
                default -> 365 * DAY;
            };
            if (amount > Long.MAX_VALUE / size) return null;
            return new Comparison(operator, amount * size);
        }

        boolean test(long actual) {
            return switch (operator) {
                case '<' -> actual < number;
                case '>' -> actual > number;
                default -> actual == number;
            };
        }
    }
}
