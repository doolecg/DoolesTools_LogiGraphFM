package net.doole.doolestools.logistics.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GraphExportData(String format, int version, String exportedAt, LogisticsGraphData graph) {
    public static final String FORMAT = "doolestools:logigraph";
    public static final int VERSION = 1;

    public static final Codec<GraphExportData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("format").forGetter(GraphExportData::format),
            Codec.INT.fieldOf("version").forGetter(GraphExportData::version),
            Codec.STRING.fieldOf("exportedAt").forGetter(GraphExportData::exportedAt),
            LogisticsGraphData.CODEC.fieldOf("graph").forGetter(GraphExportData::graph)
    ).apply(inst, GraphExportData::new));
}
