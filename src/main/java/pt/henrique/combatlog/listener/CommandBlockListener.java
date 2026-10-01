package pt.henrique.combatlog.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.i18n.LangManager;
import pt.henrique.combatlog.util.ColorUtil;

/**
 * Blocks configured commands while a player is combat-tagged.
 * <p>
 * Matching is on the exact command label, not a prefix: {@code tpa} in the list
 * blocks {@code /tpa} but not {@code /tpaccept}, so every variant a server wants
 * blocked has to be listed. A namespaced label ({@code /essentials:tpa},
 * {@code /minecraft:tp}) is matched on both the full label and the part after
 * the colon, and the comparison is case-insensitive.
 * <p>
 * Known limit: a command <em>alias</em> is a distinct label and is matched only
 * if the alias itself is in the list. This is defence in depth on top of each
 * teleport plugin's own combat check, not a substitute for it.
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
                // baseCommand comes straight from the player's message and is
                // substituted into a template that is colour-parsed, so strip
                // control characters the same way player names are stripped.
                lang.send(player, "combat.command-blocked",
                        new String[]{"command", ColorUtil.stripTags(baseCommand)},
                        new String[]{"time", String.valueOf(remaining)});
                return;
            }
        }
    }
}

