package net.doole.doolestools.client.gui;

import net.doole.doolestools.logistics.data.FurnaceSummary;
import net.doole.doolestools.logistics.data.GraphNodeData;
import net.doole.doolestools.logistics.data.GraphPortData;
import net.doole.doolestools.logistics.data.InventorySummary;
import net.doole.doolestools.logistics.data.MachineProgressData;
import net.doole.doolestools.logistics.data.ScannedBlockData;
import net.doole.doolestools.logistics.data.WarningData;
import net.doole.doolestools.logistics.FilterSettings;
import net.doole.doolestools.logistics.NodeType;
import net.doole.doolestools.logistics.PortDirection;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Stateless renderer for a single flowgraph node card (drawn in canvas space). */
public final class GraphNodeWidget {
    private GraphNodeWidget() {}

    private enum ActivityState { WORKING, STANDBY, ERROR }

    public static void render(GuiGraphicsExtractor g, Font font, GraphNodeData node, ScannedBlockData scanned,
                              boolean selected, boolean isLinkSource) {
        int x = node.x();
        int y = node.y();
        int w = node.width();
        int h = node.height();

        int worst = worstColor(scanned);
        ActivityState activity = activityState(scanned);

        // Only active machines get a moving border; standby/error states use the status dot instead.
        if (activity == ActivityState.WORKING && isMachine(node, scanned)) {
            int phase = (int) (System.currentTimeMillis() / 45L);
            DUTheme.dashedRect(g, x - 2, y - 2, w + 4, h + 4, pulse(DUTheme.OK), 3, 3, phase);
        }

        // Card body + title bar.
        DUTheme.box(g, x, y, w, h, DUTheme.PANEL, selected ? DUTheme.SELECTED : DUTheme.PANEL_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 13, DUTheme.PANEL_HEADER);
        DUTheme.glowText(g, font, trim(font, node.displayName(), w - 8), x + 5, y + 3, DUTheme.TEXT);
        statusDot(g, x + w - 10, y + 7, activity);

        // Real item icon (falls back to a category-coloured square).
        ItemIcons.render(g, scanned != null ? scanned.registryId() : "", x + 5, y + 17, ItemIcons.SIZE, iconColor(scanned));

        if (node.type() == NodeType.FILTER) {
            renderFilterSummary(g, font, node);
            if (selected) {
                brackets(g, x, y, w, h, pulse(DUTheme.SELECTED));
            } else if (isLinkSource) {
                brackets(g, x, y, w, h, pulse(DUTheme.PROGRESS_ORANGE));
            }
            renderPorts(g, font, node, selected || isLinkSource);
            return;
        }

        if (node.type() == NodeType.CHANNEL || node.type() == NodeType.SPLITTER || node.type() == NodeType.COMBINE) {
            renderRoutingSummary(g, font, node);
            if (selected) {
                brackets(g, x, y, w, h, pulse(DUTheme.SELECTED));
            } else if (isLinkSource) {
                brackets(g, x, y, w, h, pulse(DUTheme.PROGRESS_ORANGE));
            }
            renderPorts(g, font, node, selected || isLinkSource);
            return;
        }

        int textX = x + 24;
        int line = y + 18;
        String typeLine = scanned != null ? scanned.blockName() : node.type().name();
        g.text(font, trim(font, typeLine, w - 28), textX, line, DUTheme.TEXT_DIM, false);
        line += 11;

        // Main stat + optional progress bar.
        if (scanned != null && scanned.furnace().hasData()) {
            FurnaceSummary f = scanned.furnace();
            g.text(font, trim(font, f.status(), w - 28), textX, line, statusTextColor(f.status()), false);
            // Recipe row: input icon -> result icon (real items).
            int ry = y + 44;
            if (f.hasRecipe() || !f.inputId().isEmpty()) {
                int ix = x + 5;
                if (!f.inputId().isEmpty()) { ItemIcons.render(g, f.inputId(), ix, ry, ItemIcons.SIZE, DUTheme.PANEL_ALT); ix += 18; }
                g.text(font, "→", ix, ry + 4, DUTheme.TEXT_DIM, false);
                ix += font.width("→") + 4;
                if (!f.resultId().isEmpty()) ItemIcons.render(g, f.resultId(), ix, ry, ItemIcons.SIZE, DUTheme.PANEL_ALT);
            }
            MachineProgressData progress = scanned.progress();
            // Live-extrapolate the cook bar from the scan time so it animates smoothly between scans
            // (matches NodeDetailsPanel), instead of freezing on the last sampled percent.
            long elapsed = DUTheme.clientGameTime() - scanned.lastScannedGameTime();
            int pct = f.cookTotal() > 0 ? f.predictedPercent(elapsed) : 0;
            DUTheme.progress(g, x + 5, y + h - 22, w - 10, 7, pct / 100f, progress.error() ? DUTheme.ERROR : DUTheme.PROGRESS_ORANGE);
            String pctStr = pct + "%";
            g.text(font, pctStr, x + w - 5 - font.width(pctStr), y + h - 31, DUTheme.TEXT_DIM, false);
            if (f.isCooking()) {
                g.text(font, DUTheme.formatTicks(f.predictedRemainingTicks(elapsed)), x + 5, y + h - 31, DUTheme.OK, false);
            }
        } else if (scanned != null && scanned.progress().present() && isMachine(node, scanned)) {
            // Modded machines render like the furnace card: status, the material/output contents as real
            // item icons, and a current-recipe progress bar — not just an energy fill.
            MachineProgressData progress = scanned.progress();
            int pct = Math.max(0, Math.min(100, progress.percent()));
            int statusColor = progress.error() ? DUTheme.ERROR : progress.active() ? DUTheme.OK : DUTheme.WARN;
            g.text(font, trim(font, progress.status(), w - 28), textX, line, statusColor, false);
            int ix = x + 5;
            int ry = y + 44;
            for (int i = 0; i < scanned.inventory().topStacks().size() && ix + ItemIcons.SIZE <= x + w - 5; i++) {
                ItemIcons.render(g, scanned.inventory().topStacks().get(i).registryId(), ix, ry, ItemIcons.SIZE, DUTheme.PANEL_ALT);
                ix += 18;
            }
            DUTheme.progress(g, x + 5, y + h - 22, w - 10, 7, pct / 100f, statusColor);
            String pctStr = pct + "%";
            g.text(font, pctStr, x + w - 5 - font.width(pctStr), y + h - 31, DUTheme.TEXT_DIM, false);
            if (progress.hasTimer()) g.text(font, "~" + DUTheme.formatTicks(timerTicks(progress)), x + 5, y + h - 31, DUTheme.TEXT_GREEN, false);
        } else if (scanned != null && scanned.inventory().hasData()) {
            InventorySummary inv = scanned.inventory();
            g.text(font, inv.fillPercent() + "%", textX, line, fillColor(inv.fillPercent()), false);
            g.text(font, inv.usedSlots() + " / " + inv.totalSlots(), x + w - 5 - font.width(inv.usedSlots() + " / " + inv.totalSlots()), line, DUTheme.TEXT_DIM, false);
            // Top contents as real item icons.
            int ix = x + 5;
            int ry = y + 44;
            for (int i = 0; i < inv.topStacks().size() && ix + ItemIcons.SIZE <= x + w - 5; i++) {
                ItemIcons.render(g, inv.topStacks().get(i).registryId(), ix, ry, ItemIcons.SIZE, DUTheme.PANEL_ALT);
                ix += 18;
            }
            DUTheme.progress(g, x + 5, y + h - 22, w - 10, 7, inv.fillPercent() / 100f, DUTheme.PROGRESS_BLUE);
        } else {
            g.text(font, node.type().name(), textX, line, DUTheme.TEXT_DIM, false);
        }

        // Warning strip.
        String strip = stripLabel(scanned);
        String network = networkLabel(scanned);
        int networkMax = w / 2;
        int networkW = network.isBlank() ? 0 : Math.min(font.width(network), networkMax);
        g.fill(x + 1, y + h - 12, x + w - 1, y + h - 1, 0xFF0a0d09);
        g.text(font, trim(font, strip, w - networkW - 12), x + 5, y + h - 11, worst, false);
        if (!network.isBlank()) {
            String trimmedNetwork = trim(font, network, networkMax);
            g.text(font, trimmedNetwork, x + w - 5 - font.width(trimmedNetwork), y + h - 11, DUTheme.TEXT_DIM, false);
        }

        // Selection corner brackets (cyan).
        if (selected) {
            brackets(g, x, y, w, h, pulse(DUTheme.SELECTED));
        } else if (isLinkSource) {
            brackets(g, x, y, w, h, pulse(DUTheme.PROGRESS_ORANGE));
        }

        if (node.instanced()) instanceBadge(g, x + w - 12, y + 3);
        renderPorts(g, font, node, selected || isLinkSource);
    }

