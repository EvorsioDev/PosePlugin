package ru.armagidon.poseplugin.api.utils.nms.protocolized.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import ru.armagidon.poseplugin.api.PosePluginAPI;
import ru.armagidon.poseplugin.api.utils.misc.BlockCache;
import ru.armagidon.poseplugin.api.utils.misc.BlockPositionUtils;
import ru.armagidon.poseplugin.api.utils.nms.ToolPackage;
import ru.armagidon.poseplugin.api.utils.nms.npc.FakePlayer;
import ru.armagidon.poseplugin.api.utils.nms.npc.FakePlayerUtils;
import ru.armagidon.poseplugin.api.utils.nms.protocolized.wrappers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.armagidon.poseplugin.api.utils.nms.npc.FakePlayerUtils.setBit;
import static ru.armagidon.poseplugin.api.utils.nms.protocolized.npc.MetadataEditorProtocolized.OVERLAYS;

@ToolPackage(mcVersion = "protocolized")
public class FakePlayerProtocolized extends FakePlayer<WrappedDataWatcher>
{

    private static final Random RANDOM = new Random();

    @Getter private final int id;

    @Getter private final WrappedDataWatcher dataWatcher;

    @Getter private final Location position;

    private final WrapperPlayServerEntityDestroy destroy;
    private WrapperPlayServerNamedEntitySpawn spawner;
    private final WrapperPlayServerPlayerInfo addInfo;
    private final WrapperPlayServerRelEntityMoveLook movePacket;
    private WrapperPlayServerBlockChange fakeBedPacket;

    private static Object packetListener; //PacketAdapter NoClassDefFoundError avoiding crutch


    public FakePlayerProtocolized(Player parent, Pose pose) {
        super(parent, pose);
        this.position = parent.getLocation().clone();

        this.id = RANDOM.nextInt(9999);

        //Get Location of fake bed
        this.bedLoc = FakePlayerUtils.toBedLocation(position);
        //Cache original type of block
        this.cache = new BlockCache(bedLoc.getBlock().getBlockData(), bedLoc);

        //Create single instances of packet for optimisation purposes. So server won't need to create tons of copies of the same packet.

        //Create instance of move packet to pop up npc a little
        this.movePacket = new WrapperPlayServerRelEntityMoveLook();
        movePacket.setEntityID(id);
        movePacket.setDy(0.1D);
        movePacket.setOnGround(true);

        //Create packet instance of fake bed(could've used sendBlockChange but im crazy and it will recreate copies of the same packet)
        this.fakeBedPacket = bedPacket(position.getYaw());
        //Create packet instance of NPC 's data
        this.addInfo = infoPacket();

        //Create instance of npc
        this.spawner = spawnerPacket(parent, id);

        //Create data watcher to modify entity metadata


        this.dataWatcher = WrappedDataWatcher.getEntityWatcher(parent).deepClone();
        //Create instance of the packet with this data

        metadataAccessor = new MetadataEditorProtocolized(this);
        npcSynchronizer = new NPCSynchronizerProtocolized(this);
        inventory = NPCInventoryProtocolized.createInventory(this);

        //Set metadata
        setMetadata();

        this.destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(id);

    }

    private void setMetadata(){
        //Save current overlay bit mask
        byte overlays = getDataWatcher().getByte(OVERLAYS.getKey());
        //Set pose to the NPC
        metadataAccessor.setPose(pose);
        //Set current overlays to the NPC
        metadataAccessor.setOverlays(overlays);
        //Set BedLocation to NPC if its pose is SLEEPING
        if(metadataAccessor.getPose().equals(Pose.SLEEPING))
            metadataAccessor.setBedPosition(bedLoc);
        else if (metadataAccessor.getPose().equals(Pose.SPIN_ATTACK)){
            metadataAccessor.setLivingEntityTags(setBit(metadataAccessor.getLivingEntityTags(), 2, true));
        }
        metadataAccessor.merge(true);

    }

    private WrapperPlayServerNamedEntitySpawn spawnerPacket(Player parent, int id) {
        WrapperPlayServerNamedEntitySpawn spawn = new WrapperPlayServerNamedEntitySpawn();
        spawn.setEntityID(id);
        spawn.setX(position.getX());
        spawn.setY(position.getY());
        spawn.setZ(position.getZ());
        spawn.setPlayerUUID(parent.getUniqueId());
        return spawn;
    }

    private WrapperPlayServerBlockChange bedPacket(float angle) {
        WrapperPlayServerBlockChange fakeBedPacket = new WrapperPlayServerBlockChange();
        fakeBedPacket.setLocation(new BlockPosition(position.toVector().setY(0)));
        Bed bed = (Bed) Bukkit.createBlockData(Material.WHITE_BED);
        bed.setPart(Bed.Part.HEAD);
        bed.setFacing(BlockPositionUtils.yawToFace(angle).getOppositeFace());
        fakeBedPacket.setBlockData(WrappedBlockData.createData(bed));
        return fakeBedPacket;
    }

    private WrapperPlayServerPlayerInfo infoPacket() {
        WrapperPlayServerPlayerInfo addInfo = new WrapperPlayServerPlayerInfo();
        addInfo.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        List<PlayerInfoData> dataList = new ArrayList<>();
        dataList.add(new PlayerInfoData(new WrappedGameProfile(parent.getUniqueId(), parent.getName()), 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText("")));
        addInfo.setData(dataList);
        return addInfo;
    }

