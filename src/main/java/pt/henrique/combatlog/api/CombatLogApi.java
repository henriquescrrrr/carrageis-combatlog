package pt.henrique.combatlog.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Public API for CombatLOG.
 * <p>
 * Other plugins can obtain this through Bukkit's {@link org.bukkit.Bukkit#getServicesManager()}:
 * <pre>{@code
 * CombatLogApi api = Bukkit.getServicesManager().load(CombatLogApi.class);
 * if (api != null && api.isInCombat(player)) {
 *     // player is in combat
 * }
 * }</pre>
 */
public interface CombatLogApi {

    /**
     * Check if a player is currently combat tagged.
     *
     * @param playerId the player's UUID
     * @return true if the player is in combat
     */
    boolean isInCombat(@NotNull UUID playerId);

    /**
     * Check if a player is currently combat tagged.
     *
     * @param player the player
     * @return true if the player is in combat
     */
    boolean isInCombat(@NotNull Player player);

    /**
     * Get the remaining combat time in milliseconds.
     *
     * @param playerId the player's UUID
     * @return remaining millis, or 0 if not in combat
     */
    long getRemainingMillis(@NotNull UUID playerId);

    /**
     * Get the remaining combat time in seconds (rounded up).
     *
     * @param playerId the player's UUID
     * @return remaining seconds, or 0 if not in combat
     */
    int getRemainingSeconds(@NotNull UUID playerId);

    /**
     * Get the UUID of the last opponent that this player fought.
     *
     * @param playerId the player's UUID
     * @return the opponent's UUID, or null if not in combat / unknown
     */
    @Nullable
    UUID getLastOpponent(@NotNull UUID playerId);

    /**
     * Manually tag a player as in combat.
     *
     * @param player   the player to tag
     * @param opponent the opponent (nullable)
     * @param seconds  duration in seconds
     */
    void tag(@NotNull Player player, @Nullable Player opponent, int seconds);

    /**
     * Manually remove a player's combat tag.
     *
     * @param player the player to untag
     */
    void untag(@NotNull Player player);
}