    public static void renderLite(GuiGraphicsExtractor g, Font font, GraphNodeData node, boolean selected, boolean isLinkSource) {
        int x = node.x();
        int y = node.y();
        int w = node.width();
        int h = node.height();
        int border = selected ? DUTheme.SELECTED : isLinkSource ? DUTheme.PROGRESS_ORANGE : DUTheme.PANEL_BORDER;
        DUTheme.box(g, x, y, w, h, DUTheme.PANEL, border);
        g.fill(x + 1, y + 1, x + w - 1, y + 13, DUTheme.PANEL_HEADER);
        g.text(font, trim(font, node.displayName(), w - 16), x + 5, y + 3, selected ? DUTheme.SELECTED : DUTheme.TEXT, false);
        g.text(font, node.type().name(), x + 5, y + 18, DUTheme.TEXT_DIM, false);
        if (node.type() == NodeType.FILTER) renderFilterSummary(g, font, node);
        if (node.instanced()) instanceBadge(g, x + w - 12, y + 3);
        renderPortDots(g, node, selected || isLinkSource);
    }

    /** Two dots joined by a line — marks a node that is an instance of an already-placed block. */
    private static void instanceBadge(GuiGraphicsExtractor g, int x, int y) {
        int color = 0xFF7a9aaf;
        // Left dot
        g.fill(x, y + 1, x + 2, y + 5, color);
        g.fill(x - 1, y + 2, x + 3, y + 4, color);
        // Connecting line
        g.fill(x + 2, y + 3, x + 7, y + 4, color);
        // Right dot
        g.fill(x + 7, y + 1, x + 9, y + 5, color);
        g.fill(x + 6, y + 2, x + 10, y + 4, color);
    }

