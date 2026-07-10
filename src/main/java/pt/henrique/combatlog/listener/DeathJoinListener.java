package pt.henrique.combatlog.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import pt.henrique.combatlog.combat.CombatManager;

/**
 * Clears combat tag on death and performs join cleanup.
 */
public class DeathJoinListener implements Listener {

    private final CombatManager manager;

    public DeathJoinListener(CombatManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        manager.removeOnDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // Cleanup any stale tags that might remain for this UUID
        // (shouldn't happen, but safety net)
        manager.removeTag(event.getPlayer().getUniqueId(),
                pt.henrique.combatlog.event.PlayerCombatEndEvent.EndReason.MANUAL);
    }
}

