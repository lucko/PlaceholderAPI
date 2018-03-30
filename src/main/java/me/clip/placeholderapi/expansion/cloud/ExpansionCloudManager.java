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
package me.clip.placeholderapi.expansion.cloud;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExpansionCloudManager {

	private PlaceholderAPIPlugin plugin;
	private final String API = "http://api.extendedclip.com/";
	private final File dir;
	private final TreeMap<Integer, CloudExpansion> remote = new TreeMap<>();
	private final List<String> downloading = new ArrayList<>();

	public ExpansionCloudManager(PlaceholderAPIPlugin instance) {
		plugin = instance;
		dir = new File(instance.getDataFolder() + File.separator + "expansions");
		if (!dir.exists()) {
			try {
				dir.mkdirs();
			} catch (Exception ex) {
            ex.printStackTrace();
        }
		}
	}
	
	public void clean() {
		remote.clear();
		downloading.clear();
	}
	
	public boolean isDownloading(String expansion) {
	    return downloading.contains(expansion);
	}
	
	public Map<Integer, CloudExpansion> getCloudExpansions() {
	    return remote;
	}
	
	public CloudExpansion getCloudExpansion(String name) {
		return remote.values().stream().filter(ex -> ex.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	public int getCloudAuthorCount() {
        return remote.values().stream().collect(Collectors.groupingBy(CloudExpansion::getAuthor, Collectors.counting())).size();
	}
	
	public long getToUpdateCount() {
        return PlaceholderAPI.getExpansions().stream().filter(ex -> getCloudExpansion(ex.getName()) != null && getCloudExpansion(ex.getName()).shouldUpdate()).count();
    }

	public Map<Integer, CloudExpansion> getAllByAuthor(String author) {
		if (remote.isEmpty()) {
			return null;
		}
		TreeMap<Integer, CloudExpansion> byAuthor = new TreeMap<>();
		boolean first = true;
		for (CloudExpansion ex : remote.values()) {
			if (ex.getAuthor().equalsIgnoreCase(author)) {
				if (first) {
					first = false;
					byAuthor.put(0, ex);
				} else {
					byAuthor.put(byAuthor.lastKey()+1, ex);
				}
			}
		}
		
		if (byAuthor.isEmpty()) {
			return null;
		}
		return byAuthor;
	}
	
	public Map<Integer, CloudExpansion> getAllInstalled() {
		if (remote.isEmpty()) {
			return null;
		}
		TreeMap<Integer, CloudExpansion> has = new TreeMap<>();
		boolean first = true;
		for (CloudExpansion ex : remote.values()) {
			if (ex.hasExpansion()) {
				if (first) {
					first = false;
					has.put(1, ex);
				} else {
					has.put(has.lastKey()+1, ex);
				}
			}
		}
		
		if (has.isEmpty()) {
			return null;
		}
		return has;
	}
	
	public int getPagesAvailable(Map<Integer, CloudExpansion> map, int amount) {
		if (map == null) { 
			return 0;
		}
		int pages = map.size() > 0 ? 1 : 0;
		if (pages == 0) {
			return pages;
		}
		if (map.size() > amount) {
			pages = map.size()/amount;
			if (map.size() % amount > 0) {
				pages++;
			}
		}
		return pages;
	}
	
	public Map<Integer, CloudExpansion> getPage(Map<Integer, CloudExpansion> map, int page, int size) {
		if (map == null || map.size() == 0 || page > getPagesAvailable(map, size)) {
			return null;
		}
		int end = size*page;
		int start = end-size;
		TreeMap<Integer, CloudExpansion> ex = new TreeMap<>();
        IntStream.range(start, end).forEach(n -> ex.put(n, map.get(n)));
		return ex;
	}
	
	public void fetch() {
		
		plugin.getLogger().info("Fetching available expansion information...");

		new BukkitRunnable() {
			
				@Override
				public void run() {

					StringBuilder sb;

					try {

						URL site = new URL(API);
						
						HttpURLConnection connection = (HttpURLConnection) site.openConnection();
						
						connection.setRequestMethod("GET");
						
						connection.connect();
						
						BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						
						sb = new StringBuilder();
						
						String line;

						while ((line = br.readLine()) != null) {
							sb.append(line);
						}

						br.close();
						connection.disconnect();

					} catch (Exception e) {
						return;
					}

					String json = sb.toString();
					JSONParser parser = new JSONParser();
					Object obj = null;

					try {
                        obj = parser.parse(json);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

					if (obj == null) {
                        return;
                    }

					List<CloudExpansion> unsorted = new ArrayList<>();

					if (obj instanceof JSONObject) {

                        JSONObject jo = (JSONObject) obj;

                        for (Object o : jo.keySet()) {

                            JSONObject sub = (JSONObject) jo.get(o);
                            String name = o.toString();
                            String author = (String) sub.get("author");
                            String version = (String) sub.get("version");
                            String link = (String) sub.get("link");
                            String description = (String) sub.get("description");
                            String notes = "";
                            long update = -1;

                            if (sub.get("release_notes") != null) {
                                notes = (String) sub.get("release_notes");
                            }

                            if (sub.get("last_update") != null) {

                                Object u = sub.get("last_update");

                                if (u instanceof Long) {
                                    update = (long) sub.get("last_update");
                                }
                            }

                            CloudExpansion ce = new CloudExpansion(name, author, version, description, link);

                            ce.setReleaseNotes(notes);

                            ce.setLastUpdate(update);

                            PlaceholderExpansion ex = plugin.getExpansionManager().getRegisteredExpansion(name);

                            if (ex != null && ex.isRegistered()) {
                                ce.setHasExpansion(true);
                                if (!ex.getVersion().equals(version)) {
                                    ce.setShouldUpdate(true);
                                }
                            }

                            unsorted.add(ce);
                        }

                        int count = 0;

                        unsorted.sort(Comparator.comparing(CloudExpansion::getLastUpdate).reversed());

                        for (CloudExpansion e : unsorted) {
                            remote.put(count, e);
                            count++;
                        }

                        plugin.getLogger().info(count + " placeholder expansions are available on the cloud.");

                        long updates = getToUpdateCount();

                        if (updates > 0) {
                            plugin.getLogger().info(updates + " installed expansions have updates available.");
                        }
                    }
				}
		}.runTaskAsynchronously(plugin);
	}
	
	private void download(URL url, String name) throws IOException {
	    
		InputStream is = null;
		
	    FileOutputStream fos = null;

	    try {
	    	
	        URLConnection urlConn = url.openConnection();
	        
	        is = urlConn.getInputStream();  
	        
	        fos = new FileOutputStream(dir.getAbsolutePath() + File.separator + "Expansion-" + name + ".jar");

	        byte[] buffer = new byte[is.available()];
	        
	        int l;

	        while ((l = is.read(buffer)) > 0) {  
	            fos.write(buffer, 0, l);
	        }
	    } finally {
	        try {
	            if (is != null) {
	                is.close();
	            }
	        } finally {
	            if (fos != null) {
	                fos.close();
	            }
	        }
	    }
	}
	
	public void downloadExpansion(final String player, final CloudExpansion ex) {
		
		if (downloading.contains(ex.getName())) {
			return;
		}
		
		if (ex.getLink() == null) {
			return;
		}
		
		downloading.add(ex.getName());
		
		plugin.getLogger().info("Attempting download of expansion: " + ex.getName() + (player != null ? " by user: " + player : "") + " from url: " + ex.getLink());

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            try {

                download(new URL(ex.getLink()), ex.getName());

                plugin.getLogger().info("Download of expansion: " + ex.getName() + " complete!");

            } catch (Exception e) {

                plugin.getLogger().warning("Failed to download expansion: " + ex.getName() + " from: " + ex.getLink());

                Bukkit.getScheduler().runTask(plugin, () -> {

                    downloading.remove(ex.getName());

                    if (player != null) {
                        Player p = Bukkit.getPlayer(player);

                        if (p != null) {
                            Msg.msg(p, "&cThere was a problem downloading expansion: &f" + ex.getName());
                        }
                    }
                });

                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {

                downloading.remove(ex.getName());

                if (player != null) {

                    Player p = Bukkit.getPlayer(player);

                    if (p != null) {
                        Msg.msg(p, "&aExpansion &f" + ex.getName() + " &adownload complete!");
                    }
                }
            });

        });
	}
}