    private static void renderFilterSummary(GuiGraphicsExtractor g, Font font, GraphNodeData node) {
        FilterSettings filter = FilterSettings.parse(node.notes());
        int x = node.x() + 5;
        int y = node.y() + 36;
        int itemCount = 0;
        for (String item : filter.paddedItems()) if (item != null && !item.isBlank()) itemCount++;
        filterPill(g, font, x, y, 38, modeShort(filter.mode()), DUTheme.SELECTED);
        filterPill(g, font, x + 42, y, 38, itemCount + " item", DUTheme.TEXT_GREEN);
        filterPill(g, font, x + 84, y, 38, filter.routing() == FilterSettings.Routing.ROUND_ROBIN ? "RR" : "First", DUTheme.PROGRESS_BLUE);
        String settings = "Limit " + filter.limit() + "  Tick " + filter.tickSpeed() + "  Prio " + filter.priority();
        g.text(font, trim(font, settings, node.width() - 10), node.x() + 5, y + 16, DUTheme.TEXT_DIM, false);
        g.text(font, "Click wells for item picker", node.x() + 5, node.y() + node.height() - 22, DUTheme.TEXT_DIM, false);
    }

    /** Card body for the routing nodes: Channel shows its colour swatch, Splitter/Combine a hint. */
    private static void renderRoutingSummary(GuiGraphicsExtractor g, Font font, GraphNodeData node) {
        int x = node.x() + 24;
        int y = node.y() + 18;
        switch (node.type()) {
            case CHANNEL -> {
                FilterSettings fs = FilterSettings.parse(node.notes());
                int color = FilterSettings.channelColor(fs.channel());
                g.text(font, "Stamps channel", x, y, DUTheme.TEXT_DIM, false);
                int sx = node.x() + 5;
                int sy = node.y() + 40;
                g.text(font, "Channel", sx, sy, DUTheme.TEXT_DIM, false);
                if (color == 0) {
                    g.text(font, "None", sx + 48, sy, DUTheme.TEXT_DIM, false);
                } else {
                    g.fill(sx + 48, sy - 1, sx + 70, sy + 8, 0xFF0B0F0A);
                    g.fill(sx + 49, sy, sx + 69, sy + 7, color);
                }
            }
            case SPLITTER -> g.text(font, "Round-robin split", x, y, DUTheme.PROGRESS_BLUE, false);
            case COMBINE -> g.text(font, "Merge inputs", x, y, DUTheme.TEXT_GREEN, false);
            default -> { }
        }
    }

