package pt.henrique.combatlog.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when a combat-tagged player disconnects and is punished (killed).
 */
public class PlayerCombatLogEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID opponent;

    public PlayerCombatLogEvent(@NotNull Player player, @Nullable UUID opponent) {
        this.player = player;
        this.opponent = opponent;
    }

    /** The player who disconnected while in combat. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** The last known opponent UUID (may be null). */
    @Nullable
    public UUID getOpponent() {
        return opponent;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

