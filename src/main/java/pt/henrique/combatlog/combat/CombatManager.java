package pt.henrique.combatlog.combat;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pt.henrique.combatlog.CombatLOG;
import pt.henrique.combatlog.api.CombatLogApi;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.event.PlayerCombatEndEvent;
import pt.henrique.combatlog.event.PlayerCombatTagEvent;
import pt.henrique.combatlog.hook.PlayerSettingsHook;
import pt.henrique.combatlog.i18n.LangManager;
import pt.henrique.combatlog.util.ColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all combat tags. Also implements the public API.
 */
public class CombatManager implements CombatLogApi {

    private final CombatLOG plugin;
    private final ConfigManager config;
    private final LangManager lang;
    private final PlayerSettingsHook settingsHook;
    private final Map<UUID, CombatTag> tags = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    // Track last chat countdown second sent per player (to avoid spam).
    // Concurrent because getTag() (reachable from the public API off the main
    // thread) can mutate this map while the main-thread tick iterates it.
    private final Map<UUID, Integer> lastChatCountdown = new ConcurrentHashMap<>();

    public CombatManager(CombatLOG plugin, ConfigManager config, LangManager lang, PlayerSettingsHook settingsHook) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.settingsHook = settingsHook;
    }

    // ─── Lifecycle ───

    public void start() {
        int interval = Math.max(1, config.getBossBarUpdateTicks());
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        // Remove all boss bars
        for (CombatTag tag : tags.values()) {
            removeBossBar(tag);
        }
        tags.clear();
        lastChatCountdown.clear();
    }

    // ─── Tag Logic ───

    /**
     * Tag a player from PvP hit.
     * Players with the {@code combatlog.bypass} permission are never tagged
     * (as advertised in plugin.yml). Bypass is evaluated per-player: an exempt
     * attacker does not get tagged, but a non-exempt victim they hit still does.
     */
    public void tagFromPvP(@NotNull Player player, @NotNull Player opponent) {
        if (!player.hasPermission("combatlog.bypass")) {
            tagPlayer(player, opponent, config.getTagSeconds());
        }
        if (!opponent.hasPermission("combatlog.bypass")) {
            tagPlayer(opponent, player, config.getTagSeconds());
        }
    }

    private void tagPlayer(@NotNull Player player, @Nullable Player opponent, int seconds) {
        UUID pid = player.getUniqueId();
        UUID oid = opponent != null ? opponent.getUniqueId() : null;
        boolean refresh = tags.containsKey(pid);

        // Fire event
        PlayerCombatTagEvent event = new PlayerCombatTagEvent(player, opponent, seconds, refresh);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        CombatTag existing = tags.get(pid);
        if (existing != null) {
            existing.refresh(seconds * 1000L);
            existing.setOpponentId(oid);
        } else {
            CombatTag tag = new CombatTag(pid, oid, seconds * 1000L);
            tags.put(pid, tag);

            // Send tag-start chat message.
            // Sanitize the opponent name: it may be attacker-controlled (Bedrock/Geyser
            // or cracked names) and is substituted into a message that is colour-parsed.
            if (config.isChatTagStartEnabled()) {
                String opponentName = opponent != null
                        ? ColorUtil.stripTags(opponent.getName())
                        : lang.get(player, "general.unknown");
                lang.send(player, "combat.tag-start",
                        new String[]{"opponent", opponentName},
                        new String[]{"time", String.valueOf(seconds)});
            }

            // Show title on FIRST tag (not refreshes)
            if (config.isTitleOnTagStart()
                    && settingsHook.getSetting(player.getUniqueId(), "combatlog.show-titles", true)) {
                String title = lang.get(player, "combat.tag-title");
                String subtitle = lang.get(player, "combat.tag-subtitle");
                if (title != null && !title.isEmpty()) {
                    player.showTitle(Title.title(
                            ColorUtil.parse(title),
                            ColorUtil.parse(subtitle != null ? subtitle : ""),
                            Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))
                    ));
                }
            }
        }

        // Ensure boss bar exists (if player wants it)
        if (config.isBossBarEnabled()
                && settingsHook.getSetting(player.getUniqueId(), "combatlog.show-bossbar", true)) {
            CombatTag tag = tags.get(pid);
            if (tag != null && tag.getBossBar() == null) {
                createBossBar(player, tag);
            }
        }
    }

    // ─── Tick (single repeating task) ───

    private void tick() {
        Iterator<Map.Entry<UUID, CombatTag>> it = tags.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CombatTag> entry = it.next();
            UUID pid = entry.getKey();
            CombatTag tag = entry.getValue();

            if (tag.isExpired()) {
                Player player = Bukkit.getPlayer(pid);
                if (player != null && player.isOnline()) {
                    // Fire end event
                    Bukkit.getPluginManager().callEvent(
                            new PlayerCombatEndEvent(player, tag.getOpponentId(), PlayerCombatEndEvent.EndReason.TIMER_EXPIRED));

                    if (config.isChatTagEndEnabled()) {
                        lang.send(player, "combat.tag-end");
                    }

                    // Show title when combat ends naturally
                    if (config.isTitleOnTagEnd()
                            && settingsHook.getSetting(player.getUniqueId(), "combatlog.show-titles", true)) {
                        String title = lang.get(player, "combat.end-title");
                        String subtitle = lang.get(player, "combat.end-subtitle");
                        if (title != null && !title.isEmpty()) {
                            player.showTitle(Title.title(
                                    ColorUtil.parse(title),
                                    ColorUtil.parse(subtitle != null ? subtitle : ""),
                                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofMillis(500))
                            ));
                        }
                    }
                }
                removeBossBar(tag);
                lastChatCountdown.remove(pid);
                it.remove();
                continue;
            }

            // Update boss bar
            Player player = Bukkit.getPlayer(pid);
            if (player != null && player.isOnline()) {
                updateBossBar(player, tag);
                handleChatCountdown(player, tag);
            }
        }
    }

    // ─── Boss Bar ───

    private void createBossBar(Player player, CombatTag tag) {
        String title = formatBossBarTitle(player, tag);
        BossBar bar = BossBar.bossBar(
                ColorUtil.parse(title),
                tag.getProgress(config.getTagMillis()),
                config.getBossBarColor(),
                config.getBossBarOverlay()
        );
        tag.setBossBar(bar);
        player.showBossBar(bar);
    }

    private void updateBossBar(Player player, CombatTag tag) {
        BossBar bar = tag.getBossBar();
        if (bar == null) return;
        bar.name(ColorUtil.parse(formatBossBarTitle(player, tag)));
        bar.progress(tag.getProgress(config.getTagMillis()));
    }

    private void removeBossBar(CombatTag tag) {
        BossBar bar = tag.getBossBar();
        if (bar == null) return;
        Player player = Bukkit.getPlayer(tag.getPlayerId());
        if (player != null && player.isOnline()) {
            player.hideBossBar(bar);
        }
        tag.setBossBar(null);
    }

    /**
     * Format the BossBar title for a player.
     * Checks lang file first for per-player locale, falls back to config.yml.
     */
    private String formatBossBarTitle(Player player, CombatTag tag) {
        String bossBarTitle = lang.get(player, "bossbar.title");
        if (bossBarTitle == null || bossBarTitle.equals("bossbar.title")) {
            // Key not found in lang — fall back to config.yml value
            bossBarTitle = config.getBossBarTitle();
        }
        return bossBarTitle.replace("{time}", String.valueOf(tag.getRemainingSeconds()));
    }

    // ─── Chat Countdown ───

    private void handleChatCountdown(Player player, CombatTag tag) {
        // Per-player setting; falls back to config value when PlayerSettings is unavailable
        boolean showCountdown = settingsHook.getSetting(
                player.getUniqueId(), "combatlog.chat-countdown", config.isChatCountdownEnabled());
        if (!showCountdown) return;
        int remaining = tag.getRemainingSeconds();
        int interval = config.getChatCountdownIntervalSeconds();
        if (interval <= 0) return;

        Integer lastSent = lastChatCountdown.get(player.getUniqueId());
        // Send message at multiples of interval (e.g., 25, 20, 15, 10, 5)
        if (remaining > 0 && remaining % interval == 0 && (lastSent == null || lastSent != remaining)) {
            lastChatCountdown.put(player.getUniqueId(), remaining);
            lang.send(player, "combat.countdown",
                    new String[]{"time", String.valueOf(remaining)});
        }
    }

    // ─── Removal helpers ───

    /**
     * Called when a player dies – clears their tag.
     */
    public void removeOnDeath(@NotNull Player player) {
        CombatTag tag = tags.remove(player.getUniqueId());
        if (tag != null) {
            removeBossBar(tag);
            lastChatCountdown.remove(player.getUniqueId());
            Bukkit.getPluginManager().callEvent(
                    new PlayerCombatEndEvent(player, tag.getOpponentId(), PlayerCombatEndEvent.EndReason.DEATH));
        }
    }

    /**
     * Get the tag for a player (or null if not tagged / already expired).
     * <p>
     * This is reachable from the public API, which consumers commonly call from
     * async tasks (scoreboards, placeholders, chat). It is therefore READ-ONLY and
     * thread-safe: an expired tag returns {@code null} but its removal (map cleanup
     * and boss-bar hide, which must run on the main thread) is left to the repeating
     * {@link #tick()} task, which reclaims it within a couple of ticks. Doing the
     * mutation here would touch player boss bars off the main thread.
     */
    @Nullable
    public CombatTag getTag(@NotNull UUID playerId) {
        CombatTag tag = tags.get(playerId);
        if (tag != null && tag.isExpired()) {
            return null;
        }
        return tag;
    }

    /**
     * Remove tag for a player with a given reason. Returns the removed tag or null.
     */
    @Nullable
    public CombatTag removeTag(@NotNull UUID playerId, @NotNull PlayerCombatEndEvent.EndReason reason) {
        CombatTag tag = tags.remove(playerId);
        if (tag != null) {
            removeBossBar(tag);
            lastChatCountdown.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                Bukkit.getPluginManager().callEvent(new PlayerCombatEndEvent(player, tag.getOpponentId(), reason));
            }
        }
        return tag;
    }

    public Map<UUID, CombatTag> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    // ─── CombatLogApi implementation ───

    @Override
    public boolean isInCombat(@NotNull UUID playerId) {
        return getTag(playerId) != null;
    }

    @Override
    public boolean isInCombat(@NotNull Player player) {
        return isInCombat(player.getUniqueId());
    }

    @Override
    public long getRemainingMillis(@NotNull UUID playerId) {
        CombatTag tag = getTag(playerId);
        return tag != null ? tag.getRemainingMillis() : 0;
    }

    @Override
    public int getRemainingSeconds(@NotNull UUID playerId) {
        CombatTag tag = getTag(playerId);
        return tag != null ? tag.getRemainingSeconds() : 0;
    }

    @Override
    @Nullable
    public UUID getLastOpponent(@NotNull UUID playerId) {
        CombatTag tag = getTag(playerId);
        return tag != null ? tag.getOpponentId() : null;
    }

    @Override
    public void tag(@NotNull Player player, @Nullable Player opponent, int seconds) {
        tagPlayer(player, opponent, seconds);
    }

    @Override
    public void untag(@NotNull Player player) {
        CombatTag tag = removeTag(player.getUniqueId(), PlayerCombatEndEvent.EndReason.MANUAL);
        if (tag != null && config.isChatTagEndEnabled()) {
            lang.send(player, "combat.tag-end");
        }
    }
}