    private static String modeShort(FilterSettings.Mode mode) {
        return switch (mode) {
            case PASS_ALL -> "Pass";
            case WHITELIST -> "Allow";
            case BLACKLIST -> "Block";
        };
    }

    private static void filterPill(GuiGraphicsExtractor g, Font font, int x, int y, int w, String text, int color) {
        DUTheme.box(g, x, y, w, 12, 0xFF14303a, color);
        g.centeredText(font, trim(font, text, w - 4), x + w / 2, y + 3, color);
    }

    private static void renderPortDots(GuiGraphicsExtractor g, GraphNodeData node, boolean active) {
        int inIndex = 0;
        int outIndex = 0;
        for (GraphPortData port : node.ports()) {
            int index = port.direction() == PortDirection.IN ? inIndex++ : outIndex++;
            int px = GraphNodeWidget.portX(node, port.direction());
            int py = GraphNodeWidget.portY(node, port.direction(), index);
            socket(g, px, py, active ? pulse(DUTheme.SELECTED) : port.direction() == PortDirection.IN ? DUTheme.PROGRESS_BLUE : DUTheme.OK);
        }
    }

    private static boolean isMachine(GraphNodeData node, ScannedBlockData scanned) {
        if (node.type() == NodeType.MACHINE) return true;
        return scanned != null && (scanned.isMachineLike() || scanned.furnace().hasData());
    }

    private static ActivityState activityState(ScannedBlockData scanned) {
        if (scanned == null) return ActivityState.STANDBY;
        if (scanned.progress().present()) {
            if (scanned.progress().error()) return ActivityState.ERROR;
            if (scanned.progress().active()) return ActivityState.WORKING;
        }
        if (worstColor(scanned) == DUTheme.ERROR) return ActivityState.ERROR;
        if (scanned.furnace().hasData()) {
            String status = scanned.furnace().status();
            if ("Output Full".equals(status) || "Not Progressing".equals(status)) return ActivityState.ERROR;
            if (scanned.furnace().isCooking()) return ActivityState.WORKING;
        }
        // A furnace with all three slots occupied is not a "full storage" error (handled above).
        if (!scanned.furnace().hasData() && scanned.inventory().hasData() && scanned.inventory().fillPercent() >= 100) {
            return ActivityState.ERROR;
        }
        return ActivityState.STANDBY;
    }

