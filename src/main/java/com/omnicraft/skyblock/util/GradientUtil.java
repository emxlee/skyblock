package com.omnicraft.skyblock.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small self-contained gradient text helper. Supports two entry points:
 *
 *  1. gradient(text, startHex, endHex) -- applies a gradient across the
 *     whole string, character by character.
 *
 *  2. parse(text) -- scans for an inline <gradient:#AAAAAA:#BBBBBB>...</gradient>
 *     tag (used in config.yml strings like the GUI title) and applies it,
 *     leaving any surrounding plain text untouched. Only one gradient tag
 *     per string is supported, which covers our config use cases without
 *     pulling in a full MiniMessage dependency.
 */
public final class GradientUtil {

    private static final Pattern TAG_PATTERN = Pattern.compile(
            "<gradient:(#?[0-9a-fA-F]{6}):(#?[0-9a-fA-F]{6})>(.*?)</gradient>"
    );

    private GradientUtil() {
    }

    public static Component gradient(String text, String startHex, String endHex) {
        TextColor start = TextColor.fromHexString(normalize(startHex));
        TextColor end = TextColor.fromHexString(normalize(endHex));

        if (text.isEmpty() || start == null || end == null) {
            return Component.text(text);
        }

        Component result = Component.empty();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float progress = length == 1 ? 0f : (float) i / (float) (length - 1);
            TextColor color = TextColor.lerp(progress, start, end);
            result = result.append(Component.text(String.valueOf(text.charAt(i)), Style.style(color)));
        }

        return result;
    }

    /**
     * Parses a string that may contain a single <gradient:#xxxxxx:#yyyyyy>...</gradient>
     * tag. Text outside the tag is rendered plain. If no tag is found, the
     * whole string is returned as plain text.
     */
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }

        Matcher matcher = TAG_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return Component.text(raw);
        }

        String before = raw.substring(0, matcher.start());
        String inner = matcher.group(3);
        String after = raw.substring(matcher.end());

        Component result = Component.empty();
        if (!before.isEmpty()) result = result.append(Component.text(before));
        result = result.append(gradient(inner, matcher.group(1), matcher.group(2)));
        if (!after.isEmpty()) result = result.append(Component.text(after));

        return result;
    }

    private static String normalize(String hex) {
        return hex.startsWith("#") ? hex : "#" + hex;
    }
}
