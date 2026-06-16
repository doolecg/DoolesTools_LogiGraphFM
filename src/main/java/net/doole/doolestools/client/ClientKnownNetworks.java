package net.doole.doolestools.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientKnownNetworks {
    private static final Map<String, Entry> NETWORKS = new LinkedHashMap<>();

    private ClientKnownNetworks() {}

    public static void remember(String id, String name) {
        if (id == null || id.isBlank()) return;
        NETWORKS.put(id, new Entry(id, name == null || name.isBlank() ? id : name, true));
    }

    public static void replaceAll(List<String> ids, List<String> names, List<Boolean> editable) {
        NETWORKS.clear();
        int n = Math.min(ids.size(), Math.min(names.size(), editable.size()));
        for (int i = 0; i < n; i++) {
            String id = ids.get(i);
            if (id == null || id.isBlank()) continue;
            String name = names.get(i) == null || names.get(i).isBlank() ? id : names.get(i);
            NETWORKS.put(id, new Entry(id, name, editable.get(i)));
        }
    }

    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        out.addAll(NETWORKS.values());
        return out;
    }

    public record Entry(String id, String name, boolean editable) {}
}
