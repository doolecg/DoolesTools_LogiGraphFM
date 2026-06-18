package net.doole.doolestools.client.gui;

import net.doole.doolestools.client.ClientPrefs;
import net.doole.doolestools.client.EditorContext;
import net.doole.doolestools.logistics.ThroughputPlanner;
import net.doole.doolestools.logistics.WarningGenerator;
import net.doole.doolestools.logistics.data.NetworkPowerData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Renders the STATS page: power status, history graph, breakdown, inventory watch. */
public final class StatsPagePanel {

    private static final String[] SCALE_LABELS = {"NOW", "30M", "1H", "12H", "1D", "ALL"};

    private final EditorContext ctx;
    private final Font font;

    // Layout — set by the screen before render/initButtons.
    private int leftPos, contentY, panelX, panelW;

    private int timeScale = 0;
    private int graphScrollOffset = 0;
    private final TerminalButton[] scaleButtons = new TerminalButton[SCALE_LABELS.length];

    public StatsPagePanel(EditorContext ctx, Font font) {
        this.ctx = ctx;
        this.font = font;
    }

    public void updateLayout(int leftPos, int contentY, int panelX, int panelW) {
        this.leftPos = leftPos;
        this.contentY = contentY;
        this.panelX = panelX;
        this.panelW = panelW;
    }

    /**
     * Create the time-scale tab buttons and pass each to {@code addWidget}. Call from the screen's
     * {@code init()} after {@link #updateLayout} has been called.
     */
    public void initButtons(IntConsumer setTimeScale, Consumer<TerminalButton> addWidget) {
        int x = statsContentX();
        int y = statsTabStripY();
        int bw = 34, gap = 3, h = 12;
        for (int i = 0; i < SCALE_LABELS.length; i++) {
            final int idx = i;
            TerminalButton b = new TerminalButton(x + i * (bw + gap), y, bw, h,
                    Component.literal(SCALE_LABELS[i]), () -> setTimeScale(idx, setTimeScale))
                    .accent(DUTheme.SELECTED);
            b.setToggled(i == timeScale);
            scaleButtons[i] = b;
            addWidget.accept(b);
        }
    }

    private void setTimeScale(int scale, IntConsumer onScaleChanged) {
        timeScale = Math.max(0, Math.min(SCALE_LABELS.length - 1, scale));
        graphScrollOffset = 0;
        for (int i = 0; i < scaleButtons.length; i++) {
            if (scaleButtons[i] != null) scaleButtons[i].setToggled(i == timeScale);
        }
        onScaleChanged.accept(timeScale);
    }

    public void scrollGraph(double delta) {
        graphScrollOffset = Math.max(0, graphScrollOffset - (int) Math.signum(delta));
    }

