package com.zachduda.flowerdups;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import space.arim.morepaperlib.MorePaperLib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;


public class Main extends JavaPlugin implements Listener {
    public MorePaperLib morePaperLib = new MorePaperLib(this);

    static Logger log = Bukkit.getLogger();
    private static final String logtag = "[FlowerDups] ";

    static boolean debug = false;

    public List<Material> flowers = new ArrayList<>();
    private final String flowerEndpoint = "https://raw.githubusercontent.com/zachduda/FlowerDups/refs/heads/master/flowers_v1.json";

    public static void log(String msg, Boolean critical, Boolean warning) {
        if (critical) {
            if(warning) {
                log.warning("[!] " + logtag + msg);
                return;
            }
            log.info(logtag + msg);
        }
    }

    public static void debug(String msg) {
        if (debug) {
            log("[Debug] " + msg, true, false);
        }
    }

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        loadFlowersFromEndpoint();
        registerCommands();
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("reloadflowers")).setExecutor((sender, cmd, label, args) -> {
            if(!sender.hasPermission("flowerdups.reload")) {
                sender.sendMessage("§cYou don't have permission!");
                return true;
            }
            loadFlowersFromEndpoint();
            sender.sendMessage("§aFlowers reloaded! Count: " + flowers.size());
            return true;
        });
    }

    private void loadFlowersFromEndpoint() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                log("Fetching flowers from endpoint: " + flowerEndpoint, true, false);

                URL url = new URL(flowerEndpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if(connection.getResponseCode() != 200) {
                    log("Failed to fetch flowers! HTTP Code: " + connection.getResponseCode(), true, true);
                    loadDefaultFlowers();
                    return;
                }

                JSONArray jsonArray = getJsonArray(connection);
                List<Material> loadedFlowers = new ArrayList<>();

                for(Object obj : jsonArray) {
                    String materialName = (String) obj;
                    Material material = Material.getMaterial(materialName);

                    if(material != null) {
                        loadedFlowers.add(material);
                        debug("Loaded flower: " + materialName);
                    } else {
                        log("Invalid material: " + materialName, true, true);
                    }
                }

                if(loadedFlowers.isEmpty()) {
                    log("No valid flowers loaded from endpoint! Using defaults.", true, true);
                    loadDefaultFlowers();
                } else {
                    flowers = loadedFlowers;
                    log("Successfully loaded " + flowers.size() + " flowers from endpoint", true, false);
                }

            } catch(Exception e) {
                log("Error fetching flowers: " + e.getMessage(), true, true);
                loadDefaultFlowers();
            }
        });
    }

    private static JSONArray getJsonArray(HttpURLConnection connection) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder jsonString = new StringBuilder();
        String line;

        while((line = reader.readLine()) != null) {
            jsonString.append(line);
        }
        reader.close();

        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse(jsonString.toString());
    }

    private void loadDefaultFlowers() {
        flowers = new ArrayList<>(List.of(Material.FLOWERING_AZALEA, Material.CORNFLOWER));
        log("Using default flowers", true, false);
    }

    public void onDisable() {
        morePaperLib.scheduling().cancelGlobalTasks();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerClick(PlayerInteractEvent e) {
        final Player p = e.getPlayer();

        if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if(e.getItem() == null || !e.getItem().getType().equals(Material.BONE_MEAL)) {
            return;
        }

        if(e.getClickedBlock() == null || !flowers.contains(e.getClickedBlock().getType())) {
            return;
        }

        debug("Player " + p.getName() + " used bonemeal on a flower!");
        duplicateFlower(e.getClickedBlock());

        e.getItem().setAmount(e.getItem().getAmount() - 1);
    }

    private void duplicateFlower(org.bukkit.block.Block flowerBlock) {
        org.bukkit.block.Block[] adjacentBlocks = {
                flowerBlock.getRelative(org.bukkit.block.BlockFace.NORTH),
                flowerBlock.getRelative(org.bukkit.block.BlockFace.SOUTH),
                flowerBlock.getRelative(org.bukkit.block.BlockFace.EAST),
                flowerBlock.getRelative(org.bukkit.block.BlockFace.WEST)
        };

        Material flowerType = flowerBlock.getType();

        for(org.bukkit.block.Block adjacent : adjacentBlocks) {
            if(adjacent.getType() == Material.AIR) {
                adjacent.setType(flowerType);

                flowerBlock.getWorld().playSound(
                        flowerBlock.getLocation(),
                        Sound.BLOCK_COMPOSTER_FILL_SUCCESS,
                        1.0f,
                        1.0f
                );

                flowerBlock.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        adjacent.getLocation().add(0.5, 0.5, 0.5),
                        10,
                        0.3,
                        0.3,
                        0.3,
                        0.1
                );

                break;
            }
        }
    }
}