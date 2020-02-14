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

        if (calcStateFlag(attacked, plugin.getPvPSpecialKnockbackFlag())) {
            double power = Math.sqrt(event.getDamage()) / 4;
            Vector knockback = attacked.getLocation().toVector()
                    .subtract(event.getDamager().getLocation().toVector())
                    .normalize().multiply(power * 2).add(new Vector(0, power, 0))
                    .add(attacking.getVelocity());

            new BukkitRunnable(){
                
                @Override
                public void run() {
                    attacked.setVelocity(knockback);
                }
            }.runTask(plugin);

            new BukkitRunnable() {
                long expire = System.currentTimeMillis() + 1000;

                @Override
                public void run() {
                    if (attacked.getVelocity().getY() < 0 || attacked.isDead()
                            || expire < System.currentTimeMillis()) {
                        cancel();
                        return;
                    }

                    attacked.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, attacked.getLocation(), 1);
                }
            }.runTaskTimer(plugin, 1, 1);
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                if (attacked.isDead()) {
                    attacked.setVelocity(new Vector());
                    if (calcStateFlag(attacked, plugin.getPvPAutoRespawnFlag())) {
                        attacked.spigot().respawn();
                    }
                }
            }
        }.runTask(plugin);
    }

    private boolean calcStateFlag(Player player, StateFlag flag) {
        RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
        ApplicableRegionSet applicableRegionSet = rm
                .getApplicableRegions(BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint());
        return applicableRegionSet.testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }
}