    public void render(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        List<ScannedBlockData> scan = ctx.scan();
        int warnings = 0, errors = 0;
        for (ScannedBlockData s : scan) {
            for (var wd : s.warnings()) {
                if (wd.severity() == WarningData.Severity.ERROR) errors++;
                else if (wd.severity() == WarningData.Severity.WARNING) warnings++;
            }
        }

        NetworkPowerData power = ctx.power();
        int cx = statsContentX();
        int fullContentW = (x + w) - cx - 16;

        int leftColW  = fullContentW * 57 / 100;
        int rightColX = cx + leftColW + 14;
        int rightColW = fullContentW - leftColW - 14;

        // ── LEFT COLUMN ──────────────────────────────────────────────────────
        int statusColor = power.starved() ? DUTheme.ERROR : power.degraded() ? DUTheme.WARN : DUTheme.OK;
        String statusText = power.starved() ? "NO POWER — STOPPED"
                : power.degraded() ? "LOW POWER — " + Math.round(power.satisfaction() * 100) + "%"
                : "NETWORK SATISFIED";
        DUTheme.dot(g, cx, contentY + 6, statusColor);
        g.text(font, statusText, cx + 8, contentY + 4, statusColor, false);

        int barsTop = statsBarsTop();
        int peak = Math.max(1, Math.max(power.supplyCentiFe(), power.demandCentiFe()));
        statBar(g, cx, barsTop,      leftColW, "Production",  powerRatio(power.supplyCentiFe(), peak), formatFe(power.supplyCentiFe()) + "/t", DUTheme.OK);
        statBar(g, cx, barsTop + 12, leftColW, "Consumption", powerRatio(power.demandCentiFe(), peak), formatFe(power.demandCentiFe()) + "/t", DUTheme.ERROR);

        int graphY = statsGraphTop();
        int graphH = 58;
        renderPowerHistoryGraph(g, cx, graphY, leftColW, graphH);

        int breakY = graphY + graphH + 8;
        g.text(font, "BREAKDOWN", cx, breakY, DUTheme.TEXT_GREEN, false);
        int ly = breakY + 12;
        ly = statSmall(g, cx, ly, leftColW, "Computer",   formatFe(power.computerCentiFe())  + " cF/t");
        ly = statSmall(g, cx, ly, leftColW, "Sockets",    formatFe(power.endpointCentiFe())  + " cF/t");
        ly = statSmall(g, cx, ly, leftColW, "Cable",      formatFe(power.wireCentiFe())       + " cF/t");
        ly = statSmall(g, cx, ly, leftColW, "Devices",    formatFe(power.deviceCentiFe())    + " cF/t");
        ly = statSmall(g, cx, ly, leftColW, "Routes",     formatFe(power.routeCentiFe())     + " cF/t");
        ly = statSmall(g, cx, ly, leftColW, "Batteries",  formatFe(power.batteryCentiFe())   + " cF/t");

        // ── RIGHT COLUMN ─────────────────────────────────────────────────────
        int ry = contentY + 4;

        g.text(font, "CAPACITIES", rightColX, ry, DUTheme.TEXT_GREEN, false); ry += 12;
        if (power.batteryCapacity() > 0) {
            float storedFrac = Math.max(0f, Math.min(1f, (float) power.batteryStored() / power.batteryCapacity()));
            int battColor = storedFrac > 0.6f ? 0xFF3af0a0 : storedFrac >= 0.3f ? 0xFFf09030 : 0xFFf03030;
            DUTheme.horizBattery(g, rightColX, ry, rightColW - 2, 14, storedFrac, battColor);
            int pctVal = Math.round(storedFrac * 100f);
            g.text(font, pctVal + "%  " + formatMfe(power.batteryStored()) + " / " + formatMfe(power.batteryCapacity()),
                    rightColX, ry + 16, DUTheme.TEXT_DIM, false);
            ry += 30;
            if (power.batteryCount() > 0) {
                int miniW = 12, miniH = 24, miniGap = 3;
                int perRow = Math.max(1, (rightColW - 2) / (miniW + miniGap));
                int gridRows = (power.batteryCount() + perRow - 1) / perRow;
                for (int bi = 0; bi < power.batteryCount() && bi < 24; bi++) {
                    DUTheme.vertBattery(g,
                            rightColX + (bi % perRow) * (miniW + miniGap),
                            ry + (bi / perRow) * (miniH + 4),
                            miniW, miniH, storedFrac, battColor);
                }
                ry += gridRows * (miniH + 4) + 4;
            }
        } else {
            g.text(font, "No batteries", rightColX, ry, DUTheme.TEXT_DIM, false); ry += 14;
        }

        ry += 2;
        g.text(font, "NETWORK", rightColX, ry, DUTheme.TEXT_GREEN, false); ry += 12;
        ry = stat(g, rightColX, ry, "Generators", String.valueOf(power.generatorCount()));
        ry = stat(g, rightColX, ry, "Batteries",  String.valueOf(power.batteryCount()));
        ry = stat(g, rightColX, ry, "Sockets",    String.valueOf(power.endpointCount()));
        ry = stat(g, rightColX, ry, "Devices",    String.valueOf(power.deviceCount()));

        ry += 4;
        g.text(font, "ALERTS", rightColX, ry, DUTheme.TEXT_GREEN, false); ry += 12;
        if (warnings == 0 && errors == 0) {
            DUTheme.dot(g, rightColX, ry + 2, DUTheme.OK);
            g.text(font, "No issues", rightColX + 8, ry + 1, DUTheme.TEXT_DIM, false);
            ry += 12;
        } else {
            if (warnings > 0) { Glyphs.warning(g, rightColX, ry, DUTheme.WARN);  g.text(font, warnings + " warnings", rightColX + 12, ry + 1, DUTheme.WARN, false);  ry += 11; }
            if (errors > 0)   { Glyphs.warning(g, rightColX, ry, DUTheme.ERROR); g.text(font, errors   + " errors",   rightColX + 12, ry + 1, DUTheme.ERROR, false); ry += 11; }
        }

        ry += 2;
        g.text(font, "ACTIVITY", rightColX, ry, DUTheme.TEXT_GREEN, false); ry += 12;
        boolean routing = power.powered() && ClientPrefs.autoRefresh;
        g.text(font, "Routes",  rightColX,      ry, DUTheme.TEXT_DIM,  false);
        g.text(font, String.valueOf(power.routeCount()), rightColX + 60, ry, DUTheme.TEXT, false);
        g.text(font, routing ? "● LIVE" : "○ IDLE", rightColX + 80, ry, routing ? DUTheme.OK : DUTheme.DISABLED, false);
        ry += 10;
        g.text(font, "Devices", rightColX,      ry, DUTheme.TEXT_DIM,  false);
        g.text(font, String.valueOf(power.deviceCount()), rightColX + 60, ry, DUTheme.TEXT, false);
        ry += 14;

        ThroughputPlanner.PlannerResult plan = ThroughputPlanner.analyse(ctx.graph(), scan);
        g.text(font, "PLANNER", rightColX, ry, DUTheme.TEXT_GREEN, false); ry += 12;
        ry = stat(g, rightColX, ry, "Bottlenecks", String.valueOf(plan.totalBottlenecks()));
        ry = stat(g, rightColX, ry, "Starved",     String.valueOf(plan.totalStarved()));
        if (!plan.links().isEmpty()) {
            ThroughputPlanner.LinkAnalysis first = plan.links().get(0);
            String summary = first.sourceName() + " -> " + first.targetName() + " " + first.capacityPerMin() + "/min";
            g.text(font, trim(font, summary, rightColW), rightColX, ry, first.isBottleneck() ? DUTheme.WARN : DUTheme.TEXT_DIM, false);
        }

        // ── INVENTORY WATCH (full-width, below both columns) ─────────────────
        int sepY = Math.max(ly, ry) + 8;
        g.fill(cx, sepY, x + w - 16, sepY + 1, DUTheme.PANEL_BORDER);
        sepY += 6;

        List<ScannedBlockData> invBlocks = new ArrayList<>();
        for (ScannedBlockData s : scan) if (s.inventory().hasData()) invBlocks.add(s);
        invBlocks.sort(java.util.Comparator.comparingInt(s -> -s.inventory().fillPercent()));

        if (!invBlocks.isEmpty()) {
            g.text(font, "INVENTORY WATCH", cx, sepY, DUTheme.TEXT_GREEN, false); sepY += 12;
            int colGap = 8;
            int entryW = (fullContentW - colGap) / 2;
            int nameW = 80, suffixW = 30;
            int invBarW = Math.max(20, entryW - nameW - suffixW - 6);
            int panelBottom = y + h - 4;
            int col0Y = sepY, col1Y = sepY;
            for (int i = 0; i < invBlocks.size(); i++) {
                ScannedBlockData s = invBlocks.get(i);
                boolean rightSide = (i % 2 == 1);
                int entX = rightSide ? cx + entryW + colGap : cx;
                int entY = rightSide ? col1Y : col0Y;
                if (entY + 9 > panelBottom) break;
                int pct = s.inventory().fillPercent();
                boolean isFull = s.inventory().usedSlots() >= s.inventory().totalSlots();
                boolean isStorage = s.isStorageLike();
                int barColor  = isFull ? DUTheme.ERROR : pct >= WarningGenerator.NEARLY_FULL_PERCENT ? DUTheme.WARN : DUTheme.OK;
                int nameColor = isFull && isStorage ? DUTheme.WARN : DUTheme.TEXT_DIM;
                String name = s.blockName();
                while (font.width(name) > nameW && name.length() > 3) name = name.substring(0, name.length() - 1);
                g.text(font, name, entX, entY, nameColor, false);
                int bx = entX + nameW + 2;
                DUTheme.progress(g, bx, entY, invBarW, 7, pct / 100f, barColor);
                for (int seg = 1; seg < 5; seg++) {
                    int sx = bx + 1 + (invBarW - 2) * seg / 5;
                    g.fill(sx, entY + 1, sx + 1, entY + 6, 0x66000000);
                }
                String suffix = isFull ? "FULL" : pct + "%";
                g.text(font, suffix, bx + invBarW + 3, entY, isFull && isStorage ? DUTheme.WARN : DUTheme.TEXT_DIM, false);
                if (rightSide) col1Y += 10; else col0Y += 10;
            }
        }
    }

