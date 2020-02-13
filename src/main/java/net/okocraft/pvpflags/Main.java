package net.okocraft.pvpflags;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	private static Main instance;

	private StateFlag pvpItemDamageFlag;
    private StateFlag pvpSpecialKnockbackFlag;
    private StateFlag pvpAutoRespawnFlag;
	
	@Override
	public void onLoad() {
		pvpItemDamageFlag = registerStateFlag("pvp-item-damage", true);
		pvpSpecialKnockbackFlag = registerStateFlag("pvp-special-knockback", false);
		pvpAutoRespawnFlag = registerStateFlag("pvp-auto-respawn", false);
	}

	private StateFlag registerStateFlag(String name, boolean def) {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			StateFlag flag = new StateFlag(name, def);
			registry.register(flag);
			return flag;
		} catch (FlagConflictException e) {
			// some other plugin registered a flag by the same name already.
			// you can use the existing flag, but this may cause conflicts - be sure to check type
			Flag<?> existing = registry.get(name);
			if (existing instanceof StateFlag) {
				return (StateFlag) existing;
			} else {
				// types don't match - this is bad news! some other plugin conflicts with you
				// hopefully this never actually happens
				Bukkit.getPluginManager().disablePlugin(this);
				return null;
			}
		}
	}

	@Override
	public void onEnable() {
		PvPListener.start();
	}

	@Override
	public void onDisable() {
		PvPListener.stop();
	};

	/**
	 * Gets plugin instance.
	 * 
	 * @return Instance of DeathMessages plugin.
	 * @throws IllegalStateException If this plugin is not enabled.
	 */
	public static Main getInstance() throws IllegalStateException {
		if (instance == null) {
			instance = (Main) Bukkit.getPluginManager().getPlugin("PvPFlags");
			if (instance == null) {
				throw new IllegalStateException("Plugin is not enabled!");
			}
		}
		return instance;
	}
	
	/**
	 * @return the pvpItemDamageFlag
	 */
	public StateFlag getPvPItemDamageFlag() {
		return pvpItemDamageFlag;
	}

	/**
	 * @return the pvpSpecialKnockbackFlag
	 */
	public StateFlag getPvPSpecialKnockbackFlag() {
		return pvpSpecialKnockbackFlag;
	}

	/**
	 * @return the pvpAutoRespawnFlag
	 */
	public StateFlag getPvPAutoRespawnFlag() {
		return pvpAutoRespawnFlag;
	}
}
