package pt.henrique.combatlog.combat;

import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Data class representing a single combat tag on a player.
 */
public class CombatTag {

    private final UUID playerId;
    private UUID opponentId;
    private long expiresAt; // System.currentTimeMillis() based
    private BossBar bossBar;

    public CombatTag(UUID playerId, @Nullable UUID opponentId, long durationMillis) {
        this.playerId = playerId;
        this.opponentId = opponentId;
        this.expiresAt = System.currentTimeMillis() + durationMillis;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    @Nullable
    public UUID getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(@Nullable UUID opponentId) {
        this.opponentId = opponentId;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void refresh(long durationMillis) {
        this.expiresAt = System.currentTimeMillis() + durationMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public long getRemainingMillis() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public int getRemainingSeconds() {
        return (int) Math.ceil(getRemainingMillis() / 1000.0);
    }

    public float getProgress(long totalDurationMillis) {
        if (totalDurationMillis <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (float) getRemainingMillis() / totalDurationMillis));
    }

    @Nullable
    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(@Nullable BossBar bossBar) {
        this.bossBar = bossBar;
    }
}

