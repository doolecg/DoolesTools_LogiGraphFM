package net.doole.doolestools.blockentity;

import net.doole.doolestools.logistics.switchboard.SwitchboardCleanup;
import net.doole.doolestools.logistics.switchboard.SwitchboardLinkData;
import net.doole.doolestools.logistics.switchboard.SwitchboardNodePositionData;
import net.doole.doolestools.menu.NetworkSwitchboardMenu;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.util.ValueInput;
import net.doole.doolestools.util.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NetworkSwitchboardBlockEntity extends BlockEntity implements MenuProvider {
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Map<BlockPos, NetworkSwitchboardBlockEntity>> LOADED = new ConcurrentHashMap<>();

    private List<SwitchboardLinkData> links = new ArrayList<>();
    private List<SwitchboardNodePositionData> nodePositions = new ArrayList<>();

    public NetworkSwitchboardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_SWITCHBOARD.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            LOADED.computeIfAbsent(serverLevel.dimension(), key -> new ConcurrentHashMap<>()).put(worldPosition.immutable(), this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            Map<BlockPos, NetworkSwitchboardBlockEntity> map = LOADED.get(serverLevel.dimension());
            if (map != null) map.remove(worldPosition);
        }
        super.setRemoved();
    }

    public static void forEachLoaded(ServerLevel level, Consumer<NetworkSwitchboardBlockEntity> consumer) {
        Map<BlockPos, NetworkSwitchboardBlockEntity> map = LOADED.get(level.dimension());
        if (map == null) return;
        map.values().forEach(be -> {
            if (!be.isRemoved()) consumer.accept(be);
        });
    }

    public static void removeNetworkFromLoaded(ServerLevel level, String networkId) {
        forEachLoaded(level, switchboard -> switchboard.removeNetwork(networkId));
    }

    public boolean removeNetwork(String networkId) {
        SwitchboardCleanup.Result result = SwitchboardCleanup.removeNetwork(networkId, links, nodePositions);
        if (result.links().equals(links) && result.nodePositions().equals(nodePositions)) return false;
        links = new ArrayList<>(result.links());
        nodePositions = new ArrayList<>(result.nodePositions());
        setChanged();
        return true;
    }

    public boolean retainNetworks(Set<String> liveNetworkIds) {
        Set<String> live = liveNetworkIds == null ? Set.of() : liveNetworkIds;
        List<SwitchboardLinkData> keptLinks = new ArrayList<>();
        for (SwitchboardLinkData link : links) {
            if (live.contains(link.sourceNetworkId()) && live.contains(link.targetNetworkId())) keptLinks.add(link);
        }
        List<SwitchboardNodePositionData> keptPositions = new ArrayList<>();
        for (SwitchboardNodePositionData position : nodePositions) {
            if (live.contains(position.networkId())) keptPositions.add(position);
        }
        if (keptLinks.equals(links) && keptPositions.equals(nodePositions)) return false;
        links = keptLinks;
        nodePositions = keptPositions;
        setChanged();
        return true;
    }

    public static String networkDisplayName(ServerLevel level, String networkId) {
        if (networkId == null || networkId.isBlank()) return "";
        final String[] found = {""};
        forEachLoaded(level, switchboard -> {
            if (!found[0].isBlank()) return;
            BlockPos center = switchboard.getBlockPos();
            int radius = 96;
            int minChunkX = (center.getX() - radius) >> 4;
            int maxChunkX = (center.getX() + radius) >> 4;
            int minChunkZ = (center.getZ() - radius) >> 4;
            int maxChunkZ = (center.getZ() + radius) >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX && found[0].isBlank(); chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ && found[0].isBlank(); chunkZ++) {
                    if (!(level.getChunk(chunkX, chunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof LogisticsComputerBlockEntity computer && networkId.equals(computer.networkId())) {
                            found[0] = computer.networkName();
                            break;
                        }
                    }
                }
            }
        });
        return found[0];
    }

    public List<SwitchboardLinkData> links() {
        return List.copyOf(links);
    }

    public List<SwitchboardNodePositionData> nodePositions() {
        return List.copyOf(nodePositions);
    }

    public void setLinks(List<SwitchboardLinkData> newLinks) {
        List<SwitchboardLinkData> sanitized = new ArrayList<>();
        if (newLinks != null) {
            for (SwitchboardLinkData link : newLinks) {
                SwitchboardLinkData clean = link.sanitized();
                if (clean.valid()) sanitized.add(clean);
                if (sanitized.size() >= 64) break;
            }
        }
        this.links = sanitized;
        setChanged();
    }

    public void setNodePositions(List<SwitchboardNodePositionData> newPositions) {
        List<SwitchboardNodePositionData> sanitized = new ArrayList<>();
        if (newPositions != null) {
            for (SwitchboardNodePositionData position : newPositions) {
                SwitchboardNodePositionData clean = position.sanitized();
                if (clean.valid()) sanitized.add(clean);
                if (sanitized.size() >= 128) break;
            }
        }
        this.nodePositions = sanitized;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Network Switchboard");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkSwitchboardMenu(containerId, playerInventory, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(new ValueOutput(tag, registries));
    }

    private void saveData(ValueOutput output) {
        output.store("links", SwitchboardLinkData.CODEC.listOf(), links);
        output.store("nodePositions", SwitchboardNodePositionData.CODEC.listOf(), nodePositions);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(new ValueInput(tag, registries));
    }

    private void loadData(ValueInput input) {
        this.links = new ArrayList<>(input.read("links", SwitchboardLinkData.CODEC.listOf()).orElse(List.of()));
        this.nodePositions = new ArrayList<>(input.read("nodePositions", SwitchboardNodePositionData.CODEC.listOf()).orElse(List.of()));
    }
}
