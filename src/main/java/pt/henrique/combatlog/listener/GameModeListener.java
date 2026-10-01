package pt.henrique.combatlog.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.i18n.LangManager;

/**
 * Blocks a combat-tagged player from switching into an invulnerable game mode.
 * <p>
 * The tag itself survives a game mode change — it is keyed by UUID and nothing
 * clears it — but CREATIVE and SPECTATOR make the player untouchable, so the
 * timer runs out while the opponent cannot reach them. That is combat escape
 * even though the tag is intact, so the switch is refused instead.
 * <p>
 * SURVIVAL and ADVENTURE are allowed: both stay vulnerable, so switching between
 * them changes nothing about the fight.
 * <p>
 * {@code PlayerGameModeChangeEvent} carries the packet-driven F3+F4 game mode
 * switcher ({@link PlayerGameModeChangeEvent.Cause#GAMEMODE_SWITCHER}) as well as
 * {@code /gamemode}, so both routes are covered. {@code HARDCORE_DEATH} and
 * {@code DEFAULT_GAMEMODE} are never refused: the first is the server forcing
 * spectator on a hardcore death, the second is world join, and cancelling either
 * would leave the player in a broken state.
 */
public class GameModeListener implements Listener {

    private final CombatManager manager;
    private final ConfigManager config;
    private final LangManager lang;

    public GameModeListener(CombatManager manager, ConfigManager config, LangManager lang) {
        this.manager = manager;
        this.config = config;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!config.isBlockGameModeChange()) return;

        GameMode target = event.getNewGameMode();
        if (target != GameMode.CREATIVE && target != GameMode.SPECTATOR) return;

        PlayerGameModeChangeEvent.Cause cause = event.getCause();
        if (cause == PlayerGameModeChangeEvent.Cause.HARDCORE_DEATH
                || cause == PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("combatlog.bypass")) return;
        if (!manager.isInCombat(player)) return;

        event.setCancelled(true);
        int remaining = manager.getRemainingSeconds(player.getUniqueId());
        lang.send(player, "combat.gamemode-blocked",
                new String[]{"gamemode", target.name()},
                new String[]{"time", String.valueOf(remaining)});
    }
}
