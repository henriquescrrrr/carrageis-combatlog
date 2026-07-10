package pt.henrique.combatlog.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player enters combat or their combat timer is refreshed.
 */
public class PlayerCombatTagEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Player opponent;
    private final int durationSeconds;
    private final boolean refresh;
    private boolean cancelled;

    public PlayerCombatTagEvent(@NotNull Player player, @Nullable Player opponent, int durationSeconds, boolean refresh) {
        this.player = player;
        this.opponent = opponent;
        this.durationSeconds = durationSeconds;
        this.refresh = refresh;
    }

    /** The player being tagged. */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /** The opponent, may be null. */
    @Nullable
    public Player getOpponent() {
        return opponent;
    }

    /** Duration in seconds. */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /** Whether this is a refresh of an existing tag. */
    public boolean isRefresh() {
        return refresh;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
