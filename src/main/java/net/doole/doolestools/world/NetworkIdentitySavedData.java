package net.doole.doolestools.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.DoolesTools;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Locale;

/** Server-owned monotonic ID counters for visible network and endpoint numbers. */
public class NetworkIdentitySavedData extends SavedData {
    private static final String NAME = DoolesTools.MOD_ID + "_network_identities";

    public static final Codec<NetworkIdentitySavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("nextNetworkNumber").forGetter(data -> data.nextNetworkNumber),
            Codec.INT.fieldOf("nextEndpointNumber").forGetter(data -> data.nextEndpointNumber)
    ).apply(inst, NetworkIdentitySavedData::new));

    private static final SavedData.Factory<NetworkIdentitySavedData> FACTORY =
            new SavedData.Factory<>(NetworkIdentitySavedData::new, NetworkIdentitySavedData::load, null);

    private int nextNetworkNumber = 1;
    private int nextEndpointNumber = 1;

    public NetworkIdentitySavedData() {}

    private NetworkIdentitySavedData(int nextNetworkNumber, int nextEndpointNumber) {
        this.nextNetworkNumber = Math.max(1, nextNetworkNumber);
        this.nextEndpointNumber = Math.max(1, nextEndpointNumber);
    }

    public static NetworkIdentitySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static NetworkIdentitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new NetworkIdentitySavedData(tag.getInt("nextNetworkNumber"), tag.getInt("nextEndpointNumber"));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("nextNetworkNumber", nextNetworkNumber);
        tag.putInt("nextEndpointNumber", nextEndpointNumber);
        return tag;
    }

    public int allocateNetworkNumber() {
        int value = nextNetworkNumber++;
        setDirty();
        return value;
    }

    public int allocateEndpointNumber() {
        int value = nextEndpointNumber++;
        setDirty();
        return value;
    }

    public static String formatFourDigits(int value) {
        return String.format(Locale.ROOT, "%04d", Math.max(0, value));
    }
}
