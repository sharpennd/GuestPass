package dev.sharpennd.guestpass.listeners;

import dev.sharpennd.guestpass.GuestPass;
import dev.sharpennd.guestpass.managers.GuestManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class GuestListener implements Listener {

    private final GuestPass plugin;

    public GuestListener(GuestPass plugin) {
        this.plugin = plugin;
    }

    private GuestManager mgr() {
        return plugin.getGuestManager();
    }

    private void notify(Player player) {
        String raw = plugin.getConfig().getString("messages.restricted", "&cYou cannot do that as a guest.");
        if (raw == null || raw.isBlank()) return;
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
    }

    // --- Block break ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (mgr().restrictsBlockBreak() && mgr().isRestricted(p)) {
            event.setCancelled(true);
            notify(p);
        }
    }

    // --- Block place ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (mgr().restrictsBlockPlace() && mgr().isRestricted(p)) {
            event.setCancelled(true);
            notify(p);
        }
    }

    // --- Block interact ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (mgr().restrictsBlockInteract() && mgr().isRestricted(p)) {
            // Only block physical block interactions (right-click on block / air with item)
            switch (event.getAction()) {
                case RIGHT_CLICK_BLOCK:
                case LEFT_CLICK_BLOCK:
                    event.setCancelled(true);
                    notify(p);
                    break;
                default:
                    break;
            }
        }
    }

    // --- Entity interact (right-click entities: villagers, item frames, etc.) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        if (mgr().restrictsEntityInteract() && mgr().isRestricted(p)) {
            event.setCancelled(true);
            notify(p);
        }
    }

    // --- Attack (player hitting entities) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            if (mgr().restrictsAttack() && mgr().isRestricted(p)) {
                event.setCancelled(true);
                notify(p);
                return;
            }
        }
        // Protect guests from taking damage
        if (event.getEntity() instanceof Player p) {
            if (mgr().restrictsTakeDamage() && mgr().isRestricted(p)) {
                event.setCancelled(true);
            }
        }
    }

    // --- General damage to guest (fall, fire, drowning, etc.) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return; // handled above
        if (event.getEntity() instanceof Player p) {
            if (mgr().restrictsTakeDamage() && mgr().isRestricted(p)) {
                event.setCancelled(true);
            }
        }
    }

    // --- Item pickup ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (mgr().restrictsItemPickup() && mgr().isRestricted(p)) {
                event.setCancelled(true);
            }
        }
    }

    // --- XP pickup ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onXpPickup(PlayerPickupExperienceEvent event) {
        Player p = event.getPlayer();
        if (mgr().restrictsXpPickup() && mgr().isRestricted(p)) {
            event.setCancelled(true);
        }
    }

    // --- Entity mount (entering vehicles/riding mobs) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player p) {
            if (mgr().restrictsEntityMount() && mgr().isRestricted(p)) {
                event.setCancelled(true);
                notify(p);
            }
        }
    }

    // --- Mob targeting guests ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player p) {
            if (mgr().restrictsMobTargeting() && mgr().isRestricted(p)) {
                // Only suppress mob-initiated targeting, not player-provoked
                if (event.getEntity() instanceof Monster) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
