package dev.sharpennd.guestpass.managers;

import dev.sharpennd.guestpass.GuestPass;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class GuestManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String GUESTS_FILE = "guests.txt";

    private final GuestPass plugin;
    private final Set<UUID> guests = new HashSet<>();

    private boolean systemEnabled;
    private LocalDateTime activateAt;
    private LocalDateTime deactivateAt;

    private boolean restrictBlockBreak;
    private boolean restrictBlockPlace;
    private boolean restrictBlockInteract;
    private boolean restrictEntityInteract;
    private boolean restrictAttack;
    private boolean restrictTakeDamage;
    private boolean restrictItemPickup;
    private boolean restrictXpPickup;
    private boolean restrictEntityMount;
    private boolean restrictMobTargeting;

    public GuestManager(GuestPass plugin) {
        this.plugin = plugin;
        loadConfig();
        loadGuests();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        systemEnabled = cfg.getBoolean("enabled", true);
        activateAt   = parseDate(cfg.getString("activate-at", ""));
        deactivateAt = parseDate(cfg.getString("deactivate-at", ""));

        restrictBlockBreak     = cfg.getBoolean("restrictions.block-break", true);
        restrictBlockPlace     = cfg.getBoolean("restrictions.block-place", true);
        restrictBlockInteract  = cfg.getBoolean("restrictions.block-interact", true);
        restrictEntityInteract = cfg.getBoolean("restrictions.entity-interact", true);
        restrictAttack         = cfg.getBoolean("restrictions.attack", true);
        restrictTakeDamage     = cfg.getBoolean("restrictions.take-damage", true);
        restrictItemPickup     = cfg.getBoolean("restrictions.item-pickup", true);
        restrictXpPickup       = cfg.getBoolean("restrictions.xp-pickup", true);
        restrictEntityMount    = cfg.getBoolean("restrictions.entity-mount", true);
        restrictMobTargeting   = cfg.getBoolean("restrictions.mob-targeting", true);
    }

    private LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw.trim(), FMT);
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid date format in config: '" + raw + "'. Use yyyy-MM-dd HH:mm");
            return null;
        }
    }

    private File guestsFile() {
        return new File(plugin.getDataFolder(), GUESTS_FILE);
    }

    private void loadGuests() {
        File f = guestsFile();
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try { guests.add(UUID.fromString(line)); } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load guests.txt: " + e.getMessage());
        }
    }

    private void saveGuests() {
        File f = guestsFile();
        plugin.getDataFolder().mkdirs();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
            for (UUID uuid : guests) pw.println(uuid.toString());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save guests.txt: " + e.getMessage());
        }
    }

    public boolean addGuest(UUID uuid) {
        boolean added = guests.add(uuid);
        if (added) saveGuests();
        return added;
    }

    public boolean removeGuest(UUID uuid) {
        boolean removed = guests.remove(uuid);
        if (removed) saveGuests();
        return removed;
    }

    public boolean isGuest(UUID uuid) {
        return guests.contains(uuid);
    }

    public Set<UUID> getGuests() {
        return Collections.unmodifiableSet(guests);
    }

    /** Returns true when the system is active right now (respects time window). */
    public boolean isActive() {
        if (!systemEnabled) return false;
        LocalDateTime now = LocalDateTime.now();
        boolean afterActivate    = (activateAt == null)   || !now.isBefore(activateAt);
        boolean beforeDeactivate = (deactivateAt == null) || now.isBefore(deactivateAt);
        return afterActivate && beforeDeactivate;
    }

    /** Returns true if this player should be restricted right now. */
    public boolean isRestricted(Player player) {
        if (!isActive()) return false;
        if (player.hasPermission("guestpass.bypass")) return false;
        return isGuest(player.getUniqueId());
    }

    public boolean restrictsBlockBreak()     { return restrictBlockBreak; }
    public boolean restrictsBlockPlace()     { return restrictBlockPlace; }
    public boolean restrictsBlockInteract()  { return restrictBlockInteract; }
    public boolean restrictsEntityInteract() { return restrictEntityInteract; }
    public boolean restrictsAttack()         { return restrictAttack; }
    public boolean restrictsTakeDamage()     { return restrictTakeDamage; }
    public boolean restrictsItemPickup()     { return restrictItemPickup; }
    public boolean restrictsXpPickup()       { return restrictXpPickup; }
    public boolean restrictsEntityMount()    { return restrictEntityMount; }
    public boolean restrictsMobTargeting()   { return restrictMobTargeting; }
    public boolean isSystemEnabled()         { return systemEnabled; }

    public String getTimeWindowDescription() {
        if (activateAt == null && deactivateAt == null) return "Always";
        String from = activateAt   != null ? activateAt.format(FMT)   : "now";
        String to   = deactivateAt != null ? deactivateAt.format(FMT) : "indefinitely";
        return from + " -> " + to;
    }
}
