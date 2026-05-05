package com.lastchance.downed.core.state;

import java.util.UUID;

public record DownedState(UUID uuid, long enteredAtMillis, long deadlineMillis, String lastDamageSource) {
    public long remainingMillis(long nowMillis) {
        return Math.max(0L, deadlineMillis - nowMillis);
    }
}
