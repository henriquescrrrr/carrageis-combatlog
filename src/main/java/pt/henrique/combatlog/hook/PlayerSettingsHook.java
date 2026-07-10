package pt.henrique.combatlog.hook;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflection-based hook into PlayerSettings for per-player language
 * and per-player preferences (show-bossbar, show-titles, chat-countdown).
 */
public class PlayerSettingsHook {

    private Object settingsProvider;
    private Method getLanguageMethod;
    private Method getBooleanMethod;
    private Method registerCategoryMethod;
    private Method registerToggleMethod;
    private Method unregisterAllMethod;
    private boolean available;

    public void init(Logger logger) {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlayerSettings") == null) {
                available = false;
                logger.info("PlayerSettings not found — using global language.");
                return;
            }
            Class<?> apiClass = Class.forName("pt.henrique.playersettings.api.PlayerSettingsApi");
            var reg = Bukkit.getServicesManager().getRegistration(apiClass);
            if (reg != null && reg.getProvider() != null) {
                settingsProvider = reg.getProvider();
                getLanguageMethod = settingsProvider.getClass().getMethod("getLanguage", UUID.class);
                available = true;
                logger.info("PlayerSettings hook enabled — per-player language active.");

                // Try to get preferences API methods
                try {
                    getBooleanMethod = settingsProvider.getClass().getMethod("getBoolean", UUID.class, String.class, boolean.class);
                    registerCategoryMethod = settingsProvider.getClass().getMethod("registerCategory",
                            String.class, Material.class, int.class);
                    registerToggleMethod = settingsProvider.getClass().getMethod("registerToggle",
                            String.class, String.class, boolean.class, Material.class, int.class);
                    unregisterAllMethod = settingsProvider.getClass().getMethod("unregisterAll", String.class);
                } catch (NoSuchMethodException e) {
                    logger.info("PlayerSettings preferences API not available — using defaults.");
                }
            } else {
                available = false;
                logger.info("PlayerSettings API not registered — using global language.");
            }
        } catch (Exception e) {
            available = false;
            logger.info("PlayerSettings hook failed — using global language.");
        }
    }

    /**
     * Register CombatLOG settings in the PlayerSettings preferences hub.
     */
    public void registerSettings() {
        if (!available || registerCategoryMethod == null) return;
        try {
            registerCategoryMethod.invoke(settingsProvider, "combatlog", Material.IRON_SWORD, 30);
            registerToggleMethod.invoke(settingsProvider, "combatlog", "show-bossbar", true, Material.DRAGON_BREATH, 1);
            registerToggleMethod.invoke(settingsProvider, "combatlog", "show-titles", true, Material.NAME_TAG, 2);
            registerToggleMethod.invoke(settingsProvider, "combatlog", "chat-countdown", false, Material.CLOCK, 3);
        } catch (Exception ignored) {}
    }

    /**
     * Unregister all CombatLOG settings from PlayerSettings.
     */
    public void unregisterSettings() {
        if (!available || unregisterAllMethod == null) return;
        try {
            unregisterAllMethod.invoke(settingsProvider, "combatlog");
        } catch (Exception ignored) {}
    }

    /**
     * Read a boolean preference for a player.
     *
     * @param uuid         the player's UUID
     * @param fullKey      the full setting key (e.g. "combatlog.show-bossbar")
     * @param defaultValue value to return when PlayerSettings is unavailable or the key is unset
     * @return the player's preference, or defaultValue
     */
    public boolean getSetting(UUID uuid, String fullKey, boolean defaultValue) {
        if (!available || getBooleanMethod == null) return defaultValue;
        try {
            return (boolean) getBooleanMethod.invoke(settingsProvider, uuid, fullKey, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get the language locale for a player via PlayerSettings.
     *
     * @param uuid the player's UUID
     * @return the locale string (e.g. "pt_PT"), or null if unavailable
     */
    public String getLanguage(UUID uuid) {
        if (!available) return null;
        try {
            return (String) getLanguageMethod.invoke(settingsProvider, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isAvailable() {
        return available;
    }
}

