package ru.armagidon.poseplugin.api.personalListener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import ru.armagidon.poseplugin.api.PosePluginAPI;
import ru.armagidon.poseplugin.api.events.PlayerArmorChangeEvent;
import ru.armagidon.poseplugin.api.player.PosePluginPlayer;

public class PersonalEventDispatcher implements Listener
{
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event){
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if(playerAbsent(p)) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(p.getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    private boolean playerAbsent(Player player) {
        return !PosePluginAPI.getAPI().getPlayerMap().containsPlayer(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public void gameMode(PlayerGameModeChangeEvent event) {
        if (playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event){
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event){
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        if(event.getAction().equals(Action.RIGHT_CLICK_AIR)||event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event){
        //If player's not in player list, ignore him
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player =PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public void death(PlayerDeathEvent event){
        //If player's not in player list, ignore him
        if(playerAbsent(event.getEntity())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getEntity().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event){
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler(ignoreCancelled = true)
    public final void onTeleport(PlayerTeleportEvent event){
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

    @EventHandler
    public final void onArmorChange(PlayerArmorChangeEvent event){
        if(playerAbsent(event.getPlayer())) return;
        PosePluginPlayer player = PosePluginAPI.getAPI().getPlayerMap().getPosePluginPlayer(event.getPlayer().getName());
        PosePluginAPI.getAPI().getPersonalHandlerList().dispatch(player,event);
    }

}
