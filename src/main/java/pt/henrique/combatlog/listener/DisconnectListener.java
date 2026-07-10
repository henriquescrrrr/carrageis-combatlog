package pt.henrique.combatlog.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import pt.henrique.combatlog.CombatLOG;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.combat.CombatTag;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.event.PlayerCombatEndEvent;
import pt.henrique.combatlog.event.PlayerCombatLogEvent;
import pt.henrique.combatlog.i18n.LangManager;
import pt.henrique.combatlog.util.ColorUtil;

/**
 * Handles disconnect punishment for combat-tagged players.
 */
public class DisconnectListener implements Listener {

    private final CombatLOG plugin;
    private final CombatManager manager;
    private final ConfigManager config;
    private final LangManager lang;

    public DisconnectListener(CombatLOG plugin, CombatManager manager, ConfigManager config, LangManager lang) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CombatTag tag = manager.getTag(player.getUniqueId());
        if (tag == null) return;

        // Fire combat log event
        PlayerCombatLogEvent logEvent = new PlayerCombatLogEvent(player, tag.getOpponentId());
        Bukkit.getPluginManager().callEvent(logEvent);

        // Remove the tag
        manager.removeTag(player.getUniqueId(), PlayerCombatEndEvent.EndReason.MANUAL);

        // Kill
        if (config.isPunishOnDisconnect()) {
            // setHealth(0) triggers a vanilla death which drops the inventory at the
            // death location. BUT if the world has keepInventory=true the death keeps
            // the items instead of dropping them — turning the punishment into a free
            // death with zero item loss (a trivial combat-log bypass: fight on a
            // keepInventory world, log out, keep everything). Force the item loss
            // ourselves in that case.
            //
            // This is dupe-safe: the player is disconnecting, so no GUI interaction or
            // reconnection can interleave within this tick; we drop each stack and then
            // clear the inventory in the same synchronous block, and vanilla will NOT
            // also drop them (keepInventory is on), so there is no double-drop.
            if (isKeepInventory(player)) {
                forceDropInventory(player);
            }
            player.setHealth(0);
        }

        // Broadcast — per-player language.
        // Names are sanitized: a Bedrock/Geyser or cracked-server name could contain
        // MiniMessage tags or legacy colour codes that would otherwise be interpreted
        // in a message broadcast to everyone (colour/obfuscation/interactive-tag injection).
        if (config.isBroadcastOnCombatLog()) {
            String opponentName = ColorUtil.stripTags(resolveOpponentName(tag));
            String playerName = ColorUtil.stripTags(player.getName());

            // Send to each online player in their language
            for (Player online : Bukkit.getOnlinePlayers()) {
                lang.send(online, "combat.log-broadcast",
                        new String[]{"player", playerName},
                        new String[]{"opponent", opponentName});
            }

            // Also log to console (global locale)
            plugin.getLogger().info(lang.get("combat.log-broadcast",
                    new String[]{"player", playerName},
                    new String[]{"opponent", opponentName}));
        }
    }

    /**
     * Whether the player's world keeps the inventory on death (so a vanilla death
     * would NOT drop items). Defaults to false if the gamerule can't be read.
     */
    private boolean isKeepInventory(Player player) {
        Boolean keep = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
        return keep != null && keep;
    }

    /**
     * Drop the player's full inventory (main + armor + off-hand) at their location
     * and clear it. Only used when keepInventory would otherwise suppress the drop.
     * ItemStacks are dropped as-is (they are about to be cleared) — the source is
     * cleared afterwards so nothing is duplicated.
     */
    private void forceDropInventory(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir()) {
                world.dropItemNaturally(loc, item);
            }
        }
        inv.clear();
    }

    private String resolveOpponentName(CombatTag tag) {
        if (tag.getOpponentId() == null) return lang.get("general.unknown");
        Player opponent = Bukkit.getPlayer(tag.getOpponentId());
        if (opponent != null) return opponent.getName();
        String name = Bukkit.getOfflinePlayer(tag.getOpponentId()).getName();
        return name != null ? name : lang.get("general.unknown");
    }
}
