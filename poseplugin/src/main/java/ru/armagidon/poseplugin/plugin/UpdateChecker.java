package ru.armagidon.poseplugin.plugin;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.armagidon.poseplugin.PosePlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker extends BukkitRunnable
{

    public boolean uptodate = false;
    private String newest = "";

    @Override
    public void run() {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=76990").openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String newest = reader.lines().collect(Collectors.toList()).get(0);
            reader.close();

            String current = PosePlugin.getInstance().getDescription().getVersion();

            if (!current.equalsIgnoreCase(newest)) {
                uptodate = false;
                this.newest = newest;
            } else {
                uptodate = true;
                PosePlugin.getInstance().getLogger().info("Plugin Up to Date");
            }
        } catch (IOException var7) {
            Bukkit.getLogger().severe("§cCheck update fail: " + var7.getMessage());
        }

    }

    public void sendNotification(Player p){
        TextComponent link = new TextComponent("§bDownload");
        TextComponent msg = new TextComponent(ChatColor.translateAlternateColorCodes('&',"&7&lNEW UPDATE &e" + newest + "&7&l AVAILABLE! CLICK TO DOWNLOAD!"));
        p.sendMessage(ChatColor.translateAlternateColorCodes('&',"                   &f&l{[&3POSEPLUGIN&f&l]}"));
        msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{link}));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://www.spigotmc.org/resources/poseplugin-choose-your-favorite-pose.76990/"));
        p.spigot().sendMessage(msg);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&',"                   &f&l{[&3POSEPLUGIN&f&l]}"));

        PosePlugin.getInstance().getDescription().getAuthors().stream().map(Bukkit::getPlayer).filter(pl->pl != null && pl.isOnline()).forEach(pl->
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',"&bDEVELOPER IS ONLINE ON THIS SERVER > &a"+pl.getName())));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }
}
