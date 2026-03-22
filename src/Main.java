package com.zachduda.chatfeelings;

import com.zachduda.chatfeelings.api.*;
import com.zachduda.chatfeelings.other.Supports;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;

import java.util.*;
import java.util.logging.Logger;

/*
    A plugin by Zach Duda
    2026 - Licensed under CC-BY-NC-v4 (Copy @ https://zachduda.com/cc-license)
 */

public class Main extends JavaPlugin implements Listener {
    public MorePaperLib morePaperLib = new MorePaperLib(this);

    static Logger log = Bukkit.getLogger();
    private static final String logtag = "[ChatFeelings] ";

    public static void log(String msg, Boolean critical, Boolean warning) {
        if (critical || !reducemsgs) {
            if(warning) {
                log.warning("[!] " + logtag + msg);
                return;
            }
            log.info( logtag + msg);
        }
    }

    public static void debug(String msg) {
        if (debug) {
            log("[Debug] " + msg, true, false);
        }
    }

    public void onDisable() {
        morePaperLib.scheduling().cancelGlobalTasks();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        morePaperLib.scheduling().asyncScheduler().run(() -> {
            Player p = e.getPlayer();

            if (p.getUniqueId().toString().equals("6191ff85-e092-4e9a-94bd-63df409c2079")) {
                Msgs.send(p, "&7This server is running &fChatFeelings &6v" + getDescription().getVersion() +
                        " &7for " + Supports.getMCVersion() + "." + Supports.getMcPatchVersion());
            }
        });
    }
}