    // --- Private helpers ---

    private void statBar(GuiGraphicsExtractor g, int x, int y, int w, String label, float frac, String value, int color) {
        g.text(font, label, x, y, DUTheme.TEXT_DIM, false);
        int labelW = 80;
        int valW = font.width(value) + 4;
        int barW = Math.max(30, w - labelW - valW - 4);
        int barX = x + labelW;
        DUTheme.progress(g, barX, y, barW, 8, frac, color);
        for (int i = 1; i < 4; i++) {
            int sx = barX + 1 + (barW - 2) * i / 4;
            g.fill(sx, y + 1, sx + 1, y + 7, 0x66000000);
        }
        g.text(font, value, barX + barW + 4, y, DUTheme.TEXT, false);
    }

    private void renderPowerHistoryGraph(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        DUTheme.box(g, x, y, w, h, DUTheme.PANEL_ALT, DUTheme.PANEL_BORDER);
        g.text(font, "POWER HISTORY (" + SCALE_LABELS[timeScale] + ")", x + 5, y + 5, DUTheme.TEXT_GREEN, false);
        List<? extends Number> supply = supplySeries();
        List<? extends Number> demand = demandSeries();
        int max = 1;
        for (Number v : supply) max = Math.max(max, v.intValue());
        for (Number v : demand) max = Math.max(max, v.intValue());
        int gx = x + 5, gy = y + 18, gw = w - 10, gh = h - 26;
        g.fill(gx, gy, gx + gw, gy + gh, 0xFF070b08);
        for (int i = 1; i < 4; i++) {
            int ly = gy + i * gh / 4;
            g.fill(gx, ly, gx + gw, ly + 1, 0x332c3a2a);
        }
        int seriesMax = Math.max(supply == null ? 0 : supply.size(), demand == null ? 0 : demand.size());
        if (timeScale != 0 && seriesMax < 2) {
            g.centeredText(font, "Accumulating data…", gx + gw / 2, gy + gh / 2 - 4, DUTheme.TEXT_DIM);
            return;
        }
        int maxOffset = Math.max(0, seriesMax - 1);
        if (graphScrollOffset > maxOffset) graphScrollOffset = maxOffset;
        drawPowerSeries(g, supply, gx, gy, gw, gh, max, DUTheme.OK);
        drawPowerSeries(g, demand, gx, gy, gw, gh, max, powerColor());
        if (graphScrollOffset < maxOffset) g.text(font, "◀", gx + 1, gy + gh / 2 - 4, DUTheme.TEXT_DIM, false);
        if (graphScrollOffset > 0)        g.text(font, "▶", gx + gw - 7, gy + gh / 2 - 4, DUTheme.TEXT_DIM, false);
        g.text(font, "Prod", x + 5, y + h - 8, DUTheme.OK, false);
        g.text(font, "Cons", x + 44, y + h - 8, powerColor(), false);
        String top = formatFe(max) + "/t";
        g.text(font, top, x + w - 5 - font.width(top), y + 5, DUTheme.TEXT_DIM, false);
    }

