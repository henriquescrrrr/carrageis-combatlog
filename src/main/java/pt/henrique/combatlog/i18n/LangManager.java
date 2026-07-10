package pt.henrique.combatlog.i18n;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pt.henrique.combatlog.CombatLOG;
import pt.henrique.combatlog.hook.PlayerSettingsHook;
import pt.henrique.combatlog.util.ColorUtil;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages localized messages loaded from lang/ YAML files.
 * Supports multiple locales and per-player language via PlayerSettings.
 */
public class LangManager {

    private final CombatLOG plugin;
    private final PlayerSettingsHook settingsHook; // nullable
    private String globalLocale;
    private final Map<String, Map<String, String>> allLocales = new HashMap<>();

    public LangManager(CombatLOG plugin, PlayerSettingsHook settingsHook) {
        this.plugin = plugin;
        this.settingsHook = settingsHook;
    }

    /**
     * Load (or reload) all language files for the given global locale.
     */
    public void load(String globalLocale) {
        this.globalLocale = globalLocale;
        loadAllLocales();
    }

    /**
     * Reload all locale files from disk (uses the current global locale).
     */
    public void reload() {
        loadAllLocales();
    }

    // ─── Global-locale methods (backwards compatibility) ───

    /**
     * Get a raw message string for a key using the GLOBAL locale.
     * Kept for backwards compatibility — used by code that doesn't have a Player reference.
     */
    public String get(String key) {
        return getForLocale(globalLocale, key);
    }

    /**
     * Get a raw message string with replacements, using the GLOBAL locale.
     */
    public String get(String key, String[]... replacements) {
        return applyReplacements(get(key), replacements);
    }

    // ─── Per-player methods (NEW) ───

    /**
     * Get a raw message string for a specific player's locale.
     * Queries PlayerSettings first, falls back to global locale, then to pt_PT.
     */
    public String get(Player player, String key) {
        String locale = resolveLocale(player);
        return getForLocale(locale, key);
    }

    /**
     * Get a raw message string with replacements, for a specific player.
     */
    public String get(Player player, String key, String[]... replacements) {
        return applyReplacements(get(player, key), replacements);
    }

    /**
     * Send a parsed (colored) message to a player in their language.
     */
    public void send(Player player, String key, String[]... replacements) {
        String msg = get(player, key, replacements);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(ColorUtil.parse(msg));
        }
    }

    /**
     * Send a parsed message to a CommandSender.
     * If sender is Player, uses their locale. Otherwise uses global.
     */
    public void send(CommandSender sender, String key, String[]... replacements) {
        String msg;
        if (sender instanceof Player player) {
            msg = get(player, key, replacements);
        } else {
            msg = get(key, replacements);
        }
        if (msg != null && !msg.isEmpty()) {
            sender.sendMessage(ColorUtil.parse(msg));
        }
    }

    // ─── Internal ───

    /**
     * Resolve the locale for a player.
     * 1. Try PlayerSettingsHook
     * 2. Fall back to config global locale
     */
    private String resolveLocale(Player player) {
        if (settingsHook != null && settingsHook.isAvailable()) {
            String locale = settingsHook.getLanguage(player.getUniqueId());
            if (locale != null && allLocales.containsKey(locale)) {
                return locale;
            }
        }
        return globalLocale;
    }

    /** Default locale used as the ultimate fallback for missing keys. */
    private static final String DEFAULT_LOCALE = "pt_PT";

    /**
     * Get a message from a specific locale, falling back to pt_PT (the default),
     * then en_US, then the key itself.
     */
    private String getForLocale(String locale, String key) {
        Map<String, String> localeMap = allLocales.get(locale);
        if (localeMap != null && localeMap.containsKey(key)) {
            return localeMap.get(key);
        }
        // Fall back to the default locale (pt_PT).
        Map<String, String> defaults = allLocales.get(DEFAULT_LOCALE);
        if (defaults != null && defaults.containsKey(key)) {
            return defaults.get(key);
        }
        // Secondary fallback to en_US, so keys present only there still resolve.
        Map<String, String> english = allLocales.get("en_US");
        if (english != null) {
            return english.getOrDefault(key, key);
        }
        return key; // worst case: return the key itself
    }

    /**
     * Apply placeholder replacements.
     * replacements is String[][] like: {{"opponent", "Steve"}, {"time", "30"}}
     */
    private String applyReplacements(String msg, String[]... replacements) {
        if (msg == null) return "";
        for (String[] pair : replacements) {
            if (pair.length == 2) {
                msg = msg.replace("{" + pair[0] + "}", pair[1]);
            }
        }
        return msg;
    }

    /**
     * Load ALL .yml files from the lang/ folder into the allLocales map.
     */
    private void loadAllLocales() {
        allLocales.clear();

        // 1. Save defaults from JAR (if not already on disk)
        saveDefaultLang("en_US");
        saveDefaultLang("pt_PT");
        saveDefaultLang("fr_FR");

        // 2. Load ALL .yml files from lang/ folder
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String localeName = file.getName().replace(".yml", "");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Merge JAR defaults (add missing keys)
            InputStream defaultStream = plugin.getResource("lang/" + file.getName());
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                yaml.setDefaults(defaults);
            }

            // Flatten nested YAML into dot-notation keys
            Map<String, String> messages = new HashMap<>();
            for (String key : yaml.getKeys(true)) {
                if (yaml.isString(key)) {
                    messages.put(key, yaml.getString(key));
                }
            }

            allLocales.put(localeName, messages);
            plugin.getLogger().info("Loaded locale: " + localeName + " (" + messages.size() + " keys)");
        }

        // Warn if global locale is missing
        if (!allLocales.containsKey(globalLocale)) {
            plugin.getLogger().warning("Global locale '" + globalLocale + "' not found! Falling back to " + DEFAULT_LOCALE + ".");
        }
    }

    private void saveDefaultLang(String loc) {
        String path = "lang/" + loc + ".yml";
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
    }

    public String getLocale() {
        return globalLocale;
    }
}
