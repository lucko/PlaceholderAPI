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
package me.clip.placeholderapi.expansion;

/**
 * This interface allows a class which extends a {@link PlaceholderExpansion}
 * to have the clear method called when the implementing expansion is unregistered
 * from PlaceholderAPI.
 * This is useful if we want to do things when the implementing hook is unregistered
 * @author Ryan McCarthy
 *
 */
public interface Cacheable {

	/**
	 * Called when the implementing class is unregistered from PlaceholderAPI
	 */
	void clear();
}