    private void drawPowerSeries(GuiGraphicsExtractor g, List<? extends Number> values, int x, int y, int w, int h, int max, int color) {
        if (values == null || values.size() < 2) return;
        int count = Math.min(values.size(), w);
        int start = Math.max(0, values.size() - count - graphScrollOffset);
        int lastX = x;
        int lastY = y + h - Math.round(values.get(start).intValue() / (float) max * h);
        for (int i = 1; i < count; i++) {
            int px = x + Math.round(i * (w - 1) / (float) Math.max(1, count - 1));
            int py = y + h - Math.round(values.get(start + i).intValue() / (float) max * h);
            DUTheme.line(g, lastX, lastY, px, py, color);
            lastX = px; lastY = py;
        }
    }

    private int stat(GuiGraphicsExtractor g, int x, int y, String key, String value) {
        g.text(font, key, x, y, DUTheme.TEXT_DIM, false);
        g.text(font, value, x + 80, y, DUTheme.TEXT, false);
        return y + 11;
    }

    private int statSmall(GuiGraphicsExtractor g, int x, int y, int w, String key, String value) {
        g.text(font, key, x, y, DUTheme.TEXT_DIM, false);
        g.text(font, value, x + w - font.width(value), y, DUTheme.TEXT, false);
        return y + 10;
    }

