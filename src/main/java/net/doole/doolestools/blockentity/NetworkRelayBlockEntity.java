package net.doole.doolestools.blockentity;

import net.doole.doolestools.logistics.network.NetworkNodeIndex;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.world.NetworkIdentitySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class NetworkRelayBlockEntity extends BlockEntity {
    public static final int MAX_UPGRADES_PER_TYPE = 4;

    private String relayId = "";
    private int relayNumber;
    private String displayName = "";
    private String networkId = "";
    private int speedUpgrades;
    private int rangeUpgrades;
    private int efficiencyUpgrades;

    public NetworkRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_RELAY.get(), pos, state);
    }

    // Join/leave the per-dimension node index so the power calculator can find relays without
    // scanning chunks. onLoad fires on placement and on chunk load; setRemoved on break and unload.
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            NetworkNodeIndex.addRelay(serverLevel, this);
            ensureRelayNumber();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        saveAdditional(output);
        return output.buildResult();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) NetworkNodeIndex.removeRelay(serverLevel, worldPosition);
        super.setRemoved();
    }

    public String relayId() {
        if (relayId == null || relayId.isBlank()) relayId = "relay_" + Long.toUnsignedString(worldPosition.asLong());
        ensureRelayNumber();
        return relayId;
    }

    public int relayNumber() {
        ensureRelayNumber();
        return relayNumber;
    }

    public String formattedRelayId() {
        return NetworkIdentitySavedData.formatFourDigits(relayNumber());
    }

    public String displayName() {
        if (displayName == null || displayName.isBlank()) return "Network Relay#" + formattedRelayId();
        return displayName;
    }

    public String networkId() {
        return networkId == null ? "" : networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = NetworkEndpointBlockEntity.sanitizeNetworkId(networkId);
        setChanged();
    }

    public void setIdentity(String name, String networkId) {
        this.displayName = NetworkEndpointBlockEntity.sanitize(name);
        setNetworkId(networkId);
    }

    private void ensureRelayNumber() {
        if (relayNumber > 0) return;
        if (level instanceof ServerLevel serverLevel) {
            relayNumber = NetworkIdentitySavedData.get(serverLevel).allocateEndpointNumber();
            setChanged();
            // No sendBlockUpdated here (runs during onLoad/save); getUpdatePacket() syncs on placement.
        }
    }

    public boolean installUpgrade(String type) {
        int current = upgradeCount(type);
        if (current >= MAX_UPGRADES_PER_TYPE) return false;
        setUpgradeCount(type, current + 1);
        setChanged();
        return true;
    }

    public int upgradeCount(String type) {
        return switch (type == null ? "" : type) {
            case "speed" -> speedUpgrades;
            case "range" -> rangeUpgrades;
            case "efficiency" -> efficiencyUpgrades;
            default -> 0;
        };
    }

    public boolean removeUpgrade(String type) {
        int current = upgradeCount(type);
        if (current <= 0) return false;
        setUpgradeCount(type, current - 1);
        setChanged();
        return true;
    }

    private void setUpgradeCount(String type, int value) {
        int capped = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, value));
        switch (type == null ? "" : type) {
            case "speed" -> speedUpgrades = capped;
            case "range" -> rangeUpgrades = capped;
            case "efficiency" -> efficiencyUpgrades = capped;
            default -> { }
        }
    }

    public int[] upgradeCounts() {
        return new int[] { speedUpgrades, -1, rangeUpgrades, efficiencyUpgrades };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("relayId", com.mojang.serialization.Codec.STRING, relayId());
        output.putInt("relayNumber", relayNumber());
        output.store("displayName", com.mojang.serialization.Codec.STRING, displayName == null ? "" : displayName);
        output.store("networkId", com.mojang.serialization.Codec.STRING, networkId());
        output.putInt("speedUpgrades", speedUpgrades);
        output.putInt("rangeUpgrades", rangeUpgrades);
        output.putInt("efficiencyUpgrades", efficiencyUpgrades);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        relayId = input.read("relayId", com.mojang.serialization.Codec.STRING).orElse("");
        relayNumber = Math.max(0, input.getIntOr("relayNumber", 0));
        displayName = input.read("displayName", com.mojang.serialization.Codec.STRING).orElse("");
        networkId = NetworkEndpointBlockEntity.sanitizeNetworkId(input.read("networkId", com.mojang.serialization.Codec.STRING).orElse(""));
        speedUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("speedUpgrades", 0)));
        rangeUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("rangeUpgrades", 0)));
        efficiencyUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("efficiencyUpgrades", 0)));
    }

}
