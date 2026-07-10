package pt.henrique.combatlog.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when a player's combat tag expires naturally or is manually removed.
 */
public class PlayerCombatEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID lastOpponent;
    private final EndReason reason;

    public PlayerCombatEndEvent(@NotNull Player player, @Nullable UUID lastOpponent, @NotNull EndReason reason) {
        this.player = player;
        this.lastOpponent = lastOpponent;
        this.reason = reason;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public UUID getLastOpponent() {
        return lastOpponent;
    }

    @NotNull
    public EndReason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum EndReason {
        TIMER_EXPIRED,
        DEATH,
        MANUAL,
        PLUGIN_DISABLE
    }
}

