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

public enum NMSVersion {

	UNKNOWN("unknown"),
	SPIGOT_1_7_R1("v1_7_R1"),
	SPIGOT_1_7_R2("v1_7_R2"),
	SPIGOT_1_7_R3("v1_7_R3"),
	SPIGOT_1_7_R4("v1_7_R4"),
	SPIGOT_1_8_R1("v1_8_R1"),
	SPIGOT_1_8_R2("v1_8_R2"),
	SPIGOT_1_8_R3("v1_8_R3"),
	SPIGOT_1_9_R1("v1_9_R1"),
	SPIGOT_1_9_R2("v1_9_R2"),
	SPIGOT_1_10_R1("v1_10_R1"),
	SPIGOT_1_11_R1("v1_11_R1"),
	SPIGOT_1_12_R1("v1_12_R1");
	
	private String version;
	
	NMSVersion(String version) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}
	
	public static NMSVersion getVersion(String version) {
		for (NMSVersion v : values()) {
			if (v.getVersion().equalsIgnoreCase(version)) {
				return v;
			}
		}
		return NMSVersion.UNKNOWN;
	}
}