    // --- Layout ---

    public int statsTabStripY()  { return statsBarsTop() + 2 * 12 + 6; }
    private int statsContentX()  { return leftPos + 10 + 16; }
    private int statsBarsTop()   { return contentY + 18; }
    private int statsGraphTop()  { return statsTabStripY() + 16; }

    /** The scroll-graph area spans from statsGraphTop to bottom of the graph rect. */
    public boolean graphContains(double mx, double my) {
        int gx = statsContentX(), gy = statsGraphTop();
        return mx >= gx && mx < gx + panelW && my >= gy && my < gy + 58;
    }

    // --- Series helpers ---

    private List<? extends Number> supplySeries() {
        return switch (timeScale) {
            case 1 -> ctx.supply30m();
            case 2 -> ctx.supply1h();
            case 3 -> ctx.supply12h();
            case 4 -> ctx.supply1d();
            case 5 -> ctx.supplyAllTime();
            default -> ctx.powerSupplyHistory();
        };
    }

    private List<? extends Number> demandSeries() {
        return switch (timeScale) {
            case 1 -> ctx.demand30m();
            case 2 -> ctx.demand1h();
            case 3 -> ctx.demand12h();
            case 4 -> ctx.demand1d();
            case 5 -> ctx.demandAllTime();
            default -> ctx.powerDemandHistory();
        };
    }

    private int powerColor() { return ctx.power().powered() ? DUTheme.PROGRESS_ORANGE : DUTheme.ERROR; }

    // --- Static format helpers ---

    public static String formatFe(int centiFe) {
        int whole = centiFe / 100, frac = Math.abs(centiFe % 100);
        if (frac == 0) return whole + " FE";
        if (frac % 10 == 0) return whole + "." + (frac / 10) + " FE";
        return whole + "." + (frac < 10 ? "0" : "") + frac + " FE";
    }

    public static String formatMfe(long fe) {
        if (fe >= 1_000_000L) return String.format(java.util.Locale.ROOT, "%.1f MFE", fe / 1_000_000.0);
        if (fe >= 1_000L)     return String.format(java.util.Locale.ROOT, "%.1f kFE", fe / 1_000.0);
        return fe + " FE";
    }

    private static float powerRatio(int value, int max) {
        return max <= 0 ? 0f : Math.max(0f, Math.min(1f, value / (float) max));
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 3 && font.width(text + "...") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
