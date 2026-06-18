package net.doole.doolestools.blockentity;

import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.registry.ModBlockEntities;
import net.doole.doolestools.block.NetworkWireBlock;
import net.doole.doolestools.world.NetworkIdentitySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class NetworkWireBlockEntity extends BlockEntity {
    public static final int MAX_LANES = 4;

    private int laneCount = 1;
    private int connectionMask = 0;
    private String endpointKind = "";
    private String endpointId = "";
    private int endpointNumber;
    private String endpointName = "";
    private String endpointNetworkId = "";
    private Direction endpointFace = Direction.NORTH;
    private int speedUpgrades;
    private int stackUpgrades;
    private int rangeUpgrades;
    private int efficiencyUpgrades;

    public NetworkWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_WIRE.get(), pos, state);
    }

    // Sync endpoint identity/upgrade state to the client so the naming screen shows real values.
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

    public int laneCount() {
        return Math.max(1, Math.min(MAX_LANES, laneCount));
    }

    public int connectionMask() {
        return connectionMask;
    }

    public boolean connects(Direction direction) {
        return (connectionMask & bit(direction)) != 0;
    }

    public boolean hasEndpoint() {
        return "modem".equals(endpointKind);
    }

    public boolean hasRouter() {
        return "router".equals(endpointKind);
    }

    public boolean hasModem() {
        return "modem".equals(endpointKind);
    }

    public String endpointKind() {
        return endpointKind == null ? "" : endpointKind;
    }

    public String endpointId() {
        if (endpointId == null || endpointId.isBlank()) endpointId = endpointKind + "_" + Long.toUnsignedString(worldPosition.asLong());
        ensureEndpointNumber();
        return endpointId;
    }

    public int endpointNumber() {
        ensureEndpointNumber();
        return endpointNumber;
    }

    public String formattedEndpointId() {
        return NetworkIdentitySavedData.formatFourDigits(endpointNumber());
    }

    public String endpointNetworkId() {
        return endpointNetworkId == null ? "" : endpointNetworkId;
    }

    public String endpointName() {
        if (endpointName == null || endpointName.isBlank()) return defaultAttachedName();
        return endpointName;
    }

    public Direction endpointFace() {
        return endpointFace == null ? Direction.NORTH : endpointFace;
    }

    public BlockPos attachedPos() {
        return worldPosition.relative(endpointFace());
    }

    public boolean installEndpoint(String kind, Direction face, String name) {
        if (hasEndpoint()) return false;
        if (!"modem".equals(kind)) return false;
        endpointKind = kind;
        endpointFace = face == null ? Direction.NORTH : face;
        endpointId = endpointKind + "_" + Long.toUnsignedString(worldPosition.asLong());
        endpointNumber = 0;
        ensureEndpointNumber();
        endpointName = sanitize(name);
        endpointNetworkId = level instanceof ServerLevel serverLevel
                ? NetworkEndpointBlockEntity.inferNearbyNetwork(serverLevel, worldPosition)
                : "";
        refreshBlockState();
        setChanged();
        return true;
    }

    public void setEndpointIdentityFromName(String name) {
        if (!hasEndpoint()) return;
        String cleaned = NetworkEndpointBlockEntity.sanitize(name);
        endpointName = cleaned;
        setChanged();
    }

    public void setEndpointIdentity(String name, String networkId) {
        if (!hasEndpoint()) return;
        endpointName = NetworkEndpointBlockEntity.sanitize(name);
        endpointNetworkId = NetworkEndpointBlockEntity.sanitizeNetworkId(networkId);
        setChanged();
    }

    public boolean installUpgrade(UpgradeType type) {
        if (type == null) return false;
        return installUpgrade(type.id);
    }

    public boolean installUpgrade(String type) {
        if (!hasEndpoint()) return false;
        int current = upgradeCount(type);
        if (current >= NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE) return false;
        setUpgradeCount(type, current + 1);
        setChanged();
        return true;
    }

    public int upgradeCount(UpgradeType type) {
        return type == null ? 0 : upgradeCount(type.id);
    }

    public int upgradeCount(String type) {
        return switch (type == null ? "" : type) {
            case "speed" -> speedUpgrades;
            case "stack" -> stackUpgrades;
            case "range" -> rangeUpgrades;
            case "efficiency" -> efficiencyUpgrades;
            default -> 0;
        };
    }

    public boolean removeUpgrade(UpgradeType type) {
        return type != null && removeUpgrade(type.id);
    }

    public boolean removeUpgrade(String type) {
        if (!hasEndpoint()) return false;
        int current = upgradeCount(type);
        if (current <= 0) return false;
        setUpgradeCount(type, current - 1);
        setChanged();
        return true;
    }

    private void setUpgradeCount(String type, int value) {
        int capped = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, value));
        switch (type == null ? "" : type) {
            case "speed" -> speedUpgrades = capped;
            case "stack" -> stackUpgrades = capped;
            case "range" -> rangeUpgrades = capped;
            case "efficiency" -> efficiencyUpgrades = capped;
            default -> { }
        }
    }

    public int[] upgradeCounts() {
        return new int[] { speedUpgrades, stackUpgrades, rangeUpgrades, efficiencyUpgrades };
    }

    public void clearEndpoint() {
        endpointKind = "";
        endpointId = "";
        endpointNumber = 0;
        endpointName = "";
        endpointNetworkId = "";
        endpointFace = Direction.NORTH;
        speedUpgrades = 0;
        stackUpgrades = 0;
        rangeUpgrades = 0;
        efficiencyUpgrades = 0;
        refreshBlockState();
        setChanged();
    }

    private void refreshBlockState() {
        if (level != null && getBlockState().getBlock() instanceof NetworkWireBlock) {
            level.setBlock(worldPosition, NetworkWireBlock.withConnections(level, worldPosition, getBlockState()), 3);
        }
    }

    private void ensureEndpointNumber() {
        if (!hasEndpoint() || endpointNumber > 0) return;
        if (level instanceof ServerLevel serverLevel) {
            endpointNumber = NetworkIdentitySavedData.get(serverLevel).allocateEndpointNumber();
            setChanged();
            // No sendBlockUpdated here; refreshBlockState on install and getUpdatePacket() handle sync.
        }
    }

    private int tickCounter;

    public void tick40() {
        if (++tickCounter >= 40) { tickCounter = 0; refreshConnections(); }
    }

    public void refreshConnections() {
        if (level == null) return;
        int next = 0;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof NetworkWireBlockEntity || neighbor instanceof NetworkModemBlockEntity) {
                next |= bit(direction);
            }
        }
        if (hasEndpoint()) next |= bit(endpointFace());
        if (next != connectionMask) {
            connectionMask = next;
            refreshBlockState();
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("laneCount", laneCount());
        output.putInt("connectionMask", connectionMask);
        output.store("endpointKind", com.mojang.serialization.Codec.STRING, endpointKind == null ? "" : endpointKind);
        output.store("endpointId", com.mojang.serialization.Codec.STRING, endpointId());
        output.putInt("endpointNumber", endpointNumber());
        output.store("endpointName", com.mojang.serialization.Codec.STRING, endpointName == null ? "" : endpointName);
        output.store("endpointNetworkId", com.mojang.serialization.Codec.STRING, endpointNetworkId());
        output.store("endpointFace", Direction.CODEC, endpointFace());
        output.putInt("speedUpgrades", speedUpgrades);
        output.putInt("stackUpgrades", stackUpgrades);
        output.putInt("rangeUpgrades", rangeUpgrades);
        output.putInt("efficiencyUpgrades", efficiencyUpgrades);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        laneCount = Math.max(1, Math.min(MAX_LANES, input.getIntOr("laneCount", 1)));
        connectionMask = input.getIntOr("connectionMask", 0);
        endpointKind = input.read("endpointKind", com.mojang.serialization.Codec.STRING).orElse("");
        endpointId = input.read("endpointId", com.mojang.serialization.Codec.STRING).orElse("");
        endpointNumber = Math.max(0, input.getIntOr("endpointNumber", 0));
        endpointName = input.read("endpointName", com.mojang.serialization.Codec.STRING).orElse("");
        endpointNetworkId = NetworkEndpointBlockEntity.sanitizeNetworkId(input.read("endpointNetworkId", com.mojang.serialization.Codec.STRING).orElse(""));
        endpointFace = input.read("endpointFace", Direction.CODEC).orElse(Direction.NORTH);
        speedUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("speedUpgrades", 0)));
        stackUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("stackUpgrades", 0)));
        rangeUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("rangeUpgrades", 0)));
        efficiencyUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("efficiencyUpgrades", 0)));
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length() && out.length() < 48; i++) {
            char c = trimmed.charAt(i);
            if (!Character.isISOControl(c)) out.append(c);
        }
        return out.toString();
    }

    private String defaultAttachedName() {
        return attachedBaseName() + "#" + formattedEndpointId();
    }

    private String attachedBaseName() {
        String kind = hasRouter() ? "Wireless Router" : "Cable Socket";
        if (level == null) return kind;
        BlockState attachedState = level.getBlockState(attachedPos());
        if (attachedState.isAir()) return kind;
        String blockName = attachedState.getBlock().getName().getString();
        return blockName == null || blockName.isBlank() ? kind : blockName;
    }
}
