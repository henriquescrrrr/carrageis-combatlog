# CombatLOG

A professional PvP combat logging prevention plugin for **Paper/Purpur 1.21+** servers.

When two players engage in PvP, both are "combat tagged". While tagged, they cannot safely disconnect. If they disconnect before the timer ends, the system automatically kills them and broadcasts a message.

## Features

**Combat Tagging** — Both players are tagged on PvP hits (melee + projectiles)
- ⏱️ **Configurable Timer** — Default 30s, refreshes on every hit
**Disconnect Punishment** — Auto-kill + broadcast on combat log
**Boss Bar Countdown** — Real-time visual countdown with configurable colors
**Chat Notifications** — Tag start, tag end, periodic countdown
**Command Blocking** — Block `/tpa`, `/home`, etc. while in combat
**Localization** — English (en_US) and Portuguese (pt_PT) included
**Developer API** — Clean API via ServicesManager + Custom Events
**Highly Configurable** — Every feature can be toggled and customized

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/combatlog reload` | Reload configuration | `combatlog.admin` |
| `/combatlog status` | Check your combat status | — |
| `/combatlog version` | Show plugin version | — |

**Aliases:** `/clog`, `/cl`

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `combatlog.admin` | Reload the plugin | op |
| `combatlog.bypass` | Bypass combat tagging | false |

## Configuration

### config.yml

```yaml
language: en_US

combat:
  tag-seconds: 30
  apply-to-projectiles: true
  apply-in-creative: false
  ignore-worlds: []

punishment:
  kill-on-disconnect: true
  broadcast: true

bossbar:
  enabled: true
  color: RED          # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
  overlay: PROGRESS   # PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20
  title: "&c⚔ Combat &7| &f{time}s"
  update-ticks: 2

chat:
  tag-start: true
  tag-end: true
  countdown:
    enabled: false
    interval-seconds: 5

blocked-commands:
  enabled: true
  list:
    - tpa
    - tpaccept
    - spawn
    - home
```

### Language Files

Located in `plugins/CombatLOG/lang/`. Supports:
- Legacy `&` color codes
- Hex colors `&#RRGGBB`
- MiniMessage tags (gradients, etc.)

**Placeholders:** `{player}`, `{opponent}`, `{time}`, `{command}`

---

## Developer API

### Getting the API

```java
import pt.henrique.combatlog.api.CombatLogApi;
import org.bukkit.Bukkit;

CombatLogApi api = Bukkit.getServicesManager().load(CombatLogApi.class);
```

### API Methods

```java
public interface CombatLogApi {
    boolean isInCombat(UUID playerId);
    boolean isInCombat(Player player);
    long getRemainingMillis(UUID playerId);
    int getRemainingSeconds(UUID playerId);
    UUID getLastOpponent(UUID playerId);     // nullable
    void tag(Player player, Player opponent, int seconds);
    void untag(Player player);
}
```

### Custom Events

| Event | When |
|-------|------|
| `PlayerCombatTagEvent` | Player enters combat or timer refreshes (cancellable) |
| `PlayerCombatEndEvent` | Combat ends (timer expired, death, manual) |
| `PlayerCombatLogEvent` | Player disconnects while tagged (punishment applied) |

### Integration Example: Block /tpa in combat

```java
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pt.henrique.combatlog.api.CombatLogApi;

public class TpaBlocker implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().substring(1).split("\\s+")[0].toLowerCase();
        if (!cmd.equals("tpa")) return;

        CombatLogApi api = Bukkit.getServicesManager().load(CombatLogApi.class);
        if (api != null && api.isInCombat(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot use /tpa while in combat!");
        }
    }
}
```

### Event Listener Example

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pt.henrique.combatlog.event.PlayerCombatTagEvent;
import pt.henrique.combatlog.event.PlayerCombatEndEvent;

public class CombatListener implements Listener {

    @EventHandler
    public void onCombatTag(PlayerCombatTagEvent event) {
        // event.getPlayer(), event.getOpponent(), event.isRefresh()
        // event.setCancelled(true) to prevent tagging
    }

    @EventHandler
    public void onCombatEnd(PlayerCombatEndEvent event) {
        // event.getPlayer(), event.getReason()
        // EndReason: TIMER_EXPIRED, DEATH, MANUAL, PLUGIN_DISABLE
    }
}
```

## Building

```bash
mvn clean package
```

The compiled JAR will be in `target/`.

## Requirements

- **Java 21+**
- **Paper or Purpur 1.21+**

## License

Licensed under the **GNU General Public License v3.0** (GPL-3.0). See the [LICENSE](LICENSE) file for details.

