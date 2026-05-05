package com.lastchance.downed.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class DownedText {
    private DownedText() {
    }

    public static Text prefix(Text message) {
        return Text.literal("[Last Chance] ").formatted(Formatting.RED).append(message);
    }

    public static Text seconds(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        return Text.literal(Long.toString(seconds));
    }
}
