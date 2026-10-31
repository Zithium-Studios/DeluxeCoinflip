package net.zithium.deluxecoinflip.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyFormat;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class ColorUtil {


    private static final LegacyComponentSerializer LEGACY_HEX_SERIALIZER = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private static final char[] LEGACY_COLOR_CODES = {
            // Colors
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F',
            // Decorations
            'k', 'l', 'm', 'n', 'o', 'r', 'K', 'L', 'M', 'N', 'O', 'R'
    };

    private static final Pattern LEGACY_RGB_PATTERN = Pattern.compile(
            "[§&]x[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])");

    private static final Pattern LEGACY_PLUGIN_RGB_PATTERN = Pattern.compile(
            "[§&]#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final String RGB_REPLACEMENT = "<#$1$2$3$4$5$6>";


    public static String color(String message) {
        Component component = MiniMessage.miniMessage().deserialize(replaceLegacyWithTags(message.replace('\u00A7', '&')));
        return LEGACY_HEX_SERIALIZER.serialize(component);
    }

    /**
     * Replaces legacy formatting codes in the input string with corresponding HTML tags or values.
     * Legacy formatting codes are denoted by '&' or '§' followed by a character representing a color or decoration.
     * The replacement is performed according to the mapping defined in the LegacyComponentSerializer.
     *
     * @param input the input string containing legacy formatting codes
     * @return the input string with legacy formatting codes replaced by HTML tags or values
     */
    public static String replaceLegacyWithTags(String input) {
        // Replace legacy RGB color codes with the RGB_REPLACEMENT string
        String output = LEGACY_RGB_PATTERN.matcher(input).replaceAll(RGB_REPLACEMENT);
        // Replace legacy plugin RGB color codes with the RGB_REPLACEMENT string
        output = LEGACY_PLUGIN_RGB_PATTERN.matcher(output).replaceAll(RGB_REPLACEMENT);

        // Iterate over each legacy color code
        for (char legacyCode : LEGACY_COLOR_CODES) {
            // Parse the legacy formatting code using LegacyComponentSerializer
            LegacyFormat format = LegacyComponentSerializer.parseChar(legacyCode);
            // If the format is null, skip to the next code
            if (format == null) continue;

            // If the format specifies a color, replace the legacy code with corresponding HTML color tag
            if (format.color() != null)
                output = output.replaceAll("[&§]" + legacyCode, "<" + format.color().asHexString() + ">");

            // If the format specifies a decoration, replace the legacy code with corresponding HTML decoration tag
            if (format.decoration() != null)
                output = output.replaceAll("[&§]" + legacyCode, "<" + format.decoration().name() + ">");

            // If the format specifies a reset, replace the legacy code with "<reset>"
            if (format.reset())
                output = output.replaceAll("[&§]" + legacyCode, "<reset>");
        }

        // Return the output string with legacy formatting codes replaced by HTML tags or values
        return output;
    }


    public static Component componentColor(String component) {
        return MiniMessage.miniMessage().deserialize(replaceLegacy(component));
    }

    private static String replaceLegacy(String legacyText) {
        return legacyText
                .replaceAll("&1", "<dark_blue>")
                .replaceAll("&2", "<dark_green>")
                .replaceAll("&3", "<dark_aqua>")
                .replaceAll("&4", "<dark_red>")
                .replaceAll("&5", "<dark_purple>")
                .replaceAll("&6", "<gold>")
                .replaceAll("&7", "<gray>")
                .replaceAll("&8", "<dark_gray>")
                .replaceAll("&9", "<blue>")
                .replaceAll("&a", "<green>")
                .replaceAll("&b", "<aqua>")
                .replaceAll("&c", "<red>")
                .replaceAll("&d", "<light_purple>")
                .replaceAll("&e", "<yellow>")
                .replaceAll("&f", "<white>")
                .replaceAll("&l", "<bold>")
                .replaceAll("&k", "<obfuscated>")
                .replaceAll("&m", "<strikethrough>")
                .replaceAll("&n", "<underline>")
                .replaceAll("&r", "<reset>")
                .replaceAll("&o", "<italic>");
    }
}
