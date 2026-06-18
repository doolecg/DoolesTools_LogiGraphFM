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
    private int inventoryScrollOffset = 0;
    private int batteryScrollOffset = 0;
    private int inventoryX, inventoryY, inventoryW, inventoryH, inventoryMaxScroll;
    private int capacityX, capacityY, capacityW, capacityH, batteryMaxScroll;
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

    public boolean handleScroll(double mx, double my, double delta) {
        int step = -(int) Math.signum(delta);
        if (mx >= inventoryX && mx < inventoryX + inventoryW && my >= inventoryY && my < inventoryY + inventoryH && inventoryMaxScroll > 0) {
            inventoryScrollOffset = Math.max(0, Math.min(inventoryMaxScroll, inventoryScrollOffset + step));
            return true;
        }
        if (mx >= capacityX && mx < capacityX + capacityW && my >= capacityY && my < capacityY + capacityH && batteryMaxScroll > 0) {
            batteryScrollOffset = Math.max(0, Math.min(batteryMaxScroll, batteryScrollOffset + step));
            return true;
        }
        if (graphContains(mx, my)) {
            scrollGraph(delta);
            return true;
        }
        return false;
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

        int gap = 8;
        int quadW = (fullContentW - gap) / 2;
        int quadH = Math.max(76, (h - 22 - gap) / 2);
        int topY = contentY + 8;
        int bottomY = topY + quadH + gap;
        int leftX = cx;
        int rightX = cx + quadW + gap;

        // ── TOP LEFT: POWER GRAPH ────────────────────────────────────────────
        DUTheme.panelWithHeader(g, font, leftX, topY, quadW, quadH, "POWER GRAPH");
        renderPowerHistoryGraph(g, leftX + 8, topY + 16, quadW - 16, quadH - 24);

        // ── TOP RIGHT: POWER STATS + BREAKDOWN ──────────────────────────────
        DUTheme.panelWithHeader(g, font, rightX, topY, quadW, quadH, "POWER STATS / BREAKDOWN");
        int statusColor = power.starved() ? DUTheme.ERROR : power.degraded() ? DUTheme.WARN : DUTheme.OK;
        String statusText = power.starved() ? "NO POWER — STOPPED"
                : power.degraded() ? "LOW POWER — " + Math.round(power.satisfaction() * 100) + "%"
                : "NETWORK SATISFIED";
        DUTheme.dot(g, rightX + 8, topY + 18, statusColor);
        g.text(font, statusText, rightX + 18, topY + 16, statusColor, false);

        int barsTop = topY + 32;
        int peak = Math.max(1, Math.max(power.supplyCentiFe(), power.demandCentiFe()));
        statBar(g, rightX + 8, barsTop, quadW - 16, "Production", powerRatio(power.supplyCentiFe(), peak), formatFe(power.supplyCentiFe()) + "/t", DUTheme.OK);
        statBar(g, rightX + 8, barsTop + 14, quadW - 16, "Consumption", powerRatio(power.demandCentiFe(), peak), formatFe(power.demandCentiFe()) + "/t", DUTheme.ERROR);
        int ly = barsTop + 33;
        ly = statSmall(g, rightX + 8, ly, quadW - 16, "Computer", formatFe(power.computerCentiFe()) + " cF/t");
        ly = statSmall(g, rightX + 8, ly, quadW - 16, "Sockets", formatFe(power.endpointCentiFe()) + " cF/t");
        ly = statSmall(g, rightX + 8, ly, quadW - 16, "Cable", formatFe(power.wireCentiFe()) + " cF/t");
        ly = statSmall(g, rightX + 8, ly, quadW - 16, "Devices", formatFe(power.deviceCentiFe()) + " cF/t");
        ly = statSmall(g, rightX + 8, ly, quadW - 16, "Routes", formatFe(power.routeCentiFe()) + " cF/t");
        ly = statSmall(g, rightX + 8, ly, quadW - 16, "Batteries", formatFe(power.batteryCentiFe()) + " cF/t");
        ThroughputPlanner.PlannerResult plan = ThroughputPlanner.analyse(ctx.graph(), scan);
        if (ly + 10 < topY + quadH) {
            g.text(font, "Routes " + power.routeCount() + (power.powered() && ClientPrefs.autoRefresh ? "  LIVE" : "  IDLE"), rightX + 8, ly, power.powered() ? DUTheme.OK : DUTheme.DISABLED, false);
            g.text(font, "Bottlenecks " + plan.totalBottlenecks() + "  Starved " + plan.totalStarved(), rightX + 112, ly, plan.totalBottlenecks() > 0 || plan.totalStarved() > 0 ? DUTheme.WARN : DUTheme.TEXT_DIM, false);
        }

        // ── BOTTOM LEFT: INVENTORY WATCH ─────────────────────────────────────
        DUTheme.panelWithHeader(g, font, leftX, bottomY, quadW, quadH, "INVENTORY WATCH");
        inventoryX = leftX;
        inventoryY = bottomY;
        inventoryW = quadW;
        inventoryH = quadH;
        List<ScannedBlockData> invBlocks = new ArrayList<>();
        for (ScannedBlockData s : scan) if (s.inventory().hasData()) invBlocks.add(s);
        invBlocks.sort(java.util.Comparator.comparingInt(s -> -s.inventory().fillPercent()));
        if (invBlocks.isEmpty()) {
            inventoryScrollOffset = 0;
            inventoryMaxScroll = 0;
            g.centeredText(font, "NO INVENTORY DATA", leftX + quadW / 2, bottomY + quadH / 2 - 4, DUTheme.TEXT_DIM);
        } else {
            int rowY = bottomY + 18;
            int nameW = Math.max(70, quadW / 4);
            int suffixW = 34;
            int barW = Math.max(28, quadW - nameW - suffixW - 24);
            int visibleRows = Math.max(1, (quadH - 24) / 13);
            inventoryMaxScroll = Math.max(0, invBlocks.size() - visibleRows);
            inventoryScrollOffset = Math.min(inventoryScrollOffset, inventoryMaxScroll);
            for (int i = inventoryScrollOffset; i < invBlocks.size(); i++) {
                ScannedBlockData s = invBlocks.get(i);
                if (rowY + 10 > bottomY + quadH - 6) break;
                int pct = s.inventory().fillPercent();
                boolean isFull = s.inventory().usedSlots() >= s.inventory().totalSlots();
                boolean isStorage = s.isStorageLike();
                int barColor = isFull ? DUTheme.ERROR : pct >= WarningGenerator.NEARLY_FULL_PERCENT ? DUTheme.WARN : DUTheme.OK;
                int nameColor = isFull && isStorage ? DUTheme.WARN : DUTheme.TEXT_DIM;
                String name = trim(font, s.blockName(), nameW);
                g.text(font, name, leftX + 8, rowY, nameColor, false);
                int bx = leftX + 10 + nameW;
                DUTheme.progress(g, bx, rowY, barW, 7, pct / 100f, barColor);
                String suffix = isFull ? "FULL" : pct + "%";
                g.text(font, suffix, bx + barW + 5, rowY, isFull && isStorage ? DUTheme.WARN : DUTheme.TEXT_DIM, false);
                rowY += 13;
            }
            renderScrollbar(g, leftX + quadW - 5, bottomY + 14, quadH - 20, inventoryScrollOffset, inventoryMaxScroll);
        }

        // ── BOTTOM RIGHT: CAPACITIES ─────────────────────────────────────────
        DUTheme.panelWithHeader(g, font, rightX, bottomY, quadW, quadH, "CAPACITIES");
        capacityX = rightX;
        capacityY = bottomY;
        capacityW = quadW;
        capacityH = quadH;
        int ry = bottomY + 18;
        if (power.batteryCapacity() > 0) {
            float storedFrac = Math.max(0f, Math.min(1f, (float) power.batteryStored() / power.batteryCapacity()));
            int battColor = storedFrac > 0.6f ? 0xFF3af0a0 : storedFrac >= 0.3f ? 0xFFf09030 : 0xFFf03030;
            DUTheme.horizBattery(g, rightX + 8, ry, quadW - 16, 14, storedFrac, battColor);
            int pctVal = Math.round(storedFrac * 100f);
            g.text(font, pctVal + "%  " + formatMfe(power.batteryStored()) + " / " + formatMfe(power.batteryCapacity()),
                    rightX + 8, ry + 17, DUTheme.TEXT_DIM, false);
            ry += 34;
            if (power.batteryCount() > 0) {
                int gridH = Math.max(16, bottomY + quadH - ry - 26);
                int count = power.batteryCount();
                int miniGap = count <= 32 ? 3 : 2;
                int maxCols = Math.max(1, (quadW - 16) / 9);
                int cols = Math.max(1, Math.min(count, maxCols));
                int rows = Math.max(1, (count + cols - 1) / cols);
                int miniW = Math.max(4, Math.min(12, (quadW - 16 - miniGap * (cols - 1)) / cols));
                int miniH = Math.max(8, Math.min(24, (gridH - miniGap * (rows - 1)) / rows));
                int visibleRows = Math.max(1, gridH / Math.max(1, miniH + miniGap));
                batteryMaxScroll = Math.max(0, rows - visibleRows);
                batteryScrollOffset = Math.min(batteryScrollOffset, batteryMaxScroll);
                int visible = Math.min(count, cols * visibleRows);
                int start = batteryScrollOffset * cols;
                for (int local = 0; local < visible && start + local < count; local++) {
                    DUTheme.vertBattery(g,
                            rightX + 8 + (local % cols) * (miniW + miniGap),
                            ry + (local / cols) * (miniH + miniGap) + 2,
                            miniW, miniH, storedFrac, battColor);
                }
                if (start + visible < count) {
                    String more = "+" + (count - start - visible);
                    g.text(font, more, rightX + quadW - 8 - font.width(more), bottomY + quadH - 34, DUTheme.TEXT_GREEN_DIM, false);
                }
                renderScrollbar(g, rightX + quadW - 5, ry, gridH, batteryScrollOffset, batteryMaxScroll);
            }
        } else {
            batteryScrollOffset = 0;
            batteryMaxScroll = 0;
            g.text(font, "No batteries", rightX + 8, ry, DUTheme.TEXT_DIM, false); ry += 14;
        }
        int netY = bottomY + quadH - 24;
        g.fill(rightX + 8, netY - 4, rightX + quadW - 8, netY - 3, DUTheme.PANEL_BORDER);
        g.text(font, "GEN " + power.generatorCount(), rightX + 8, netY, DUTheme.TEXT_DIM, false);
        g.text(font, "BAT " + power.batteryCount(), rightX + 60, netY, DUTheme.TEXT_DIM, false);
        g.text(font, "SOCKETS " + power.endpointCount(), rightX + 112, netY, DUTheme.TEXT_DIM, false);
        g.text(font, warnings + "W " + errors + "E", rightX + quadW - 52, netY, errors > 0 ? DUTheme.ERROR : warnings > 0 ? DUTheme.WARN : DUTheme.OK, false);
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

    private void renderScrollbar(GuiGraphicsExtractor g, int x, int y, int h, int offset, int maxOffset) {
        if (maxOffset <= 0 || h <= 10) return;
        g.fill(x, y, x + 2, y + h, 0x6632452f);
        int thumbH = Math.max(8, h / (maxOffset + 1));
        int travel = Math.max(1, h - thumbH);
        int thumbY = y + Math.round(travel * (offset / (float) maxOffset));
        g.fill(x - 1, thumbY, x + 3, thumbY + thumbH, DUTheme.SELECTED);
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
        int cx = statsContentX();
        int fullContentW = panelW - 32;
        int gap = 8;
        int quadW = (fullContentW - gap) / 2;
        int gx = cx + 8;
        int gy = contentY + 8 + 16;
        return mx >= gx && mx < gx + quadW - 16 && my >= gy && my < gy + 96;
    }

    // --- Series helpers ---

    private List<? extends Number> supplySeries() {
        List<? extends Number> selected = switch (timeScale) {
            case 1 -> ctx.supply30m();
            case 2 -> ctx.supply1h();
            case 3 -> ctx.supply12h();
            case 4 -> ctx.supply1d();
            case 5 -> ctx.supplyAllTime();
            default -> ctx.powerSupplyHistory();
        };
        return selected.size() >= 2 || timeScale == 0 ? selected : downsample(ctx.powerSupplyHistory(), 32);
    }

    private List<? extends Number> demandSeries() {
        List<? extends Number> selected = switch (timeScale) {
            case 1 -> ctx.demand30m();
            case 2 -> ctx.demand1h();
            case 3 -> ctx.demand12h();
            case 4 -> ctx.demand1d();
            case 5 -> ctx.demandAllTime();
            default -> ctx.powerDemandHistory();
        };
        return selected.size() >= 2 || timeScale == 0 ? selected : downsample(ctx.powerDemandHistory(), 32);
    }

    private static List<Integer> downsample(List<Integer> source, int targetPoints) {
        if (source == null || source.size() <= targetPoints) return source == null ? List.of() : source;
        List<Integer> out = new ArrayList<>();
        int bucket = Math.max(1, source.size() / targetPoints);
        for (int i = 0; i < source.size(); i += bucket) {
            long sum = 0;
            int count = 0;
            for (int j = i; j < source.size() && j < i + bucket; j++) {
                sum += source.get(j);
                count++;
            }
            out.add(count == 0 ? 0 : (int) (sum / count));
        }
        return out;
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
