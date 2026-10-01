package pt.henrique.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import pt.henrique.combatlog.api.CombatLogApi;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.command.CombatLogCommand;
import pt.henrique.combatlog.config.ConfigManager;
import pt.henrique.combatlog.hook.PlayerSettingsHook;
import pt.henrique.combatlog.i18n.LangManager;
import pt.henrique.combatlog.listener.CombatListener;
import pt.henrique.combatlog.listener.CommandBlockListener;
import pt.henrique.combatlog.listener.CushionListener;
import pt.henrique.combatlog.listener.DeathJoinListener;
import pt.henrique.combatlog.listener.DisconnectListener;
import pt.henrique.combatlog.listener.GameModeListener;

public final class CombatLOG extends JavaPlugin {

    private ConfigManager configManager;
    private LangManager langManager;
    private CombatManager combatManager;
    private PlayerSettingsHook settingsHook;

    @Override
    public void onEnable() {
        // Config
        configManager = new ConfigManager(this);
        configManager.load();

        // PlayerSettings hook (per-player language + preferences)
        settingsHook = new PlayerSettingsHook();
        settingsHook.init(getLogger());
        settingsHook.registerSettings();

        // Localization — multi-locale + per-player
        langManager = new LangManager(this, settingsHook);
        langManager.load(configManager.getLanguage());

        // Combat Manager
        combatManager = new CombatManager(this, configManager, langManager, settingsHook);
        combatManager.start();

        // Register API via ServicesManager
        Bukkit.getServicesManager().register(CombatLogApi.class, combatManager, this, ServicePriority.Normal);

        // Listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new CombatListener(combatManager, configManager), this);
        pm.registerEvents(new DisconnectListener(this, combatManager, configManager, langManager), this);
        pm.registerEvents(new DeathJoinListener(combatManager), this);
        pm.registerEvents(new CommandBlockListener(combatManager, configManager, langManager), this);
        pm.registerEvents(new GameModeListener(combatManager, configManager, langManager), this);
        pm.registerEvents(new CushionListener(combatManager, configManager, langManager), this);

        // Command
        var cmd = getCommand("combatlog");
        if (cmd != null) {
            CombatLogCommand executor = new CombatLogCommand(this, combatManager, langManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("CombatLOG v" + getPluginMeta().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.shutdown();
        }
        if (settingsHook != null) {
            settingsHook.unregisterSettings();
        }
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("CombatLOG disabled.");
    }

    /**
     * Reload config and language files (called by /combatlog reload).
     * <p>
     * Active combat tags are preserved. Dropping them here would make
     * {@code /combatlog reload} a way to free everyone currently fighting,
     * which an op could use mid-fight and which would silently skip the
     * {@code PlayerCombatEndEvent} consumers depend on.
     */
    public void reload() {
        configManager.load();
        langManager.load(configManager.getLanguage());

        combatManager.restartTicker();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public PlayerSettingsHook getSettingsHook() {
        return settingsHook;
    }
}
