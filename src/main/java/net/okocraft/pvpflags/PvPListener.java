package net.okocraft.pvpflags;

import java.util.HashSet;
import java.util.Set;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PvPListener implements Listener {

    private static Main plugin = Main.getInstance();
    private static RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    private static PvPListener instance;

    private Set<Player> inPvPPlayers = new HashSet<>();

    private PvPListener() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void start() {
        if (!isRunning()) {
            instance = new PvPListener();
        }
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static void stop() {
        if (isRunning()) {
            HandlerList.unregisterAll(instance);
            instance = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvPItemDamage(PlayerItemDamageEvent event) {
        if (inPvPPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
            inPvPPlayers.remove(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player)) {
            return;
        }

        Player attacked = (Player) event.getEntity();
        Player attacking = (Player) event.getDamager();

        if (!calcStateFlag(attacking, plugin.getPvPItemDamageFlag())) {
            inPvPPlayers.add(attacking);
        }

        if (!calcStateFlag(attacked, plugin.getPvPItemDamageFlag())) {
            return;
        }

        
        event.setCancelled(true);
        attacked.damage(event.getDamage());

        Vector knockback = attacked.getLocation().toVector().subtract(event.getDamager().getLocation().toVector());
        double power = Math.sqrt(event.getDamage() * 0.75);

        knockback = knockback.normalize().multiply(power).add(new Vector(0, power / 3, 0));
        attacked.setVelocity(knockback);

        new BukkitRunnable(){
        
            @Override
            public void run() {
                if (attacked.isDead() && calcStateFlag(attacked, plugin.getPvPAutoRespawnFlag())) {
                    attacked.spigot().respawn();
                }
            }
        }.runTaskLater(plugin, 0);

        new BukkitRunnable() {
            long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                attacked.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, attacked.getLocation(), 1);

                if (attacked.getVelocity().getY() < 0 || attacked.isDead()
                        || startTime + 1000 < System.currentTimeMillis()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private boolean calcStateFlag(Player player, StateFlag flag) {
        RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
        ApplicableRegionSet applicableRegionSet = rm.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint());
        return applicableRegionSet.testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }
}