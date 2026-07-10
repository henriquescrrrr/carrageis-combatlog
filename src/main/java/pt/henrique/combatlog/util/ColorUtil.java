package pt.henrique.combatlog.util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
/**
 * Utility for parsing color codes.
 * Supports legacy {@code &} codes, hex {@code &#RRGGBB}, and MiniMessage tags including gradients.
 */
public final class ColorUtil {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexCharacter('#')
                    .hexColors()
                    .build();
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private ColorUtil() {
    }
    /**
     * Parse a string that may contain legacy &amp; codes, hex codes, or MiniMessage tags.
     *
     * @param text the raw text
     * @return a parsed {@link Component}
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        // If it contains MiniMessage-style tags, parse with MiniMessage
        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI.deserialize(text);
            } catch (Exception ignored) {
                // fallback to legacy
            }
        }
        // Legacy & codes + hex &#RRGGBB
        return LEGACY.deserialize(text);
    }
    /**
     * Replace placeholders in text and parse colors.
     */
    public static Component parse(String text, String[][] placeholders) {
        if (text == null) return Component.empty();
        for (String[] pair : placeholders) {
            text = text.replace(pair[0], pair[1]);
        }
        return parse(text);
    }

    /**
     * Neutralize color/format control characters in an UNTRUSTED value (e.g. a player
     * name that may originate from Bedrock/Geyser or a cracked server) before it is
     * substituted into a message template that is later run through {@link #parse}.
     * <p>
     * Strips the legacy section sign ({@code §}) and ampersand ({@code &}) as well as
     * the MiniMessage tag delimiters ({@code <} / {@code >}), so a crafted name cannot
     * inject colours, obfuscation, or interactive {@code <click>}/{@code <hover>} tags
     * into broadcasts shown to everyone. Returns {@code ""} for null.
     */
    public static String stripTags(String value) {
        if (value == null) return "";
        return value.replace("§", "").replace('&', ' ')
                .replace('<', ' ').replace('>', ' ');
    }
}