    private static int timerTicks(MachineProgressData progress) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, progress.remainingTicks()));
    }

    private static void statusDot(GuiGraphicsExtractor g, int cx, int cy, ActivityState state) {
        int color = switch (state) {
            case WORKING -> pulse(DUTheme.OK);
            case ERROR -> DUTheme.ERROR;
            case STANDBY -> DUTheme.WARN;
        };
        g.fill(cx - 4, cy - 4, cx + 5, cy + 5, 0xFF020402);
        if (state == ActivityState.WORKING && net.doole.doolestools.client.ClientPrefs.animate) {
            int glow = pulse(0x6632ff7a);
            g.fill(cx - 5, cy - 1, cx + 6, cy + 2, glow);
            g.fill(cx - 1, cy - 5, cx + 2, cy + 6, glow);
        }
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, color);
    }

    /** Sockets sit on the card edges; port labels are drawn OUTSIDE the node so they never overlap the body. */
    private static void renderPorts(GuiGraphicsExtractor g, Font font, GraphNodeData node, boolean active) {
        int inIndex = 0;
        int outIndex = 0;
        for (GraphPortData port : node.ports()) {
            int py;
            int px;
            if (port.direction() == PortDirection.IN) {
                py = portY(node, PortDirection.IN, inIndex++);
                px = node.x() - 3;
                socket(g, px, py, active ? pulse(DUTheme.SELECTED) : DUTheme.PROGRESS_BLUE);
                String label = port.label();
                g.text(font, label, node.x() - 7 - font.width(label), py - 4, DUTheme.TEXT_DIM, false);
            } else {
                py = portY(node, PortDirection.OUT, outIndex++);
                px = node.x() + node.width() + 3;
                socket(g, px, py, active ? pulse(DUTheme.SELECTED) : DUTheme.OK);
                g.text(font, port.label(), node.x() + node.width() + 7, py - 4, DUTheme.TEXT_DIM, false);
            }
        }
    }

    private static void socket(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFF020402);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, color);
    }

    private static int pulse(int color) {
        if (!net.doole.doolestools.client.ClientPrefs.animate) return color;
        int lift = (int) ((Math.sin(System.currentTimeMillis() / 160.0) + 1.0) * 24.0);
        int a = color & 0xFF000000;
        int r = Math.min(255, ((color >> 16) & 0xFF) + lift);
        int g = Math.min(255, ((color >> 8) & 0xFF) + lift);
        int b = Math.min(255, (color & 0xFF) + lift);
        return a | (r << 16) | (g << 8) | b;
    }

    public static int portY(GraphNodeData node, PortDirection direction, int index) {
        return node.y() + 24 + index * 14;
    }

    public static int portX(GraphNodeData node, PortDirection direction) {
        return direction == PortDirection.IN ? node.x() - 3 : node.x() + node.width() + 3;
    }

    private static void brackets(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        int s = 5;
        // top-left
        g.fill(x - 2, y - 2, x - 2 + s, y - 1, c);
        g.fill(x - 2, y - 2, x - 1, y - 2 + s, c);
        // top-right
        g.fill(x + w + 2 - s, y - 2, x + w + 2, y - 1, c);
        g.fill(x + w + 1, y - 2, x + w + 2, y - 2 + s, c);
        // bottom-left
        g.fill(x - 2, y + h + 1, x - 2 + s, y + h + 2, c);
        g.fill(x - 2, y + h + 2 - s, x - 1, y + h + 2, c);
        // bottom-right
        g.fill(x + w + 2 - s, y + h + 1, x + w + 2, y + h + 2, c);
        g.fill(x + w + 1, y + h + 2 - s, x + w + 2, y + h + 2, c);
    }

    private static String stripLabel(ScannedBlockData scanned) {
        if (scanned == null || scanned.warnings().isEmpty()) return "OK";
        WarningData w = scanned.warnings().get(0);
        return switch (w.severity()) {
            case ERROR -> "! " + w.message().toUpperCase(java.util.Locale.ROOT);
            case WARNING -> w.message();
            default -> w.message();
        };
    }

    private static String networkLabel(ScannedBlockData scanned) {
        if (scanned == null || scanned.networkName() == null || scanned.networkName().isBlank()) return "";
        return scanned.networkName();
    }

    private static int worstColor(ScannedBlockData scanned) {
        if (scanned == null) return DUTheme.OK;
        int color = DUTheme.OK;
        for (WarningData w : scanned.warnings()) {
            if (w.severity() == WarningData.Severity.ERROR) return DUTheme.ERROR;
            if (w.severity() == WarningData.Severity.WARNING) color = DUTheme.WARN;
        }
        return color;
    }

    private static int iconColor(ScannedBlockData scanned) {
        if (scanned == null) return DUTheme.DISABLED;
        return switch (scanned.type()) {
            case STORAGE -> 0xFFb07a3a;
            case MACHINE -> 0xFF8a8f96;
            case TRANSPORT -> 0xFF6a7066;
            case UNKNOWN_STORAGE, UNKNOWN_MACHINE, UNKNOWN -> 0xFF55405a;
        };
    }

    private static int fillColor(int pct) {
        if (pct >= 100) return DUTheme.ERROR;
        if (pct >= 85) return DUTheme.WARN;
        return DUTheme.OK;
    }

    private static int statusTextColor(String status) {
        return switch (status) {
            case "Running" -> DUTheme.OK;
            case "Output Full" -> DUTheme.ERROR;
            case "No Fuel", "Not Progressing" -> DUTheme.WARN;
            default -> DUTheme.TEXT_DIM;
        };
    }

    private static String trim(Font font, String s, int maxWidth) {
        if (s == null) return "";
        if (font.width(s) <= maxWidth) return s;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (font.width(b.toString() + s.charAt(i) + "…") > maxWidth) break;
            b.append(s.charAt(i));
        }
        return b + "…";
    }
}
