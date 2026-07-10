package pt.henrique.combatlog.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.i18n.LangManager;

/**
 * Blocks configured commands while a player is combat-tagged.
 */
public class CommandBlockListener implements Listener {

    private final CombatManager manager;
    private final ConfigManager config;
    private final LangManager lang;

    public CommandBlockListener(CombatManager manager, ConfigManager config, LangManager lang) {
        this.manager = manager;
        this.config = config;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!config.isBlockedCommandsEnabled()) return;

        Player player = event.getPlayer();
        if (!manager.isInCombat(player)) return;

        // Parse the base command (strip /)
        String message = event.getMessage().substring(1).trim();
        String baseCommand = message.split("\\s+")[0].toLowerCase();

        // Also check with plugin prefix (e.g., "essentials:tpa" → "tpa")
        String stripped = baseCommand.contains(":") ? baseCommand.split(":", 2)[1] : baseCommand;

        for (String blocked : config.getBlockedCommands()) {
            if (stripped.equalsIgnoreCase(blocked) || baseCommand.equalsIgnoreCase(blocked)) {
                event.setCancelled(true);
                int remaining = manager.getRemainingSeconds(player.getUniqueId());
                lang.send(player, "combat.command-blocked",
                        new String[]{"command", baseCommand},
                        new String[]{"time", String.valueOf(remaining)});
                return;
            }
        }
    }
}

