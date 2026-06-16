package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.doole.doolestools.logistics.NodeType;

import java.util.List;

/**
 * A node placed on the flowgraph canvas. Mutable position/name fields are stored as a record but
 * the editor replaces instances on edit to keep the data immutable in transit.
 */
public record GraphNodeData(String nodeId,
                            String scannedBlockId,
                            String displayName,
                            NodeType type,
                            int x,
                            int y,
                             int width,
                             int height,
                             List<GraphPortData> ports,
                             boolean collapsed,
                             String notes) {

    public static final int DEFAULT_WIDTH = 132;
    public static final int DEFAULT_HEIGHT = 84;

    public static final Codec<GraphNodeData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("nodeId").forGetter(GraphNodeData::nodeId),
            Codec.STRING.fieldOf("scannedBlockId").forGetter(GraphNodeData::scannedBlockId),
            Codec.STRING.fieldOf("displayName").forGetter(GraphNodeData::displayName),
            NodeType.CODEC.fieldOf("type").forGetter(GraphNodeData::type),
            Codec.INT.fieldOf("x").forGetter(GraphNodeData::x),
            Codec.INT.fieldOf("y").forGetter(GraphNodeData::y),
            Codec.INT.fieldOf("width").forGetter(GraphNodeData::width),
            Codec.INT.fieldOf("height").forGetter(GraphNodeData::height),
            GraphPortData.CODEC.listOf().optionalFieldOf("ports", List.of()).forGetter(GraphNodeData::ports),
            Codec.BOOL.fieldOf("collapsed").forGetter(GraphNodeData::collapsed),
            Codec.STRING.fieldOf("notes").forGetter(GraphNodeData::notes)
    ).apply(inst, GraphNodeData::new));

    public GraphNodeData(String nodeId, String scannedBlockId, String displayName, NodeType type,
                         int x, int y, int width, int height, boolean collapsed, String notes) {
        this(nodeId, scannedBlockId, displayName, type, x, y, width, height, List.of(), collapsed, notes);
    }

    public GraphNodeData withPosition(int newX, int newY) {
        return new GraphNodeData(nodeId, scannedBlockId, displayName, type, newX, newY, width, height, ports, collapsed, notes);
    }

    public GraphNodeData withName(String newName) {
        return new GraphNodeData(nodeId, scannedBlockId, newName, type, x, y, width, height, ports, collapsed, notes);
    }

    public GraphNodeData withType(NodeType newType) {
        return new GraphNodeData(nodeId, scannedBlockId, displayName, newType, x, y, width, height, ports, collapsed, notes);
    }

    public GraphNodeData withNotes(String newNotes) {
        return new GraphNodeData(nodeId, scannedBlockId, displayName, type, x, y, width, height, ports, collapsed, newNotes);
    }

    public GraphNodeData withPorts(List<GraphPortData> newPorts) {
        return new GraphNodeData(nodeId, scannedBlockId, displayName, type, x, y, width, height, List.copyOf(newPorts), collapsed, notes);
    }

    public GraphPortData findPort(String portId) {
        for (GraphPortData port : ports) {
            if (port.portId().equals(portId)) return port;
        }
        return null;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }
}
