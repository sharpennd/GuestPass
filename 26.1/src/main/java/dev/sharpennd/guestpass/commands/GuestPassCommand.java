package dev.sharpennd.guestpass.commands;

import dev.sharpennd.guestpass.GuestPass;
import dev.sharpennd.guestpass.managers.GuestManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GuestPassCommand implements CommandExecutor, TabCompleter {

    private final GuestPass plugin;

    public GuestPassCommand(GuestPass plugin) {
        this.plugin = plugin;
    }

    private GuestManager mgr() {
        return plugin.getGuestManager();
    }

    private void msg(CommandSender sender, String key, String... replacements) {
        String raw = plugin.getConfig().getString("messages." + key, "");
        if (raw == null || raw.isBlank()) return;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', raw));
    }

    private void send(CommandSender sender, String raw) {
        if (raw == null || raw.isBlank()) return;
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', raw));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guestpass.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "add" -> {
                if (args.length < 2) { sendHelp(sender, label); return true; }
                String name = args[1];
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage("Player not found: " + name);
                    return true;
                }
                boolean added = mgr().addGuest(target.getUniqueId());
                if (added) msg(sender, "added", "{player}", target.getName());
                else       msg(sender, "already-guest", "{player}", target.getName());
            }

            case "remove" -> {
                if (args.length < 2) { sendHelp(sender, label); return true; }
                String name = args[1];
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                boolean removed = mgr().removeGuest(target.getUniqueId());
                if (removed) msg(sender, "removed", "{player}", target.getName() != null ? target.getName() : name);
                else         msg(sender, "not-guest", "{player}", target.getName() != null ? target.getName() : name);
            }

            case "list" -> {
                Set<UUID> guests = mgr().getGuests();
                if (guests.isEmpty()) {
                    sender.sendMessage("No players in guest pass mode.");
                    return true;
                }
                sender.sendMessage("Guest list (" + guests.size() + "):");
                for (UUID uuid : guests) {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String display = (op.getName() != null) ? op.getName() : uuid.toString();
                    boolean online = op.isOnline();
                    send(sender, "  &7" + display + (online ? " &a(online)" : " &8(offline)"));
                }
            }

            case "reload" -> {
                mgr().loadConfig();
                msg(sender, "reloaded");
            }

            case "status" -> {
                msg(sender, "status-header");
                msg(sender, "status-enabled",          "{value}", String.valueOf(mgr().isSystemEnabled()));
                msg(sender, "status-time-window",      "{value}", mgr().getTimeWindowDescription());
                msg(sender, "status-currently-active", "{value}", String.valueOf(mgr().isActive()));

                long onlineGuests = mgr().getGuests().stream()
                        .filter(u -> Bukkit.getPlayer(u) != null)
                        .count();
                msg(sender, "status-guest-count", "{value}", String.valueOf(onlineGuests));
            }

            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        send(sender, "&6GuestPass Commands:");
        send(sender, "&e/" + label + " add <player>    &7- Add a player to GuestPass");
        send(sender, "&e/" + label + " remove <player> &7- Remove a player from GuestPass");
        send(sender, "&e/" + label + " list            &7- List all guests");
        send(sender, "&e/" + label + " status          &7- Show current status");
        send(sender, "&e/" + label + " reload          &7- Reload config");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("guestpass.admin")) return List.of();

        if (args.length == 1) {
            return List.of("add", "remove", "list", "reload", "status").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            String partial = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("remove")) {
                // Suggest current guests who are online
                return mgr().getGuests().stream()
                        .map(u -> {
                            Player p = Bukkit.getPlayer(u);
                            return p != null ? p.getName() : null;
                        })
                        .filter(n -> n != null && n.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
