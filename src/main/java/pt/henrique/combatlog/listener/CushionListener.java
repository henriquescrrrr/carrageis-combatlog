package pt.henrique.combatlog.listener;

import org.bukkit.entity.Cushion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.event.PlayerCombatTagEvent;
import pt.henrique.combatlog.i18n.LangManager;

/**
 * Keeps tagged players off cushions (Minecraft 26.3).
 * <p>
 * A player sitting on a cushion is immune to knockback, pistons and portals, so in a
 * fight it is a free anchor: the opponent cannot push them off a ledge or out of
 * reach. While tagged the player cannot sit down, and a player who is tagged while
 * already seated is stood up.
 */
public class CushionListener implements Listener {

    private final CombatManager manager;
    private final ConfigManager config;
    private final LangManager lang;

    public CushionListener(CombatManager manager, ConfigManager config, LangManager lang) {
        this.manager = manager;
        this.config = config;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!config.isBlockCushionSitting()) return;
        if (!(event.getMount() instanceof Cushion)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.hasPermission("combatlog.bypass")) return;
        if (!manager.isInCombat(player)) return;

        event.setCancelled(true);
        int remaining = manager.getRemainingSeconds(player.getUniqueId());
        lang.send(player, "combat.cushion-blocked",
                new String[]{"time", String.valueOf(remaining)});
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTag(PlayerCombatTagEvent event) {
        if (!config.isBlockCushionSitting()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("combatlog.bypass")) return;
        if (player.getVehicle() instanceof Cushion) {
            player.leaveVehicle();
        }
    }
}
