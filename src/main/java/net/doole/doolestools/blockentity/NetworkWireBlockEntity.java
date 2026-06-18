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

import java.util.ArrayList;
import java.util.List;

public class NetworkWireBlockEntity extends BlockEntity {
    public static final int MAX_LANES = 4;

    private int laneCount = 1;
    private int connectionMask = 0;

    /** One slot per Direction.ordinal() (0=DOWN … 5=EAST). Null entries = no endpoint on that face. */
    private static class EndpointSlot {
        String kind = "";
        String id = "";
        int number;
        String name = "";
        String networkId = "";
        int speedUpgrades, stackUpgrades, rangeUpgrades, efficiencyUpgrades;

        boolean has() { return "modem".equals(kind); }

        void clear() {
            kind = ""; id = ""; number = 0; name = "";
            networkId = ""; speedUpgrades = 0; stackUpgrades = 0;
            rangeUpgrades = 0; efficiencyUpgrades = 0;
        }
    }

    private final EndpointSlot[] slots = new EndpointSlot[Direction.values().length];

    public NetworkWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NETWORK_WIRE.get(), pos, state);
        for (int i = 0; i < slots.length; i++) slots[i] = new EndpointSlot();
    }

    // ── Sync ─────────────────────────────────────────────────────────────────

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

    // ── Wire identity ─────────────────────────────────────────────────────────

    public int laneCount() { return Math.max(1, Math.min(MAX_LANES, laneCount)); }
    public int connectionMask() { return connectionMask; }
    public boolean connects(Direction direction) { return (connectionMask & bit(direction)) != 0; }

    // ── Endpoint query ────────────────────────────────────────────────────────

    public boolean hasEndpoint() {
        for (EndpointSlot slot : slots) if (slot.has()) return true;
        return false;
    }

    public boolean hasEndpointAt(Direction face) {
        return face != null && slots[face.ordinal()].has();
    }

    public boolean hasModem() { return hasEndpoint(); }
    public boolean hasRouter() { return false; }

    /** True if any endpoint face on this wire has a router AND its attached block is {@code pos}. */
    public boolean hasRouterAttachedTo(BlockPos pos) {
        if (!hasRouter()) return false;
        for (Direction dir : Direction.values()) {
            if (hasEndpointAt(dir) && attachedPos(dir).equals(pos)) return true;
        }
        return false;
    }

    /** Sum of upgrade counts of the given type across all endpoint faces whose attached block is {@code pos}. */
    public int upgradeCountForAttachedPos(BlockPos pos, UpgradeType type) {
        int total = 0;
        for (Direction dir : Direction.values()) {
            if (hasEndpointAt(dir) && attachedPos(dir).equals(pos)) total += upgradeCount(dir, type);
        }
        return total;
    }

    public int upgradeCountForAttachedPos(BlockPos pos, String type) {
        return upgradeCountForAttachedPos(pos, UpgradeType.byId(type));
    }

    /** Returns the first occupied face, or NORTH as fallback (backward compat). */
    public Direction endpointFace() {
        for (Direction dir : Direction.values()) if (slots[dir.ordinal()].has()) return dir;
        return Direction.NORTH;
    }

    /** All faces that have an endpoint installed. */
    public List<Direction> endpointFaces() {
        List<Direction> out = new ArrayList<>();
        for (Direction dir : Direction.values()) if (slots[dir.ordinal()].has()) out.add(dir);
        return out;
    }

    /** Attached block position for the given endpoint face. */
    public BlockPos attachedPos(Direction face) {
        return worldPosition.relative(face);
    }

    /** Attached block position for the primary (first) endpoint. */
    public BlockPos attachedPos() {
        return attachedPos(endpointFace());
    }

    // ── Per-face accessors ────────────────────────────────────────────────────

    public String endpointId(Direction face) {
        EndpointSlot slot = slots[face.ordinal()];
        if (slot.id == null || slot.id.isBlank()) {
            slot.id = "modem_" + face.getSerializedName() + "_" + Long.toUnsignedString(worldPosition.asLong());
        }
        ensureEndpointNumber(face);
        return slot.id;
    }

    /** Legacy – primary endpoint id (first face). */
    public String endpointId() { return endpointId(endpointFace()); }

    public int endpointNumber(Direction face) {
        ensureEndpointNumber(face);
        return slots[face.ordinal()].number;
    }

    public String formattedEndpointId(Direction face) {
        return NetworkIdentitySavedData.formatFourDigits(endpointNumber(face));
    }

    /** Legacy – primary endpoint formatted id. */
    public String formattedEndpointId() { return formattedEndpointId(endpointFace()); }

    public String endpointNetworkId(Direction face) {
        String s = slots[face.ordinal()].networkId;
        return s == null ? "" : s;
    }

    /** First occupied face's networkId – used by the scanner's legacy networkMatches check. */
    public String endpointNetworkId() {
        Direction first = endpointFace();
        return endpointNetworkId(first);
    }

    public String endpointName(Direction face) {
        EndpointSlot slot = slots[face.ordinal()];
        if (slot.name == null || slot.name.isBlank()) return defaultAttachedName(face);
        return slot.name;
    }

    /** Primary endpoint name (backward compat). */
    public String endpointName() { return endpointName(endpointFace()); }

    public int[] upgradeCounts(Direction face) {
        EndpointSlot s = slots[face.ordinal()];
        return new int[]{s.speedUpgrades, s.stackUpgrades, s.rangeUpgrades, s.efficiencyUpgrades};
    }

    /** Legacy – primary endpoint upgrade counts. */
    public int[] upgradeCounts() { return upgradeCounts(endpointFace()); }

    public int upgradeCount(Direction face, UpgradeType type) {
        EndpointSlot s = slots[face.ordinal()];
        if (type == null) return 0;
        return switch (type) {
            case SPEED -> s.speedUpgrades;
            case STACK -> s.stackUpgrades;
            case RANGE -> s.rangeUpgrades;
            case EFFICIENCY -> s.efficiencyUpgrades;
        };
    }

    public int upgradeCount(Direction face, String type) { return upgradeCount(face, UpgradeType.byId(type)); }

    /** Legacy – primary endpoint upgrade count. */
    public int upgradeCount(String type) { return upgradeCount(endpointFace(), type); }

    // ── Endpoint mutation ─────────────────────────────────────────────────────

    /**
     * Installs an endpoint on {@code face}. Returns false if that face already has an endpoint,
     * the face is invalid, or kind is not "modem".
     */
    public boolean installEndpoint(String kind, Direction face, String name) {
        if (face == null || !"modem".equals(kind)) return false;
        EndpointSlot slot = slots[face.ordinal()];
        if (slot.has()) return false;
        slot.kind = kind;
        slot.id = "modem_" + face.getSerializedName() + "_" + Long.toUnsignedString(worldPosition.asLong());
        slot.number = 0;
        ensureEndpointNumber(face);
        slot.name = sanitize(name);
        slot.networkId = level instanceof ServerLevel serverLevel
                ? NetworkEndpointBlockEntity.inferNearbyNetwork(serverLevel, worldPosition)
                : "";
        refreshBlockState();
        setChanged();
        return true;
    }

    public boolean installUpgrade(UpgradeType type, Direction face) {
        if (type == null) return false;
        if (!hasEndpointAt(face)) return false;
        EndpointSlot slot = slots[face.ordinal()];
        int current = upgradeCount(face, type);
        if (current >= NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE) return false;
        setUpgradeCount(slot, type, current + 1);
        setChanged();
        return true;
    }

    public boolean installUpgrade(String type, Direction face) {
        return installUpgrade(UpgradeType.byId(type), face);
    }

    /** Legacy – installs on first available endpoint face. */
    public boolean installUpgrade(UpgradeType type) {
        Direction face = endpointFace();
        return hasEndpointAt(face) && installUpgrade(type, face);
    }

    public boolean installUpgrade(String type) {
        Direction face = endpointFace();
        return hasEndpointAt(face) && installUpgrade(UpgradeType.byId(type), face);
    }

    public boolean removeUpgrade(UpgradeType type, Direction face) {
        if (type == null) return false;
        if (!hasEndpointAt(face)) return false;
        EndpointSlot slot = slots[face.ordinal()];
        int current = upgradeCount(face, type);
        if (current <= 0) return false;
        setUpgradeCount(slot, type, current - 1);
        setChanged();
        return true;
    }

    public boolean removeUpgrade(String type, Direction face) {
        return removeUpgrade(UpgradeType.byId(type), face);
    }

    /** Legacy – removes from first available endpoint face. */
    public boolean removeUpgrade(UpgradeType type) {
        Direction face = endpointFace();
        return hasEndpointAt(face) && removeUpgrade(type, face);
    }

    public boolean removeUpgrade(String type) {
        Direction face = endpointFace();
        return hasEndpointAt(face) && removeUpgrade(UpgradeType.byId(type), face);
    }

    public int upgradeCount(UpgradeType type) {
        return type == null ? 0 : upgradeCount(endpointFace(), type);
    }

    private static void setUpgradeCount(EndpointSlot slot, UpgradeType type, int value) {
        int capped = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, value));
        if (type == null) return;
        switch (type) {
            case SPEED -> slot.speedUpgrades = capped;
            case STACK -> slot.stackUpgrades = capped;
            case RANGE -> slot.rangeUpgrades = capped;
            case EFFICIENCY -> slot.efficiencyUpgrades = capped;
        }
    }

    /** Clears the endpoint on {@code face} (returns the modem item separately). */
    public void clearEndpointAt(Direction face) {
        slots[face.ordinal()].clear();
        refreshBlockState();
        setChanged();
    }

    /** Clears ALL endpoints. */
    public void clearEndpoint() {
        for (EndpointSlot slot : slots) slot.clear();
        refreshBlockState();
        setChanged();
    }

    public void setEndpointIdentityFromName(String name) {
        // Apply to all endpoints (shared name for simplicity when no face is specified).
        for (EndpointSlot slot : slots) {
            if (!slot.has()) continue;
            slot.name = NetworkEndpointBlockEntity.sanitize(name);
        }
        setChanged();
    }

    public void setEndpointIdentity(String name, String networkId) {
        String cleanedName = NetworkEndpointBlockEntity.sanitize(name);
        String cleanedNetId = NetworkEndpointBlockEntity.sanitizeNetworkId(networkId);
        for (EndpointSlot slot : slots) {
            if (!slot.has()) continue;
            slot.name = cleanedName;
            slot.networkId = cleanedNetId;
        }
        setChanged();
    }

    /** Sets identity for only the endpoint on the given face. */
    public void setEndpointIdentityAt(Direction face, String name, String networkId) {
        if (!hasEndpointAt(face)) return;
        EndpointSlot slot = slots[face.ordinal()];
        slot.name = NetworkEndpointBlockEntity.sanitize(name);
        slot.networkId = NetworkEndpointBlockEntity.sanitizeNetworkId(networkId);
        setChanged();
    }

    // ── Connection refresh ────────────────────────────────────────────────────

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
        if (next != connectionMask) {
            connectionMask = next;
            refreshBlockState();
            setChanged();
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("laneCount", laneCount());
        output.putInt("connectionMask", connectionMask);
        // Save each occupied slot keyed by direction name.
        for (Direction dir : Direction.values()) {
            EndpointSlot slot = slots[dir.ordinal()];
            if (!slot.has()) continue;
            String pfx = "ep_" + dir.getSerializedName() + "_";
            output.store(pfx + "kind", com.mojang.serialization.Codec.STRING, slot.kind);
            output.store(pfx + "id", com.mojang.serialization.Codec.STRING, slot.id == null ? "" : slot.id);
            output.putInt(pfx + "number", slot.number);
            output.store(pfx + "name", com.mojang.serialization.Codec.STRING, slot.name == null ? "" : slot.name);
            output.store(pfx + "networkId", com.mojang.serialization.Codec.STRING, slot.networkId == null ? "" : slot.networkId);
            output.putInt(pfx + "speed", slot.speedUpgrades);
            output.putInt(pfx + "stack", slot.stackUpgrades);
            output.putInt(pfx + "range", slot.rangeUpgrades);
            output.putInt(pfx + "efficiency", slot.efficiencyUpgrades);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        laneCount = Math.max(1, Math.min(MAX_LANES, input.getIntOr("laneCount", 1)));
        connectionMask = input.getIntOr("connectionMask", 0);

        // Load new multi-endpoint format.
        boolean anyLoaded = false;
        for (Direction dir : Direction.values()) {
            String pfx = "ep_" + dir.getSerializedName() + "_";
            String kind = input.read(pfx + "kind", com.mojang.serialization.Codec.STRING).orElse("");
            if (kind.isBlank()) continue;
            EndpointSlot slot = slots[dir.ordinal()];
            slot.kind = kind;
            slot.id = input.read(pfx + "id", com.mojang.serialization.Codec.STRING).orElse("");
            slot.number = Math.max(0, input.getIntOr(pfx + "number", 0));
            slot.name = input.read(pfx + "name", com.mojang.serialization.Codec.STRING).orElse("");
            slot.networkId = NetworkEndpointBlockEntity.sanitizeNetworkId(
                    input.read(pfx + "networkId", com.mojang.serialization.Codec.STRING).orElse(""));
            slot.speedUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr(pfx + "speed", 0)));
            slot.stackUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr(pfx + "stack", 0)));
            slot.rangeUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr(pfx + "range", 0)));
            slot.efficiencyUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr(pfx + "efficiency", 0)));
            anyLoaded = true;
        }

        // Migration: load old single-endpoint format if no new-format slots were found.
        if (!anyLoaded) {
            String oldKind = input.read("endpointKind", com.mojang.serialization.Codec.STRING).orElse("");
            if (!oldKind.isBlank()) {
                Direction oldFace = input.read("endpointFace", Direction.CODEC).orElse(Direction.NORTH);
                EndpointSlot slot = slots[oldFace.ordinal()];
                slot.kind = oldKind;
                slot.id = input.read("endpointId", com.mojang.serialization.Codec.STRING).orElse("");
                slot.number = Math.max(0, input.getIntOr("endpointNumber", 0));
                slot.name = input.read("endpointName", com.mojang.serialization.Codec.STRING).orElse("");
                slot.networkId = NetworkEndpointBlockEntity.sanitizeNetworkId(
                        input.read("endpointNetworkId", com.mojang.serialization.Codec.STRING).orElse(""));
                slot.speedUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("speedUpgrades", 0)));
                slot.stackUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("stackUpgrades", 0)));
                slot.rangeUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("rangeUpgrades", 0)));
                slot.efficiencyUpgrades = Math.max(0, Math.min(NetworkEndpointBlockEntity.MAX_UPGRADES_PER_TYPE, input.getIntOr("efficiencyUpgrades", 0)));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshBlockState() {
        if (level != null && getBlockState().getBlock() instanceof NetworkWireBlock) {
            level.setBlock(worldPosition, NetworkWireBlock.withConnections(level, worldPosition, getBlockState()), 3);
        }
    }

    private void ensureEndpointNumber(Direction face) {
        EndpointSlot slot = slots[face.ordinal()];
        if (!slot.has() || slot.number > 0) return;
        if (level instanceof ServerLevel serverLevel) {
            slot.number = NetworkIdentitySavedData.get(serverLevel).allocateEndpointNumber();
            setChanged();
        }
    }

    private static int bit(Direction direction) { return 1 << direction.ordinal(); }

    private static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (char c : value.trim().toCharArray()) {
            if (!Character.isISOControl(c) && out.length() < 48) out.append(c);
        }
        return out.toString();
    }

    private String defaultAttachedName(Direction face) {
        String baseName = attachedBaseName(face);
        return baseName + "#" + formattedEndpointId(face);
    }

    private String attachedBaseName(Direction face) {
        String kind = "Cable Socket";
        if (level == null) return kind;
        BlockState attachedState = level.getBlockState(attachedPos(face));
        if (attachedState.isAir()) return kind;
        String blockName = attachedState.getBlock().getName().getString();
        return blockName == null || blockName.isBlank() ? kind : blockName;
    }
}
