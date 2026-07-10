package pt.henrique.combatlog.listener;
import org.bukkit.GameMode;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.config.ConfigManager;
/**
 * Detects PvP hits and tags both players.
 */
public class CombatListener implements Listener {
    private final CombatManager manager;
    private final ConfigManager config;
    public CombatListener(CombatManager manager, ConfigManager config) {
        this.manager = manager;
        this.config = config;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        // Same player
        if (attacker.equals(victim)) return;
        // Creative check
        if (!config.isApplyInCreative()) {
            if (attacker.getGameMode() == GameMode.CREATIVE || victim.getGameMode() == GameMode.CREATIVE) return;
        }
        // World check
        String world = victim.getWorld().getName();
        if (config.getIgnoreWorlds().contains(world)) return;
        manager.tagFromPvP(attacker, victim);
    }
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        // Direct melee.
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        // Projectiles (arrows, tridents, snowballs, ender pearls, thrown splash/lingering
        // damage potions, fishing hook, etc. — all extend Projectile). The shooter is the
        // real attacker.
        if (config.isApplyToProjectiles() && event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }
        // Tamed pets (wolf, cat, etc.). The damager is the animal; credit its owner so
        // "sic your wolf on them and log out" is not a free bypass.
        if (config.isApplyToPets() && event.getDamager() instanceof Tameable tameable && tameable.isTamed()) {
            AnimalTamer owner = tameable.getOwner();
            if (owner instanceof Player ownerPlayer) {
                return ownerPlayer;
            }
        }
        return null;
    }
}
