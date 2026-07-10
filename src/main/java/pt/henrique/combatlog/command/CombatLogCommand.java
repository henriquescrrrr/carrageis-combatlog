package pt.henrique.combatlog.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pt.henrique.combatlog.CombatLOG;
import pt.henrique.combatlog.combat.CombatManager;
import pt.henrique.combatlog.i18n.LangManager;

import java.util.ArrayList;
import java.util.List;

/**
 * /combatlog command — admin & info.
 */
public class CombatLogCommand implements CommandExecutor, TabCompleter {

    private final CombatLOG plugin;
    private final CombatManager manager;
    private final LangManager lang;

    public CombatLogCommand(CombatLOG plugin, CombatManager manager, LangManager lang) {
        this.plugin = plugin;
        this.manager = manager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            lang.send(sender, "command.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("combatlog.admin")) {
                    lang.send(sender, "command.no-permission");
                    return true;
                }
                plugin.reload();
                lang.send(sender, "command.reloaded");
            }
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    lang.send(sender, "command.player-only");
                    return true;
                }
                if (manager.isInCombat(player)) {
                    int remaining = manager.getRemainingSeconds(player.getUniqueId());
                    lang.send(player, "command.status-in-combat",
                            new String[]{"time", String.valueOf(remaining)});
                } else {
                    lang.send(player, "command.status-safe");
                }
            }
            case "version" -> {
                lang.send(sender, "command.version",
                        new String[]{"version", plugin.getPluginMeta().getVersion()});
            }
            default -> lang.send(sender, "command.usage");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("status");
            completions.add("version");
            if (sender.hasPermission("combatlog.admin")) {
                completions.add("reload");
            }
            completions.removeIf(c -> !c.toLowerCase().startsWith(args[0].toLowerCase()));
        }
        return completions;
    }
}
