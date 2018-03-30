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

import me.clip.placeholderapi.events.ExpansionUnregisterEvent;
import me.clip.placeholderapi.events.PlaceholderHookUnloadEvent;
import me.clip.placeholderapi.expansion.*;
import me.clip.placeholderapi.expansion.cloud.CloudExpansion;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class PlaceholderListener implements Listener {
	
	private PlaceholderAPIPlugin plugin;
	
	public PlaceholderListener(PlaceholderAPIPlugin instance) {
		plugin = instance;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}
	
	@EventHandler
	public void onExpansionUnregister(ExpansionUnregisterEvent event) {
		
		if (event.getExpansion() instanceof Listener) {
			HandlerList.unregisterAll((Listener)event.getExpansion());
		}
		
		if (event.getExpansion() instanceof Taskable) {
			((Taskable) event.getExpansion()).stop();
		}
		
		if (event.getExpansion() instanceof Cacheable) {
			((Cacheable) event.getExpansion()).clear();
		}
		
		if (plugin.getExpansionCloud() != null) {
			
			CloudExpansion ex = plugin.getExpansionCloud().getCloudExpansion(event.getExpansion().getName());
			
			if (ex != null) {
				ex.setHasExpansion(false);
				ex.setShouldUpdate(false);
			}
		}
	}
	
	@EventHandler
	public void onEnable(PluginEnableEvent event) {
		ExpansionManager m = plugin.getExpansionManager();
		PlaceholderExpansion e = m.getCachedExpansion(event.getPlugin().getName().toLowerCase());
		if (e != null && e.canRegister()) {
			if (e.isRegistered() || m.registerExpansion(e)) {
				m.removeCachedExpansion(e.getPlugin());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPluginUnload(PluginDisableEvent e) {
		
		String n = e.getPlugin().getName();
		
		if (n == null) {
			return;
		}
		
		if (n.equals(plugin.getName())) {
			return;
		}
			
		Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();
			
		for (Entry<String, PlaceholderHook> hook : hooks.entrySet()) {
				
			PlaceholderHook i = hook.getValue();
				
			if (i instanceof EZPlaceholderHook) {
					
				EZPlaceholderHook h = (EZPlaceholderHook) i;
				
				if (h.getPluginName() == null) {
					continue;
				}
					
				if (h.getPluginName().equalsIgnoreCase(n)) {
					if (PlaceholderAPI.unregisterPlaceholderHook(hook.getKey())) {
						plugin.getLogger().info("Unregistered placeholder hook for placeholder: " + h.getPlaceholderName());
						break;
					}
				}
			} else if (i instanceof PlaceholderExpansion) {
				
				PlaceholderExpansion ex = (PlaceholderExpansion) i;
				
				if (ex.getPlugin() == null) {
					continue;
				}
				
				if (ex.getPlugin().equalsIgnoreCase(n)) {
					if (PlaceholderAPI.unregisterExpansion(ex)) {
						plugin.getLogger().info("Unregistered placeholder expansion: " + ex.getIdentifier());
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {

	    Set<PlaceholderExpansion> expansions = PlaceholderAPI.getExpansions();

		if (expansions.isEmpty()) {
			return;
		}
		
		for (PlaceholderExpansion ex : expansions) {
			if (ex instanceof Cleanable) {
				((Cleanable) ex).cleanup(e.getPlayer());
			}
		}
	}
}
