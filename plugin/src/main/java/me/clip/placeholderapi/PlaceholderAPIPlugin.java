/*
 *
 * PlaceholderAPI
 * Copyright (C) 2018 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package me.clip.placeholderapi;

import me.clip.placeholderapi.commands.PlaceholderAPICommands;
import me.clip.placeholderapi.configuration.PlaceholderAPIConfig;
import me.clip.placeholderapi.expansion.ExpansionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Version;
import me.clip.placeholderapi.expansion.cloud.ExpansionCloudManager;
import me.clip.placeholderapi.updatechecker.UpdateChecker;
import me.clip.placeholderapi.util.TimeUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Yes I have a shit load of work to do...
 * 
 * @author Ryan McCarthy
 *
 */
public class PlaceholderAPIPlugin extends JavaPlugin {

	private static PlaceholderAPIPlugin instance;

	private PlaceholderAPIConfig config;

	private ExpansionManager expansionManager;

	private ExpansionCloudManager expansionCloud;

	private static SimpleDateFormat dateFormat;

	private static String booleanTrue;

	private static String booleanFalse;

	private static Version serverVersion;

	private long startTime;

	@Override
	public void onLoad() {
		startTime = System.currentTimeMillis();
		instance = this;
		serverVersion = getVersion();
		config = new PlaceholderAPIConfig(this);
		expansionManager = new ExpansionManager(this);
	}

	@Override
	public void onEnable() {
		config.loadDefConfig();
		setupOptions();
		setupCommands();
		new PlaceholderListener(this);
		getLogger().info("Placeholder expansion registration initializing...");
		expansionManager.registerAllExpansions();
		if (config.checkUpdates()) {
			new UpdateChecker(this);
		}
		if (config.isCloudEnabled()) {
			enableCloud();
		}
		setupMetrics();
	}

	@Override
	public void onDisable() {
		disableCloud();
		PlaceholderAPI.unregisterAll();
		expansionManager.clean();
		expansionManager = null;
		Bukkit.getScheduler().cancelTasks(this);
		serverVersion = null;
		instance = null;
	}

	public void reloadConf(CommandSender s) {
		boolean cloudEnabled = this.expansionCloud != null;
		expansionManager.clean();
		PlaceholderAPI.unregisterAllProvidedExpansions();
		reloadConfig();
		setupOptions();
		expansionManager.registerAllExpansions();
		if (!config.isCloudEnabled()) {
			disableCloud();
		} else if (!cloudEnabled) {
			enableCloud();
		}
		s.sendMessage(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.getRegisteredIdentifiers().size() + " &aplaceholder hooks successfully registered!"));
	}

	private void setupCommands() {
		getCommand("placeholderapi").setExecutor(new PlaceholderAPICommands(this, (serverVersion != null && serverVersion.isSpigot())));
	}

	private void setupOptions() {
		booleanTrue = config.booleanTrue();
		if (booleanTrue == null) {
			booleanTrue = "true";
		}
		booleanFalse = config.booleanFalse();
		if (booleanFalse == null) {
			booleanFalse = "false";
		}
		try {
			dateFormat = new SimpleDateFormat(config.dateFormat());
		} catch (Exception e) {
			dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
		}
	}

	private void setupMetrics() {
		Metrics m = new Metrics(this);
	    m.addCustomChart(new Metrics.SimplePie("using_expansion_cloud", () -> getExpansionCloud() != null ? "yes" : "no"));
		
	    m.addCustomChart(new Metrics.SimplePie("using_spigot", () -> getServerVersion().isSpigot() ? "yes" : "no"));

	    m.addCustomChart(new Metrics.AdvancedPie("expansions_used", () -> {
            Map<String, Integer> map = new HashMap<>();
            Map<String, PlaceholderHook> p = PlaceholderAPI.getPlaceholders();

            if (!p.isEmpty()) {

                for (PlaceholderHook hook : p.values()) {
                    if (hook instanceof PlaceholderExpansion) {
                        PlaceholderExpansion ex = (PlaceholderExpansion) hook;
                        map.put(ex.getPlugin() == null ? ex.getIdentifier()
                                : ex.getPlugin(), 1);
                    }
                }
            }
            return map;

        }));

	}

	private static Version getVersion() {
		String v = "unknown";
		boolean spigot = false;
		try {
			v = Bukkit.getServer().getClass().getPackage().getName()
					.split("\\.")[3];
		} catch (ArrayIndexOutOfBoundsException ex) {
		}
		try {
			Class.forName("org.spigotmc.SpigotConfig");
			Class.forName("net.md_5.bungee.api.chat.BaseComponent");
			spigot = true;
		} catch (ExceptionInInitializerError | ClassNotFoundException exception) {
		}
		return new Version(v, spigot);
	}

	public void enableCloud() {
		if (expansionCloud == null) {
			expansionCloud = new ExpansionCloudManager(this);
			expansionCloud.fetch();
		} else {
			expansionCloud.clean();
			expansionCloud.fetch();
		}
	}

	public void disableCloud() {
		if (expansionCloud != null) {
			expansionCloud.clean();
			expansionCloud = null;
		}
	}

	/**
	 * Gets the static instance of the main class for PlaceholderAPI. This class
	 * is not the actual API class, this is the main class that extends
	 * JavaPlugin. For most API methods, use static methods available from the
	 * class: {@link PlaceholderAPI}
	 * 
	 * @return PlaceholderAPIPlugin instance
	 */
	public static PlaceholderAPIPlugin getInstance() {
		return instance;
	}

	/**
	 * Get the configurable {@linkplain SimpleDateFormat} object that is used to
	 * parse time for generic time based placeholders
	 * 
	 * @return date format
	 */
	public static SimpleDateFormat getDateFormat() {
		return dateFormat != null ? dateFormat : new SimpleDateFormat(
				"MM/dd/yy HH:mm:ss");
	}

	/**
	 * Get the configurable {@linkplain String} value that should be returned
	 * when a boolean is true
	 * 
	 * @return string value of true
	 */
	public static String booleanTrue() {
		return booleanTrue != null ? booleanTrue : "true";
	}

	/**
	 * Get the configurable {@linkplain String} value that should be returned
	 * when a boolean is false
	 * 
	 * @return string value of false
	 */
	public static String booleanFalse() {
		return booleanFalse != null ? booleanFalse : "false";
	}

	public static Version getServerVersion() {
		return serverVersion != null ? serverVersion : getVersion();
	}

	/**
	 * Obtain the configuration class for PlaceholderAPI.
	 * 
	 * @return PlaceholderAPIConfig instance
	 */
	public PlaceholderAPIConfig getPlaceholderAPIConfig() {
		return config;
	}

	public ExpansionManager getExpansionManager() {
		return expansionManager;
	}

	public ExpansionCloudManager getExpansionCloud() {
		return expansionCloud;
	}

	public String getUptime() {
		return TimeUtil.getTime((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
	}

	public long getUptimeMillis() {
		return (System.currentTimeMillis() - startTime);
	}
}