    @Override
    public void tick() {
        //Get players nearby
        Set<Player> detectedPlayers = BlockPositionUtils.getNear(getViewDistance(), parent);

        //Check if some of them aren't trackers
        for (Player detectedPlayer : detectedPlayers) {
            if(!this.trackers.contains(detectedPlayer)){
                spawnToPlayer(detectedPlayer);
                trackers.add(detectedPlayer);
            }
        }
        //Check if some of trackers aren't in view distance
        for (Player tracker : this.trackers) {
            if(!detectedPlayers.contains(tracker)){
                removeToPlayer(tracker);
                trackers.remove(tracker);
            }
        }

        if(isSynchronizationEquipmentEnabled()) npcSynchronizer.syncEquipment();
        if(isSynchronizationOverlaysEnabled()) npcSynchronizer.syncOverlays();
        if(isHeadRotationEnabled()) npcSynchronizer.syncHeadRotation();

        trackers.forEach(fakeBedPacket::sendPacket);
    }

    @Override
    public void broadCastSpawn() {
        Set<Player> detectedPlayers = Bukkit.getOnlinePlayers().stream().filter(p -> p.getWorld().equals(parent.getWorld()))
                .filter(p -> p.getLocation().distanceSquared(parent.getLocation()) <= Math.pow(viewDistance, 2)).collect(Collectors.toSet());
        trackers.addAll(detectedPlayers);
        trackers.forEach(this::spawnToPlayer);
        if (isDeepDiveEnabled()) activateDeepDive();
    }

    @Override
    public void spawnToPlayer(Player player) {
        spawner.sendPacket(player);
        fakeBedPacket.sendPacket(player);
        inventory.show(player);
        metadataAccessor.showPlayer(player);
        if(metadataAccessor.getPose().equals(Pose.SLEEPING))
            movePacket.sendPacket(player);
        if(isHeadRotationEnabled()) {
            setHeadRotationEnabled(false);
            PosePluginAPI.getAPI().getTickManager().later(() ->
                    setHeadRotationEnabled(true), 10);
        }
    }

    @Override
    public void removeToPlayer(Player player) {
        destroy.sendPacket(player);
        cache.restore(player);
    }

    public void swingHand(boolean mainHand) {
        if(isSwingAnimationEnabled()) {
            WrapperPlayServerAnimation animation = new WrapperPlayServerAnimation();
            animation.setEntityID(id);
            animation.setAnimation(mainHand ? 0 : 3);
            trackers.forEach(animation::sendPacket);
        }
    }


    @Override
    protected void activateDeepDive() {
        if (packetListener == null) {
            packetListener = new PacketAdapter(PosePluginAPI.getAPI().getPlugin(), PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    WrapperPlayClientUseEntity packet = new WrapperPlayClientUseEntity(event.getPacket());
                    if (!FAKE_PLAYERS.containsKey(event.getPlayer())) return;
                    if (!FAKE_PLAYERS.get(event.getPlayer()).isDeepDiveEnabled()) return;
                    int id = packet.getTargetID();
                    if (id == event.getPlayer().getEntityId())
                        event.setCancelled(true);
                }
            };
            ProtocolLibrary.getProtocolManager().addPacketListener((PacketListener) packetListener);
        }
        WrapperPlayServerCamera camera = new WrapperPlayServerCamera();
        camera.setCameraId(getId());
        camera.sendPacket(getParent());
    }

    @Override
    protected void deactivateDeepDive() {
        WrapperPlayServerCamera camera = new WrapperPlayServerCamera();
        camera.setCameraId(getParent().getEntityId());
        camera.sendPacket(getParent());
    }

    @Override
    public void setLocationRotation(double x, double y, double z, float pitch, float yaw) {
        this.movePacket.setPitch(pitch);
        this.movePacket.setYaw(yaw);
        this.position.setX(x);
        this.position.setY(y);
        this.position.setZ(z);
        this.position.setPitch(pitch);
        this.position.setYaw(yaw);
        this.spawner.setX(x);
        this.spawner.setY(y);
        this.spawner.setZ(z);
    }

    @Override
    public void setRotation(float pitch, float yaw) {
        this.movePacket.setPitch(pitch);
        this.movePacket.setYaw(yaw);
        this.spawner.setYaw(yaw);
        this.spawner.setPitch(pitch);
        this.position.setPitch(pitch);
        this.position.setYaw(yaw);
    }

    public void animation(byte id){
        WrapperPlayServerEntityStatus status = new WrapperPlayServerEntityStatus();
        status.setEntityID(id);
        status.setEntityStatus(id);
        trackers.forEach(status::sendPacket);
    }

    @Override
    public void teleport(Location destination) {
        getTrackers().forEach(t -> cache.restore(t));
        Location bedLoc = FakePlayerUtils.toBedLocation(destination);

        cache.setLocation(bedLoc);
        cache.setData(bedLoc.getBlock().getBlockData());

        fakeBedPacket = bedPacket(parent.getLocation().getYaw());

        this.bedLoc.setX(destination.getX());
        this.bedLoc.setY(destination.getY());
        this.bedLoc.setZ(destination.getZ());


        getMetadataAccessor().setBedPosition(bedLoc);

        getMetadataAccessor().merge(true);

        spawner = spawnerPacket(parent, id);

        updateNPC();

        getTrackers().forEach(movePacket::sendPacket);

    }

    private void updateOverlays() {
        npcSynchronizer.syncOverlays();
    }

    private void updateHeadRotation() {
        npcSynchronizer.syncHeadRotation();
    }

    private void updateEquipment(){
        npcSynchronizer.syncEquipment();
    }
}
