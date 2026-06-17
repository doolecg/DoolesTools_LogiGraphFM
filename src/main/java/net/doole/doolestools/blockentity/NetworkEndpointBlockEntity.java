package net.doole.doolestools.blockentity;

import net.doole.doolestools.block.NetworkEndpointBlock;
import net.doole.doolestools.world.NetworkIdentitySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public abstract class NetworkEndpointBlockEntity extends BlockEntity {
    public static final int MAX_UPGRADES_PER_TYPE = 4;

    private String deviceId = "";
    private int deviceNumber;
    private String deviceName = "";
    private String networkId = "";
    private int speedUpgrades;
    private int stackUpgrades;
    private int rangeUpgrades;
    private int efficiencyUpgrades;

    protected NetworkEndpointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract String deviceKind();

    // Allocate the numeric id the moment the device loads on the server (i.e. on placement), so it
    // reads a real id straight away rather than 0000 until someone opens the naming screen.
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ensureDeviceNumber();
            if (networkId().isBlank()) setNetworkId(inferNearbyNetwork(serverLevel, worldPosition));
        }
    }

    // Sync identity/upgrade state to the client so the naming screen and holograms show real values.
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

    public String deviceId() {
        if (deviceId == null || deviceId.isBlank()) deviceId = defaultDeviceId();
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
        if (deviceName == null || deviceName.isBlank()) return defaultAttachedName();
        return deviceName;
    }

    public void setDeviceName(String name) {
        this.deviceName = sanitize(name);
        setChanged();
    }

    public void setNetworkId(String networkId) {
        this.networkId = sanitizeNetworkId(networkId);
        setChanged();
    }

    public void setIdentity(String name, String networkId) {
        this.deviceName = sanitize(name);
        setNetworkId(networkId);
    }

    public void setIdentityFromName(String name) {
        String cleaned = sanitize(name);
        this.deviceName = cleaned;
        setChanged();
    }

    private void ensureDeviceNumber() {
        if (deviceNumber > 0) return;
        if (level instanceof ServerLevel serverLevel) {
            deviceNumber = NetworkIdentitySavedData.get(serverLevel).allocateEndpointNumber();
            setChanged();
            // No sendBlockUpdated here: this runs during onLoad/saveAdditional, and a re-entrant block
            // update (with neighbour notifications) corrupts placement/break sync. getUpdatePacket()
            // already pushes the freshly-allocated id to clients when the block is placed.
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
            case "stack" -> stackUpgrades;
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
            case "stack" -> stackUpgrades = capped;
            case "range" -> rangeUpgrades = capped;
            case "efficiency" -> efficiencyUpgrades = capped;
            default -> { }
        }
    }

    public int[] upgradeCounts() {
        return new int[] { speedUpgrades, stackUpgrades, rangeUpgrades, efficiencyUpgrades };
    }

    public BlockPos attachedPos() {
        return NetworkEndpointBlock.attachedPos(worldPosition, getBlockState());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("deviceId", com.mojang.serialization.Codec.STRING, deviceId());
        output.putInt("deviceNumber", deviceNumber());
        output.store("deviceName", com.mojang.serialization.Codec.STRING, deviceName());
        output.store("networkId", com.mojang.serialization.Codec.STRING, networkId());
        output.putInt("speedUpgrades", speedUpgrades);
        output.putInt("stackUpgrades", stackUpgrades);
        output.putInt("rangeUpgrades", rangeUpgrades);
        output.putInt("efficiencyUpgrades", efficiencyUpgrades);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.deviceId = input.read("deviceId", com.mojang.serialization.Codec.STRING).orElse("");
        this.deviceNumber = Math.max(0, input.getIntOr("deviceNumber", 0));
        this.deviceName = input.read("deviceName", com.mojang.serialization.Codec.STRING).orElse("");
        this.networkId = sanitizeNetworkId(input.read("networkId", com.mojang.serialization.Codec.STRING).orElse(""));
        this.speedUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("speedUpgrades", 0)));
        this.stackUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("stackUpgrades", 0)));
        this.rangeUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("rangeUpgrades", 0)));
        this.efficiencyUpgrades = Math.max(0, Math.min(MAX_UPGRADES_PER_TYPE, input.getIntOr("efficiencyUpgrades", 0)));
    }

    private String defaultDeviceId() {
        return deviceKind().toLowerCase(java.util.Locale.ROOT).replace(' ', '_') + "_" + Long.toUnsignedString(worldPosition.asLong());
    }

    /** Default nickname: the attached block's name plus the device id, e.g. {@code Chest#0001}. */
    private String defaultAttachedName() {
        return attachedBaseName() + "#" + formattedDeviceId();
    }

    private String attachedBaseName() {
        if (level == null) return deviceKind();
        BlockState attachedState = level.getBlockState(attachedPos());
        if (attachedState.isAir()) return deviceKind();
        String blockName = attachedState.getBlock().getName().getString();
        return blockName == null || blockName.isBlank() ? deviceKind() : blockName;
    }

    public static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String trimmed = value.trim();
        int i = 0;
        while (i < trimmed.length() && out.length() < 48) {
            int cp = trimmed.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isISOControl(cp)) out.appendCodePoint(cp);
        }
        return out.toString();
    }

    public static String slug(String value) {
        String cleaned = sanitize(value).toLowerCase(java.util.Locale.ROOT);
        StringBuilder out = new StringBuilder();
        boolean lastUnderscore = false;
        for (int i = 0; i < cleaned.length() && out.length() < 48; i++) {
            char c = cleaned.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (ok) {
                out.append(c);
                lastUnderscore = false;
            } else if (!lastUnderscore && out.length() > 0) {
                out.append('_');
                lastUnderscore = true;
            }
        }
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == '_') out.setLength(len - 1);
        return out.isEmpty() ? "endpoint" : out.toString();
    }

    public static String sanitizeNetworkId(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        String cleaned = value.trim().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < cleaned.length() && out.length() < 64; i++) {
            char c = cleaned.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') out.append(c);
        }
        return out.toString();
    }

    public static String inferNearbyNetwork(ServerLevel level, BlockPos center) {
        String found = "";
        int matches = 0;
        int radius = 32;
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!(level.getChunk(chunkX, chunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof LogisticsComputerBlockEntity computer)) continue;
                    if (center.distSqr(computer.getBlockPos()) > radius * radius) continue;
                    String id = computer.networkId();
                    if (id.isBlank()) continue;
                    if (!id.equals(found)) {
                        found = id;
                        matches++;
                        if (matches > 1) return "";
                    }
                }
            }
        }
        return matches == 1 ? found : "";
    }
}
