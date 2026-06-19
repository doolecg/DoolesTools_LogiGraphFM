package net.doole.doolestools.blockentity;

import net.doole.doolestools.item.UpgradeType;
import net.doole.doolestools.util.NetworkIdentityUtils;
import net.doole.doolestools.util.ValueInput;
import net.doole.doolestools.util.ValueOutput;
import net.doole.doolestools.world.NetworkIdentitySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for all network-connected devices that carry an identity, a network membership, and
 * optional upgrade cards. Handles persistence, client sync, and upgrade logic; subclasses supply
 * the on-disk key names (for save compat) and their own load/removal hooks.
 */
public abstract class NetworkDeviceBlockEntity extends BlockEntity {

    public static final int MAX_UPGRADES_PER_TYPE = 4;

    private String deviceId = "";
    private int deviceNumber;
    private String deviceName = "";
    private String networkId = "";
    private int speedUpgrades;
    private int stackUpgrades;
    private int rangeUpgrades;
    private int efficiencyUpgrades;

    protected NetworkDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- Abstract contract ---

    /** NBT key for the device id string (e.g. {@code "deviceId"} or {@code "relayId"}). */
    protected abstract String idKey();

    /** NBT key for the numeric device number (e.g. {@code "deviceNumber"} or {@code "relayNumber"}). */
    protected abstract String numberKey();

    /** NBT key for the display name string (e.g. {@code "deviceName"} or {@code "displayName"}). */
    protected abstract String nameKey();

    /** Whether this device type accepts the given upgrade. Relay returns false for STACK. */
    protected abstract boolean supportsUpgrade(UpgradeType type);

    /** Fallback id when no id has been persisted yet. */
    protected abstract String defaultId();

    /** Fallback display name when no name has been set. */
    protected abstract String defaultName();

    // --- Identity ---

    /** Allocate the device number on first server-load so it displays a real id immediately. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) ensureDeviceNumber();
    }

    private void ensureDeviceNumber() {
        if (deviceNumber > 0) return;
        if (level instanceof ServerLevel serverLevel) {
            deviceNumber = NetworkIdentitySavedData.get(serverLevel).allocateEndpointNumber();
            setChanged();
            // No sendBlockUpdated here: runs during onLoad/save; getUpdatePacket() syncs on placement.
        }
    }

    public String deviceId() {
        if (deviceId == null || deviceId.isBlank()) deviceId = defaultId();
        ensureDeviceNumber();
        return deviceId;
    }

    public int deviceNumber() {
        ensureDeviceNumber();
        return deviceNumber;
    }

    public String formattedDeviceId() {
        return NetworkIdentitySavedData.formatFourDigits(deviceNumber());
    }

    public String networkId() {
        return networkId == null ? "" : networkId;
    }

    public String deviceName() {
        if (deviceName == null || deviceName.isBlank()) return defaultName();
        return deviceName;
    }

    public void setDeviceName(String name) {
        this.deviceName = NetworkIdentityUtils.sanitize(name);
        setChanged();
    }

    public void setNetworkId(String networkId) {
        this.networkId = NetworkIdentityUtils.sanitizeNetworkId(networkId);
        setChanged();
    }

    public void setIdentity(String name, String networkId) {
        this.deviceName = NetworkIdentityUtils.sanitize(name);
        setNetworkId(networkId);
    }

    // --- Upgrades ---

    public boolean installUpgrade(UpgradeType type) {
        if (type == null || !supportsUpgrade(type)) return false;
        int current = upgradeCount(type);
        if (current >= MAX_UPGRADES_PER_TYPE) return false;
        setUpgradeCount(type, current + 1);
        setChanged();
        return true;
    }

    public boolean installUpgrade(String type) {
        UpgradeType ut = UpgradeType.byId(type);
        return ut != null && installUpgrade(ut);
    }

    public boolean removeUpgrade(UpgradeType type) {
        if (type == null || !supportsUpgrade(type)) return false;
        int current = upgradeCount(type);
        if (current <= 0) return false;
        setUpgradeCount(type, current - 1);
        setChanged();
        return true;
    }

    public boolean removeUpgrade(String type) {
        UpgradeType ut = UpgradeType.byId(type);
        return ut != null && removeUpgrade(ut);
    }

    public int upgradeCount(UpgradeType type) {
        if (type == null || !supportsUpgrade(type)) return 0;
        return switch (type) {
            case SPEED -> speedUpgrades;
            case STACK -> stackUpgrades;
            case RANGE -> rangeUpgrades;
            case EFFICIENCY -> efficiencyUpgrades;
        };
    }

    public int upgradeCount(String type) {
        UpgradeType ut = UpgradeType.byId(type);
        return ut == null ? 0 : upgradeCount(ut);
    }

    public int[] upgradeCounts() {
        return new int[]{
            speedUpgrades,
            supportsUpgrade(UpgradeType.STACK) ? stackUpgrades : -1,
            rangeUpgrades,
            efficiencyUpgrades
        };
    }

    private void setUpgradeCount(UpgradeType type, int value) {
        int capped = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, value));
        switch (type) {
            case SPEED      -> speedUpgrades      = capped;
            case STACK      -> stackUpgrades      = capped;
            case RANGE      -> rangeUpgrades      = capped;
            case EFFICIENCY -> efficiencyUpgrades = capped;
        }
    }

    // --- Client sync ---

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // --- Persistence --- save keys stay byte-for-byte identical to the original files ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(new ValueOutput(tag, registries));
    }

    private void saveData(ValueOutput output) {
        output.store(idKey(),     com.mojang.serialization.Codec.STRING, deviceId());
        output.putInt(numberKey(), deviceNumber());
        output.store(nameKey(),   com.mojang.serialization.Codec.STRING, deviceName == null ? "" : deviceName);
        output.store("networkId", com.mojang.serialization.Codec.STRING, networkId());
        output.putInt("speedUpgrades",      speedUpgrades);
        if (supportsUpgrade(UpgradeType.STACK)) output.putInt("stackUpgrades", stackUpgrades);
        output.putInt("rangeUpgrades",      rangeUpgrades);
        output.putInt("efficiencyUpgrades", efficiencyUpgrades);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(new ValueInput(tag, registries));
    }

    private void loadData(ValueInput input) {
        deviceId    = input.read(idKey(),     com.mojang.serialization.Codec.STRING).orElse("");
        deviceNumber = Math.max(0, input.getIntOr(numberKey(), 0));
        deviceName  = input.read(nameKey(),   com.mojang.serialization.Codec.STRING).orElse("");
        networkId   = NetworkIdentityUtils.sanitizeNetworkId(
                          input.read("networkId", com.mojang.serialization.Codec.STRING).orElse(""));
        speedUpgrades      = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("speedUpgrades",      0)));
        if (supportsUpgrade(UpgradeType.STACK))
            stackUpgrades  = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("stackUpgrades",     0)));
        rangeUpgrades      = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("rangeUpgrades",      0)));
        efficiencyUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("efficiencyUpgrades", 0)));
    }
}
