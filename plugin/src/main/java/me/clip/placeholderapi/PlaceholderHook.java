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

import org.bukkit.entity.Player;

public abstract class PlaceholderHook {

	/**
	 * called when a placeholder is requested from this PlaceholderHook
	 * @param p Player requesting the placeholder value for, null if not needed for a player
	 * @param params String passed for the placeholder hook to determine what value to return
	 * @return value for the requested player and params
	 */
	public abstract String onPlaceholderRequest(Player p, String params);
}
