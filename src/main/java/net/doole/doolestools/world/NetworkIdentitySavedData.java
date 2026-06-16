package net.doole.doolestools.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.DoolesTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Locale;

/** Server-owned monotonic ID counters for visible network and endpoint numbers. */
public class NetworkIdentitySavedData extends SavedData {
    private static final String NAME = DoolesTools.MOD_ID + "_network_identities";

    public static final Codec<NetworkIdentitySavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("nextNetworkNumber").forGetter(data -> data.nextNetworkNumber),
            Codec.INT.fieldOf("nextEndpointNumber").forGetter(data -> data.nextEndpointNumber)
    ).apply(inst, NetworkIdentitySavedData::new));

    public static final SavedDataType<NetworkIdentitySavedData> TYPE =
            new SavedDataType<>(DoolesTools.id(NAME), NetworkIdentitySavedData::new, CODEC);

    private int nextNetworkNumber = 1;
    private int nextEndpointNumber = 1;

    public NetworkIdentitySavedData() {}

    private NetworkIdentitySavedData(int nextNetworkNumber, int nextEndpointNumber) {
        this.nextNetworkNumber = Math.max(1, nextNetworkNumber);
        this.nextEndpointNumber = Math.max(1, nextEndpointNumber);
    }

    public static NetworkIdentitySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
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
