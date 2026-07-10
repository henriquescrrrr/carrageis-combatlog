package pt.henrique.combatlog.config;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import pt.henrique.combatlog.CombatLOG;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed access to config.yml values.
 */
public class ConfigManager {

    private final CombatLOG plugin;

    // Combat
    private int tagSeconds;
    private boolean applyToProjectiles;
    private boolean applyToPets;
    private boolean applyInCreative;
    private List<String> ignoreWorlds;

    // Punishment
    private boolean punishOnDisconnect;
    private boolean broadcastOnCombatLog;

    // BossBar
    private boolean bossBarEnabled;
    private BossBar.Color bossBarColor;
    private BossBar.Overlay bossBarOverlay;
    private String bossBarTitle;
    private int bossBarUpdateTicks;

    // Titles
    private boolean titleOnTagStart;
    private boolean titleOnTagEnd;

    // Chat
    private boolean chatTagStartEnabled;
    private boolean chatTagEndEnabled;
    private boolean chatCountdownEnabled;
    private int chatCountdownIntervalSeconds;

    // Blocked commands
    private boolean blockedCommandsEnabled;
    private List<String> blockedCommands;

    // Language
    private String language;

    public ConfigManager(CombatLOG plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();

        try {
            plugin.reloadConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse config.yml! Please fix the YAML syntax.");
            plugin.getLogger().severe("Error: " + e.getMessage());
            plugin.getLogger().severe("Tip: Delete plugins/CombatLOG/config.yml to regenerate the default config.");
            plugin.getLogger().warning("Loading default values...");
            applyDefaults();
            return;
        }

        FileConfiguration c = plugin.getConfig();

        tagSeconds = c.getInt("combat.tag-seconds", 30);
        applyToProjectiles = c.getBoolean("combat.apply-to-projectiles", true);
        applyToPets = c.getBoolean("combat.apply-to-pets", true);
        applyInCreative = c.getBoolean("combat.apply-in-creative", false);
        ignoreWorlds = c.getStringList("combat.ignore-worlds");

        punishOnDisconnect = c.getBoolean("punishment.kill-on-disconnect", true);
        broadcastOnCombatLog = c.getBoolean("punishment.broadcast", true);

        bossBarEnabled = c.getBoolean("bossbar.enabled", true);
        bossBarColor = parseBossBarColor(c.getString("bossbar.color", "RED"));
        bossBarOverlay = parseBossBarOverlay(c.getString("bossbar.overlay", "PROGRESS"));
        bossBarTitle = c.getString("bossbar.title", "&c⚔ Combat &7| &f{time}s");
        bossBarUpdateTicks = c.getInt("bossbar.update-ticks", 2);

        titleOnTagStart = c.getBoolean("titles.on-tag-start", true);
        titleOnTagEnd = c.getBoolean("titles.on-tag-end", true);

        chatTagStartEnabled = c.getBoolean("chat.tag-start", true);
        chatTagEndEnabled = c.getBoolean("chat.tag-end", true);
        chatCountdownEnabled = c.getBoolean("chat.countdown.enabled", false);
        chatCountdownIntervalSeconds = c.getInt("chat.countdown.interval-seconds", 5);

        blockedCommandsEnabled = c.getBoolean("blocked-commands.enabled", true);
        blockedCommands = new ArrayList<>();
        for (String cmd : c.getStringList("blocked-commands.list")) {
            blockedCommands.add(cmd.toLowerCase());
        }

        language = c.getString("language", "pt_PT");
    }

    /**
     * Apply safe defaults when config.yml cannot be parsed.
     */
    private void applyDefaults() {
        tagSeconds = 30;
        applyToProjectiles = true;
        applyToPets = true;
        applyInCreative = false;
        ignoreWorlds = new ArrayList<>();
        punishOnDisconnect = true;
        broadcastOnCombatLog = true;
        bossBarEnabled = true;
        bossBarColor = BossBar.Color.RED;
        bossBarOverlay = BossBar.Overlay.PROGRESS;
        bossBarTitle = "&c⚔ Combat &7| &f{time}s";
        bossBarUpdateTicks = 2;
        titleOnTagStart = true;
        titleOnTagEnd = true;
        chatTagStartEnabled = true;
        chatTagEndEnabled = true;
        chatCountdownEnabled = false;
        chatCountdownIntervalSeconds = 5;
        blockedCommandsEnabled = true;
        blockedCommands = new ArrayList<>();
        language = "pt_PT";
    }

    // ─── Getters ───

    public int getTagSeconds() { return tagSeconds; }
    public long getTagMillis() { return tagSeconds * 1000L; }
    public boolean isApplyToProjectiles() { return applyToProjectiles; }
    public boolean isApplyToPets() { return applyToPets; }
    public boolean isApplyInCreative() { return applyInCreative; }
    public List<String> getIgnoreWorlds() { return ignoreWorlds; }

    public boolean isPunishOnDisconnect() { return punishOnDisconnect; }
    public boolean isBroadcastOnCombatLog() { return broadcastOnCombatLog; }

    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public BossBar.Color getBossBarColor() { return bossBarColor; }
    public BossBar.Overlay getBossBarOverlay() { return bossBarOverlay; }
    public String getBossBarTitle() { return bossBarTitle; }
    public int getBossBarUpdateTicks() { return bossBarUpdateTicks; }

    public boolean isTitleOnTagStart() { return titleOnTagStart; }
    public boolean isTitleOnTagEnd() { return titleOnTagEnd; }

    public boolean isChatTagStartEnabled() { return chatTagStartEnabled; }
    public boolean isChatTagEndEnabled() { return chatTagEndEnabled; }
    public boolean isChatCountdownEnabled() { return chatCountdownEnabled; }
    public int getChatCountdownIntervalSeconds() { return chatCountdownIntervalSeconds; }

    public boolean isBlockedCommandsEnabled() { return blockedCommandsEnabled; }
    public List<String> getBlockedCommands() { return blockedCommands; }

    public String getLanguage() { return language; }

    // ─── Helpers ───

    private BossBar.Color parseBossBarColor(String s) {
        try {
            return BossBar.Color.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseBossBarOverlay(String s) {
        try {
            return BossBar.Overlay.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}

