package top.xcyyds.starworld.forge.debug.wand.client.screen.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.debug.wand.DebugWandItem;
import top.xcyyds.starworld.forge.debug.wand.DebugWandOptions;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;
import top.xcyyds.starworld.forge.nation.client.StarWorldClientNationOverlayState;
import top.xcyyds.starworld.forge.network.StarWorldNetwork;
import top.xcyyds.starworld.forge.network.packet.RequestNationOverlayC2SPacket;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("null")
public final class MapPage implements DebugPage {
    private static final int[] BLOCKS_PER_PIXEL_LEVELS = new int[]{1, 2, 4, 8, 16};
    private static boolean DEBUG = false;

    private static final Set<Long> DIRTY_TILES = new HashSet<>();

    private final DebugWandDebugScreen screen;

    private int mapLeft;
    private int mapTop;
    private int mapRight;
    private int mapBottom;
    private int mapW;
    private int mapH;

    private Button recenterButton;

    private int blocksPerPixelIndex = 2;
    private double centerX;
    private double centerZ;

    private boolean dragging;
    private double lastDragX;
    private double lastDragY;

    private boolean wDown;
    private boolean aDown;
    private boolean sDown;
    private boolean dDown;

    private Integer pinnedWorldX;
    private Integer pinnedWorldZ;

    private int pendingZoomSteps;
    private double pendingZoomMouseX;
    private double pendingZoomMouseY;

    private MapRenderer renderer;

    public MapPage(DebugWandDebugScreen screen) {
        this.screen = screen;
    }

    private boolean shouldShowNationArea() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof DebugWandItem) {
            DebugWandOptions opt = DebugWandOptions.get(main);
            if (opt.enableMainHand && opt.showNationArea) {
                return true;
            }
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof DebugWandItem) {
            DebugWandOptions opt = DebugWandOptions.get(off);
            if (opt.enableOffHand && opt.showNationArea) {
                return true;
            }
        }
        return false;
    }

    private void drawNationOverlay(GuiGraphics graphics, int bpp, double renderCenterX, double renderCenterZ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        StarWorldClientNationOverlayState.Snapshot snapshot = StarWorldClientNationOverlayState.getSnapshot();
        if (snapshot == null) {
            return;
        }
        String dimId = level.dimension().location().toString();
        if (!snapshot.dimId().equals(dimId)) {
            return;
        }

        double worldMinX = renderCenterX - (mapW / 2.0) * bpp;
        double worldMinZ = renderCenterZ - (mapH / 2.0) * bpp;

        int lineColor = 0xCCFFFFFF;
        for (StarWorldClientNationOverlayState.Segment s : snapshot.segments()) {
            int sx0 = (int) Math.round(mapLeft + (s.x0() - worldMinX) / (double) bpp);
            int sy0 = (int) Math.round(mapTop + (s.z0() - worldMinZ) / (double) bpp);
            int sx1 = (int) Math.round(mapLeft + (s.x1() - worldMinX) / (double) bpp);
            int sy1 = (int) Math.round(mapTop + (s.z1() - worldMinZ) / (double) bpp);

            if (sx0 == sx1) {
                int x = sx0;
                int y0 = Math.min(sy0, sy1);
                int y1 = Math.max(sy0, sy1);
                if (x < mapLeft || x >= mapRight) {
                    continue;
                }
                if (y1 < mapTop || y0 >= mapBottom) {
                    continue;
                }
                y0 = Math.max(y0, mapTop);
                y1 = Math.min(y1, mapBottom);
                graphics.fill(x, y0, x + 1, y1, lineColor);
            } else if (sy0 == sy1) {
                int y = sy0;
                int x0 = Math.min(sx0, sx1);
                int x1 = Math.max(sx0, sx1);
                if (y < mapTop || y >= mapBottom) {
                    continue;
                }
                if (x1 < mapLeft || x0 >= mapRight) {
                    continue;
                }
                x0 = Math.max(x0, mapLeft);
                x1 = Math.min(x1, mapRight);
                graphics.fill(x0, y, x1, y + 1, lineColor);
            }
        }

        var font = Minecraft.getInstance().font;
        for (StarWorldClientNationOverlayState.Label label : snapshot.labels()) {
            String name = label.displayName();
            if (name == null || name.isEmpty()) {
                continue;
            }

            int sx = (int) Math.round(mapLeft + (label.capitalX() - worldMinX) / (double) bpp);
            int sy = (int) Math.round(mapTop + (label.capitalZ() - worldMinZ) / (double) bpp);
            if (sx < mapLeft || sx >= mapRight || sy < mapTop || sy >= mapBottom) {
                continue;
            }

            int w = font.width(name);
            int x0 = sx - w / 2 - 2;
            int y0 = sy - 5;
            int x1 = x0 + w + 4;
            int y1 = y0 + 10;
            if (x1 < mapLeft || x0 >= mapRight || y1 < mapTop || y0 >= mapBottom) {
                continue;
            }
            x0 = Math.max(x0, mapLeft);
            y0 = Math.max(y0, mapTop);
            x1 = Math.min(x1, mapRight);
            y1 = Math.min(y1, mapBottom);
            graphics.fill(x0, y0, x1, y1, 0x80000000);

            int textColor = 0xFF000000 | (label.colorRgb() & 0xFFFFFF);
            graphics.drawString(font, Component.literal(name), sx - w / 2, sy - 4, textColor);
        }
    }

    @Override
    public Component title() {
        return Component.translatable("gui.starworld.debug_wand.map.title");
    }

    @Override
    public void onOpen() {
        this.mapLeft = screen.contentLeft();
        this.mapTop = screen.contentTop();
        this.mapRight = screen.contentRight();
        this.mapBottom = screen.contentBottom();
        this.mapW = Math.max(1, mapRight - mapLeft);
        this.mapH = Math.max(1, mapBottom - mapTop);

        DIRTY_TILES.clear();

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            this.centerX = player.getX();
            this.centerZ = player.getZ();
        }

        recenterButton = Button.builder(Component.translatable("gui.starworld.debug_wand.map.recenter"), b -> recenterToPlayer())
                .bounds(mapRight - 90, mapTop, 90, 20)
                .build();
        screen.addRenderableWidgetPublic(recenterButton);

        renderer = new MapRenderer();
    }

    @Override
    public void onClose() {
        if (recenterButton != null) {
            screen.removeWidgetPublic(recenterButton);
            recenterButton = null;
        }
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
    }

    @Override
    public void tick() {
        if (pendingZoomSteps != 0) {
            applyPendingZoom();
        }

        int bpp = blocksPerPixel();
        double step = Math.max(16.0, 64.0 * bpp);
        if (wDown) {
            centerZ -= step;
        }
        if (sDown) {
            centerZ += step;
        }
        if (aDown) {
            centerX -= step;
        }
        if (dDown) {
            centerX += step;
        }
        if (renderer != null) {
            renderer.pump(500_000L);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int left, int top, int right, int bottom, int mouseX, int mouseY, float partialTick) {
        this.mapLeft = left;
        this.mapTop = top;
        this.mapRight = right;
        this.mapBottom = bottom;
        this.mapW = Math.max(1, mapRight - mapLeft);
        this.mapH = Math.max(1, mapBottom - mapTop);

        if (recenterButton != null) {
            recenterButton.setX(mapRight - 90);
            recenterButton.setY(mapTop);
        }

        int bpp = blocksPerPixel();
        double renderCenterX = snapCenter(centerX, bpp);
        double renderCenterZ = snapCenter(centerZ, bpp);
        if (renderer != null) {
            renderer.setView(renderCenterX, renderCenterZ, bpp, mapW, mapH);
            renderer.pump(2_000_000L);
            renderer.draw(graphics, mapLeft, mapTop);
        } else {
            graphics.fill(mapLeft, mapTop, mapRight, mapBottom, 0xAA000000);
        }

        if (shouldShowNationArea()) {
            drawNationOverlay(graphics, bpp, renderCenterX, renderCenterZ);
        }

        int hoverWorldX;
        int hoverWorldZ;
        if (isInMap(mouseX, mouseY)) {
            double worldMinX = renderCenterX - (mapW / 2.0) * bpp;
            double worldMinZ = renderCenterZ - (mapH / 2.0) * bpp;
            hoverWorldX = (int) Math.floor(worldMinX + (mouseX - mapLeft) * (double) bpp);
            hoverWorldZ = (int) Math.floor(worldMinZ + (mouseY - mapTop) * (double) bpp);

            int cellX = mapLeft + (mouseX - mapLeft);
            int cellY = mapTop + (mouseY - mapTop);
            graphics.renderOutline(cellX, cellY, 1, 1, 0xFFFFFFFF);
        } else {
            hoverWorldX = 0;
            hoverWorldZ = 0;
        }

        if (isInMap(mouseX, mouseY)) {
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("gui.starworld.debug_wand.map.hover_coord", hoverWorldX, hoverWorldZ),
                    mapLeft + 4, mapTop + 24, 0xFFFFFF);
        }

        if (DEBUG && renderer != null) {
            graphics.drawString(Minecraft.getInstance().font,
                    Component.literal(renderer.statsLine()),
                    mapLeft + 4, mapTop + 4, 0xFFFFFF);
        }

        drawPlayerMarker(graphics, bpp, renderCenterX, renderCenterZ);

        if (pinnedWorldX != null && pinnedWorldZ != null) {
            renderPinnedTooltip(graphics, mouseX, mouseY, pinnedWorldX, pinnedWorldZ);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        if (!isInMap(mouseX, mouseY)) {
            return false;
        }

        int bpp = blocksPerPixel();
        double worldMinX = centerX - (mapW / 2.0) * bpp;
        double worldMinZ = centerZ - (mapH / 2.0) * bpp;
        int wx = (int) Math.floor(worldMinX + (mouseX - mapLeft) * (double) bpp);
        int wz = (int) Math.floor(worldMinZ + (mouseY - mapTop) * (double) bpp);

        pinnedWorldX = wx;
        pinnedWorldZ = wz;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            return false;
        }
        if (!dragging) {
            dragging = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        if (!isInMap(lastDragX, lastDragY)) {
            lastDragX = mouseX;
            lastDragY = mouseY;
            return false;
        }

        double dx = mouseX - lastDragX;
        double dy = mouseY - lastDragY;

        int bpp = blocksPerPixel();
        centerX -= dx * bpp;
        centerZ -= dy * bpp;

        lastDragX = mouseX;
        lastDragY = mouseY;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isInMap(mouseX, mouseY)) {
            return false;
        }
        if (delta == 0) {
            return false;
        }

        int steps = (int) Math.round(Math.abs(delta));
        steps = Mth.clamp(steps, 1, 3);
        int dir = delta > 0 ? -1 : 1;

        pendingZoomSteps += dir * steps;
        pendingZoomMouseX = mouseX;
        pendingZoomMouseY = mouseY;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_W) {
            wDown = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S) {
            sDown = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_A) {
            aDown = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_D) {
            dDown = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_W) {
            wDown = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S) {
            sDown = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_A) {
            aDown = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_D) {
            dDown = false;
            return true;
        }
        return false;
    }

    private int blocksPerPixel() {
        blocksPerPixelIndex = Mth.clamp(blocksPerPixelIndex, 0, BLOCKS_PER_PIXEL_LEVELS.length - 1);
        return BLOCKS_PER_PIXEL_LEVELS[blocksPerPixelIndex];
    }

    private void applyPendingZoom() {
        if (mapW <= 0 || mapH <= 0) {
            pendingZoomSteps = 0;
            return;
        }

        double mouseX = pendingZoomMouseX;
        double mouseY = pendingZoomMouseY;

        int oldIndex = blocksPerPixelIndex;
        int oldBpp = blocksPerPixel();

        double anchorX;
        double anchorZ;
        if (isInMap(mouseX, mouseY)) {
            double worldMinX = centerX - (mapW / 2.0) * oldBpp;
            double worldMinZ = centerZ - (mapH / 2.0) * oldBpp;
            anchorX = worldMinX + (mouseX - mapLeft) * (double) oldBpp;
            anchorZ = worldMinZ + (mouseY - mapTop) * (double) oldBpp;
        } else {
            anchorX = centerX;
            anchorZ = centerZ;
            mouseX = mapLeft + mapW / 2.0;
            mouseY = mapTop + mapH / 2.0;
        }

        blocksPerPixelIndex = Mth.clamp(oldIndex + pendingZoomSteps, 0, BLOCKS_PER_PIXEL_LEVELS.length - 1);
        pendingZoomSteps = 0;

        int newBpp = blocksPerPixel();
        if (newBpp == oldBpp) {
            return;
        }

        double newWorldMinX = anchorX - (mouseX - mapLeft) * (double) newBpp;
        double newWorldMinZ = anchorZ - (mouseY - mapTop) * (double) newBpp;
        centerX = newWorldMinX + (mapW / 2.0) * newBpp;
        centerZ = newWorldMinZ + (mapH / 2.0) * newBpp;
    }

    private boolean isInMap(double x, double y) {
        return x >= mapLeft && x < mapRight && y >= mapTop && y < mapBottom;
    }

    private void recenterToPlayer() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        centerX = player.getX();
        centerZ = player.getZ();
    }

    private void drawPlayerMarker(GuiGraphics graphics, int bpp) {
        drawPlayerMarker(graphics, bpp, centerX, centerZ);
    }

    private void drawPlayerMarker(GuiGraphics graphics, int bpp, double renderCenterX, double renderCenterZ) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        double worldMinX = renderCenterX - (mapW / 2.0) * bpp;
        double worldMinZ = renderCenterZ - (mapH / 2.0) * bpp;
        int px = (int) Math.round(mapLeft + (player.getX() - worldMinX) / (double) bpp);
        int pz = (int) Math.round(mapTop + (player.getZ() - worldMinZ) / (double) bpp);

        if (px < mapLeft || px >= mapRight || pz < mapTop || pz >= mapBottom) {
            return;
        }

        int c = 0xFFFFD54F;
        graphics.fill(px - 1, pz - 1, px + 2, pz + 2, c);

        float yaw = player.getYRot();
        float rad = (float) Math.toRadians(yaw);
        float dx = -Mth.sin(rad);
        float dz = Mth.cos(rad);
        int len = 10;
        int ex = (int) Math.round(px + dx * len);
        int ez = (int) Math.round(pz + dz * len);
        drawLine(graphics, px, pz, ex, ez, 0xFFFFFFFF);
    }

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            g.fill(x, y, x + 1, y + 1, argb);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private void renderPinnedTooltip(GuiGraphics graphics, int mouseX, int mouseY, int wx, int wz) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.starworld.debug_wand.map.tooltip.coord", wx, wz));

        ChunkAccess chunk = level.getChunkSource().getChunk(wx >> 4, wz >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            lines.add(Component.translatable("gui.starworld.debug_wand.map.tooltip.chunk", Component.translatable("gui.starworld.debug_wand.map.unloaded")));
        } else {
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
            BlockPos pos = new BlockPos(wx, Math.max(level.getMinBuildHeight(), y - 1), wz);
            BlockState state = level.getBlockState(pos);
            lines.add(Component.translatable("gui.starworld.debug_wand.map.tooltip.block", state.getBlock().getName()));
        }

        graphics.renderComponentTooltip(mc.font, lines, mouseX, mouseY);
    }

    private final class MapRenderer {
        private static final int UNLOADED_RGBA = 0xFF000000;
        private static final int REGION_CHUNKS = 32;
        private static final int MAX_TILES = 131072;
        private static final int MAX_REGIONS = 256;

        private static final int REVALIDATE_PERIOD_TICKS = 20;
        private static final int REVALIDATE_BATCH = 32;

        private static final Map<Long, Tile> TILE_CACHE = new LinkedHashMap<>(8192, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<Long, Tile> eldest) {
                return size() > MAX_TILES;
            }
        };

        private static final LinkedHashMap<Long, Region> REGION_CACHE = new LinkedHashMap<>(256, 0.75f, true);

        private final TileDiskCache diskCache = new TileDiskCache();

        private final ArrayDeque<Long> buildQueue = new ArrayDeque<>();
        private final Set<Long> buildQueued = new HashSet<>();
        private final ArrayDeque<Long> regionUploadQueue = new ArrayDeque<>();
        private final Set<Long> regionUploadQueued = new HashSet<>();

        private final ArrayDeque<Long> revalidateQueue = new ArrayDeque<>();
        private final Set<Long> revalidateQueued = new HashSet<>();
        private long lastRevalidateGameTime;
        private int revalidateCursor;

        private long lastNationRequestMs;
        private int lastNationReqMinX;
        private int lastNationReqMaxX;
        private int lastNationReqMinZ;
        private int lastNationReqMaxZ;

        private final ArrayDeque<Long> diskLoadQueue = new ArrayDeque<>();
        private final Set<Long> diskLoadQueued = new HashSet<>();
        private final Set<Long> diskWriteQueued = new HashSet<>();

        private double viewCenterX;
        private double viewCenterZ;
        private int viewBpp;
        private int viewW;
        private int viewH;
        private long viewKey;

        private int lastCMinX;
        private int lastCMaxX;
        private int lastCMinZ;
        private int lastCMaxZ;

        private int lastVisible;
        private int lastChunkPresent;
        private int lastChunkMissing;
        private int lastBuildQueue;
        private int lastBlitQueue;
        private int lastTileCache;

        private void setView(double cx, double cz, int bpp, int w, int h) {
            boolean first = viewKey == 0;
            int oldBpp = viewBpp;
            int oldW = viewW;
            int oldH = viewH;

            long newKey = (((long) bpp) << 48) ^ ((long) w << 16) ^ (long) h;
            boolean reset = first || newKey != viewKey;
            viewKey = newKey;

            viewCenterX = cx;
            viewCenterZ = cz;
            viewBpp = bpp;
            viewW = w;
            viewH = h;

            if (reset || oldBpp != bpp || oldW != w || oldH != h) {
                buildQueue.clear();
                buildQueued.clear();
                diskLoadQueue.clear();
                diskLoadQueued.clear();
                regionUploadQueue.clear();
                regionUploadQueued.clear();
                lastCMinX = 0;
                lastCMaxX = 0;
                lastCMinZ = 0;
                lastCMaxZ = 0;
            }

            scheduleVisibleChunks();
        }

        private void scheduleVisibleChunks() {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                return;
            }
            int visible = 0;
            int present = 0;
            int missing = 0;

            double worldMinX = viewCenterX - (viewW / 2.0) * viewBpp;
            double worldMinZ = viewCenterZ - (viewH / 2.0) * viewBpp;
            double worldMaxX = viewCenterX + (viewW / 2.0) * viewBpp;
            double worldMaxZ = viewCenterZ + (viewH / 2.0) * viewBpp;

            int cMinX = Mth.floor(worldMinX) >> 4;
            int cMaxX = Mth.floor(worldMaxX) >> 4;
            int cMinZ = Mth.floor(worldMinZ) >> 4;
            int cMaxZ = Mth.floor(worldMaxZ) >> 4;

            if (cMinX == lastCMinX && cMaxX == lastCMaxX && cMinZ == lastCMinZ && cMaxZ == lastCMaxZ) {
                lastBuildQueue = buildQueue.size();
                lastBlitQueue = regionUploadQueue.size();
                lastTileCache = TILE_CACHE.size();
                return;
            }

            lastCMinX = cMinX;
            lastCMaxX = cMaxX;
            lastCMinZ = cMinZ;
            lastCMaxZ = cMaxZ;

            if (shouldShowNationArea()) {
                maybeRequestNationOverlay(level, cMinX, cMaxX, cMinZ, cMaxZ);
            }

            for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                for (int cx = cMinX; cx <= cMaxX; cx++) {
                    visible++;

                    long viewKey = tileKey(cx, cz, viewBpp);
                    Tile viewTile = TILE_CACHE.get(viewKey);
                    if (viewTile != null && (viewBpp == 1 || viewTile.sourceBpp == 1)) {
                        Region region = ensureRegion(cx >> 5, cz >> 5, viewBpp);
                        if (region != null) {
                            if (region.paintChunk(cx, cz, viewTile, false)) {
                                enqueueRegionUpload(region);
                            }
                        }
                        continue;
                    } else if (viewTile != null) {
                        TILE_CACHE.remove(viewKey);
                    }

                    long baseKey = tileKey(cx, cz, 1);
                    Tile baseTile = TILE_CACHE.get(baseKey);
                    if (baseTile == null) {
                        if (diskLoadQueued.add(baseKey)) {
                            diskLoadQueue.addLast(baseKey);
                        }
                        ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                        boolean loaded = chunk != null;
                        if (loaded) {
                            present++;
                        } else {
                            missing++;
                        }

                        if (loaded && buildQueued.add(baseKey)) {
                            buildQueue.addLast(baseKey);
                        }
                        continue;
                    }

                    if (viewBpp == 1) {
                        Region region = ensureRegion(cx >> 5, cz >> 5, viewBpp);
                        if (region != null) {
                            if (region.paintChunk(cx, cz, baseTile, false)) {
                                enqueueRegionUpload(region);
                            }
                        }
                        continue;
                    }

                    Tile derived = deriveTileFromBase(baseTile, viewBpp);
                    TILE_CACHE.put(viewKey, derived);
                    Region region = ensureRegion(cx >> 5, cz >> 5, viewBpp);
                    if (region != null) {
                        if (region.paintChunk(cx, cz, derived, true)) {
                            enqueueRegionUpload(region);
                        }
                    }
                }
            }

            lastVisible = visible;
            lastChunkPresent = present;
            lastChunkMissing = missing;
            lastBuildQueue = buildQueue.size();
            lastBlitQueue = regionUploadQueue.size();
            lastTileCache = TILE_CACHE.size();
        }

        private void maybeRequestNationOverlay(ClientLevel level, int cMinX, int cMaxX, int cMinZ, int cMaxZ) {
            StarWorldClientNationOverlayState.Snapshot snap = StarWorldClientNationOverlayState.getSnapshot();
            String dimId = level.dimension().location().toString();
            if (snap != null && snap.covers(dimId, cMinX, cMaxX, cMinZ, cMaxZ)) {
                return;
            }

            long now = Util.getMillis();
            if (now - lastNationRequestMs < 200L) {
                return;
            }

            if (cMinX == lastNationReqMinX && cMaxX == lastNationReqMaxX && cMinZ == lastNationReqMinZ && cMaxZ == lastNationReqMaxZ) {
                return;
            }

            lastNationRequestMs = now;
            lastNationReqMinX = cMinX;
            lastNationReqMaxX = cMaxX;
            lastNationReqMinZ = cMinZ;
            lastNationReqMaxZ = cMaxZ;

            StarWorldNetwork.CHANNEL.sendToServer(new RequestNationOverlayC2SPacket(dimId, cMinX, cMaxX, cMinZ, cMaxZ));
        }

        private void pump(long nanosBudget) {
            if (viewW <= 0 || viewH <= 0 || viewBpp <= 0) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                return;
            }

            long gameTime = level.getGameTime();
            if (gameTime - lastRevalidateGameTime >= REVALIDATE_PERIOD_TICKS) {
                lastRevalidateGameTime = gameTime;
                scheduleRevalidateBatch(REVALIDATE_BATCH);
            }

            long deadline = System.nanoTime() + Math.max(0L, nanosBudget);

            int diskOps = 64;
            while (diskOps-- > 0 && !diskLoadQueue.isEmpty() && System.nanoTime() < deadline) {
                long firstKey = diskLoadQueue.removeFirst();
                diskLoadQueued.remove(firstKey);
                if (TILE_CACHE.containsKey(firstKey)) {
                    continue;
                }

                int bpp = unpackBpp(firstKey);
                if (bpp != 1) {
                    continue;
                }
                int cx0 = unpackChunkX(firstKey);
                int cz0 = unpackChunkZ(firstKey);
                int rx = cx0 >> 5;
                int rz = cz0 >> 5;

                ArrayList<Long> batch = new ArrayList<>(16);
                batch.add(firstKey);

                while (diskOps > 0 && !diskLoadQueue.isEmpty()) {
                    long nextKey = diskLoadQueue.peekFirst();
                    if (unpackBpp(nextKey) != bpp) {
                        break;
                    }
                    int ncx = unpackChunkX(nextKey);
                    int ncz = unpackChunkZ(nextKey);
                    if ((ncx >> 5) != rx || (ncz >> 5) != rz) {
                        break;
                    }
                    diskLoadQueue.removeFirst();
                    diskLoadQueued.remove(nextKey);
                    if (!TILE_CACHE.containsKey(nextKey)) {
                        batch.add(nextKey);
                    }
                    diskOps--;
                    if (System.nanoTime() >= deadline) {
                        break;
                    }
                }

                diskCache.tryLoadBatch(level, rx, rz, bpp, batch, (key, tile) -> {
                    TILE_CACHE.put(key, tile);
                    onTileAvailable(key, tile);
                });
            }

            int revalidateOps = 8;
            while (revalidateOps-- > 0 && !revalidateQueue.isEmpty() && System.nanoTime() < deadline) {
                long key = revalidateQueue.removeFirst();
                revalidateQueued.remove(key);

                int bpp = unpackBpp(key);
                if (bpp != 1) {
                    continue;
                }
                if (DIRTY_TILES.contains(key)) {
                    continue;
                }

                Tile tile = TILE_CACHE.get(key);
                if (tile == null) {
                    continue;
                }

                int cx = unpackChunkX(key);
                int cz = unpackChunkZ(key);
                ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                int newSig = computeSignatureFromWorld(level, cx, cz, 1, 16);
                if (newSig != tile.signature) {
                    DIRTY_TILES.add(key);
                    if (buildQueued.add(key)) {
                        buildQueue.addLast(key);
                    }
                }
            }

            int buildOps = 64;
            while (buildOps-- > 0 && !buildQueue.isEmpty() && System.nanoTime() < deadline) {
                long key = buildQueue.removeFirst();
                buildQueued.remove(key);

                int bpp = unpackBpp(key);
                if (bpp != 1) {
                    DIRTY_TILES.remove(key);
                    continue;
                }
                if (TILE_CACHE.containsKey(key) && !DIRTY_TILES.contains(key)) {
                    continue;
                }

                int cx = unpackChunkX(key);
                int cz = unpackChunkZ(key);

                ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                boolean force = DIRTY_TILES.remove(key);
                Tile tile = buildTile(level, cx, cz, bpp);
                TILE_CACHE.put(key, tile);

                if (diskWriteQueued.add(key)) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            diskCache.tryWrite(level, key, tile);
                        } finally {
                            diskWriteQueued.remove(key);
                        }
                    }, Util.ioPool());
                }

                onTileAvailable(key, tile, force);
            }

            int uploadOps = 8;
            while (uploadOps-- > 0 && !regionUploadQueue.isEmpty() && System.nanoTime() < deadline) {
                long regionKey = regionUploadQueue.removeFirst();
                regionUploadQueued.remove(regionKey);
                Region region = REGION_CACHE.get(regionKey);
                if (region == null) {
                    continue;
                }
                region.uploadIfDirty();
            }

            lastBuildQueue = buildQueue.size();
            lastBlitQueue = regionUploadQueue.size();
            lastTileCache = TILE_CACHE.size();
        }

        private String statsLine() {
            return "vis=" + lastVisible + " present=" + lastChunkPresent + " missing=" + lastChunkMissing + " buildQ=" + lastBuildQueue + " blitQ=" + lastBlitQueue + " tiles=" + lastTileCache;
        }

        private void draw(GuiGraphics graphics, int x, int y) {
            int w = viewW;
            int h = viewH;
            if (w <= 0 || h <= 0 || viewBpp <= 0) {
                return;
            }

            graphics.fill(x, y, x + w, y + h, UNLOADED_RGBA);

            double worldMinX = viewCenterX - (viewW / 2.0) * viewBpp;
            double worldMinZ = viewCenterZ - (viewH / 2.0) * viewBpp;
            double worldMaxX = viewCenterX + (viewW / 2.0) * viewBpp;
            double worldMaxZ = viewCenterZ + (viewH / 2.0) * viewBpp;

            int cMinX = Mth.floor(worldMinX) >> 4;
            int cMaxX = Mth.floor(worldMaxX) >> 4;
            int cMinZ = Mth.floor(worldMinZ) >> 4;
            int cMaxZ = Mth.floor(worldMaxZ) >> 4;

            int rMinX = cMinX >> 5;
            int rMaxX = cMaxX >> 5;
            int rMinZ = cMinZ >> 5;
            int rMaxZ = cMaxZ >> 5;

            int tileSize = Math.max(1, 16 / viewBpp);
            int regionPx = REGION_CHUNKS * tileSize;

            for (int rz = rMinZ; rz <= rMaxZ; rz++) {
                for (int rx = rMinX; rx <= rMaxX; rx++) {
                    long regionKey = regionKey(rx, rz, viewBpp);
                    Region region = REGION_CACHE.get(regionKey);
                    if (region == null || region.textureId == null) {
                        continue;
                    }

                    int regionWorldX = (rx * REGION_CHUNKS) << 4;
                    int regionWorldZ = (rz * REGION_CHUNKS) << 4;
                    int px0 = (int) Math.floor((regionWorldX - worldMinX) / (double) viewBpp);
                    int py0 = (int) Math.floor((regionWorldZ - worldMinZ) / (double) viewBpp);

                    int dx0 = Math.max(0, px0);
                    int dy0 = Math.max(0, py0);
                    int dx1 = Math.min(w, px0 + regionPx);
                    int dy1 = Math.min(h, py0 + regionPx);
                    if (dx1 <= dx0 || dy1 <= dy0) {
                        continue;
                    }

                    int srcX = dx0 - px0;
                    int srcY = dy0 - py0;
                    int drawW = dx1 - dx0;
                    int drawH = dy1 - dy0;
                    graphics.blit(region.textureId, x + dx0, y + dy0, srcX, srcY, drawW, drawH, regionPx, regionPx);
                }
            }
        }

        private Tile buildTile(ClientLevel level, int chunkX, int chunkZ, int bpp) {
            int tileSize = Math.max(1, 16 / bpp);
            int[] pixels = new int[tileSize * tileSize];
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;

            for (int tz = 0; tz < tileSize; tz++) {
                for (int tx = 0; tx < tileSize; tx++) {
                    int wx = baseX + tx * bpp + (bpp >> 1);
                    int wz = baseZ + tz * bpp + (bpp >> 1);

                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
                    if (y <= level.getMinBuildHeight()) {
                        pixels[tz * tileSize + tx] = 0xFFFF00FF;
                        continue;
                    }
                    int by = y - 1;
                    BlockPos pos = new BlockPos(wx, by, wz);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) {
                        pixels[tz * tileSize + tx] = 0xFFFF00FF;
                        continue;
                    }

                    MapColor mapColor = state.getMapColor(level, pos);
                    int rgb = mapColor == null ? 0 : mapColor.col;
                    int argb = 0xFF000000 | (rgb & 0xFFFFFF);
                    pixels[tz * tileSize + tx] = argb;
                }
            }

            return new Tile(tileSize, pixels, 1);
        }

        private int argbToRgba(int argb) {
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = (argb) & 0xFF;
            return (a << 24) | (b << 16) | (g << 8) | r;
        }

        private void scheduleRevalidateBatch(int max) {
            if (viewBpp <= 0) {
                return;
            }
            int cMinX = lastCMinX;
            int cMaxX = lastCMaxX;
            int cMinZ = lastCMinZ;
            int cMaxZ = lastCMaxZ;
            if (cMaxX < cMinX || cMaxZ < cMinZ) {
                return;
            }

            int w = cMaxX - cMinX + 1;
            int h = cMaxZ - cMinZ + 1;
            long total = (long) w * (long) h;
            if (total <= 0 || total > 262144L) {
                return;
            }

            int attempts = Math.min(max, (int) Math.min(Integer.MAX_VALUE, total));
            for (int i = 0; i < attempts; i++) {
                int idx = (int) (Math.floorMod(revalidateCursor++, (int) total));
                int dx = idx % w;
                int dz = idx / w;
                int cx = cMinX + dx;
                int cz = cMinZ + dz;
                long key = tileKey(cx, cz, 1);
                if (DIRTY_TILES.contains(key)) {
                    continue;
                }
                if (!TILE_CACHE.containsKey(key)) {
                    continue;
                }
                if (revalidateQueued.add(key)) {
                    revalidateQueue.addLast(key);
                }
            }
        }

        private int computeSignatureFromWorld(ClientLevel level, int chunkX, int chunkZ, int bpp, int tileSize) {
            int stride = Math.max(1, tileSize / 4);
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;

            int h = 1;
            for (int tz = 0; tz < tileSize; tz += stride) {
                for (int tx = 0; tx < tileSize; tx += stride) {
                    int wx = baseX + tx * bpp + (bpp >> 1);
                    int wz = baseZ + tz * bpp + (bpp >> 1);
                    int argb = sampleWorldArgb(level, wx, wz);
                    h = 31 * h + argb;
                }
            }
            return h;
        }

        private int sampleWorldArgb(ClientLevel level, int wx, int wz) {
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
            if (y <= level.getMinBuildHeight()) {
                return 0xFFFF00FF;
            }
            BlockPos pos = new BlockPos(wx, y - 1, wz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return 0xFFFF00FF;
            }
            MapColor mapColor = state.getMapColor(level, pos);
            int rgb = mapColor == null ? 0 : mapColor.col;
            return 0xFF000000 | (rgb & 0xFFFFFF);
        }

        private void close() {
            buildQueue.clear();
            buildQueued.clear();
            diskLoadQueue.clear();
            diskLoadQueued.clear();
            regionUploadQueue.clear();
            regionUploadQueued.clear();
        }

        private boolean onTileAvailable(long key, Tile tile) {
            int bpp = unpackBpp(key);
            int cx = unpackChunkX(key);
            int cz = unpackChunkZ(key);

            if (bpp == 1) {
                invalidateDerivedForChunk(cx, cz);
                if (viewBpp == 1) {
                    Region region = ensureRegion(cx >> 5, cz >> 5, 1);
                    if (region == null) {
                        return false;
                    }
                    boolean painted = region.paintChunk(cx, cz, tile, false);
                    if (painted) {
                        enqueueRegionUpload(region);
                    }
                    return painted;
                }

                Tile derived = deriveTileFromBase(tile, viewBpp);
                long viewKey = tileKey(cx, cz, viewBpp);
                TILE_CACHE.put(viewKey, derived);
                Region region = ensureRegion(cx >> 5, cz >> 5, viewBpp);
                if (region == null) {
                    return false;
                }
                boolean painted = region.paintChunk(cx, cz, derived, true);
                if (painted) {
                    enqueueRegionUpload(region);
                }
                return painted;
            }

            if (bpp != viewBpp) {
                return false;
            }
            Region region = ensureRegion(cx >> 5, cz >> 5, bpp);
            if (region == null) {
                return false;
            }
            boolean painted = region.paintChunk(cx, cz, tile, false);
            if (painted) {
                enqueueRegionUpload(region);
            }
            return painted;
        }

        private boolean onTileAvailable(long key, Tile tile, boolean force) {
            int bpp = unpackBpp(key);
            int cx = unpackChunkX(key);
            int cz = unpackChunkZ(key);

            if (bpp == 1) {
                invalidateDerivedForChunk(cx, cz);
                if (viewBpp == 1) {
                    Region region = ensureRegion(cx >> 5, cz >> 5, 1);
                    if (region == null) {
                        return false;
                    }
                    boolean painted = region.paintChunk(cx, cz, tile, force);
                    if (painted) {
                        enqueueRegionUpload(region);
                    }
                    return painted;
                }

                Tile derived = deriveTileFromBase(tile, viewBpp);
                long viewKey = tileKey(cx, cz, viewBpp);
                TILE_CACHE.put(viewKey, derived);
                Region region = ensureRegion(cx >> 5, cz >> 5, viewBpp);
                if (region == null) {
                    return false;
                }
                boolean painted = region.paintChunk(cx, cz, derived, true);
                if (painted) {
                    enqueueRegionUpload(region);
                }
                return painted;
            }

            if (bpp != viewBpp) {
                return false;
            }
            Region region = ensureRegion(cx >> 5, cz >> 5, bpp);
            if (region == null) {
                return false;
            }
            boolean painted = region.paintChunk(cx, cz, tile, force);
            if (painted) {
                enqueueRegionUpload(region);
            }
            return painted;
        }

        private void enqueueRegionUpload(Region region) {
            long k = region.key;
            if (regionUploadQueued.add(k)) {
                regionUploadQueue.addLast(k);
            }
        }

        private Region ensureRegion(int rx, int rz, int bpp) {
            long key = regionKey(rx, rz, bpp);
            Region existing = REGION_CACHE.get(key);
            if (existing != null) {
                return existing;
            }

            Region region = new Region(key, rx, rz, bpp);
            REGION_CACHE.put(key, region);

            if (REGION_CACHE.size() > MAX_REGIONS) {
                Iterator<Entry<Long, Region>> it = REGION_CACHE.entrySet().iterator();
                while (REGION_CACHE.size() > MAX_REGIONS && it.hasNext()) {
                    Entry<Long, Region> e = it.next();
                    Region r = e.getValue();
                    it.remove();
                    r.close();
                }
            }

            return region;
        }

        private void invalidateDerivedForChunk(int chunkX, int chunkZ) {
            int rx = chunkX >> 5;
            int rz = chunkZ >> 5;
            for (int bpp : BLOCKS_PER_PIXEL_LEVELS) {
                if (bpp == 1) {
                    continue;
                }
                long k = tileKey(chunkX, chunkZ, bpp);
                TILE_CACHE.remove(k);
                DIRTY_TILES.remove(k);

                Region region = REGION_CACHE.get(regionKey(rx, rz, bpp));
                if (region != null) {
                    region.invalidateChunk(chunkX, chunkZ);
                }
            }
        }

        private Tile deriveTileFromBase(Tile base, int bpp) {
            if (bpp <= 1) {
                return base;
            }
            int tileSize = Math.max(1, 16 / bpp);
            int[] pixels = new int[tileSize * tileSize];

            int baseSize = base.size;
            int factor = 16 / tileSize;
            if (baseSize != 16 || factor <= 0) {
                return base;
            }

            for (int tz = 0; tz < tileSize; tz++) {
                for (int tx = 0; tx < tileSize; tx++) {
                    int sumR = 0;
                    int sumG = 0;
                    int sumB = 0;
                    int count = 0;
                    for (int dz = 0; dz < factor; dz++) {
                        for (int dx = 0; dx < factor; dx++) {
                            int argb = base.pixels[(tz * factor + dz) * 16 + (tx * factor + dx)];
                            if (argb == 0xFFFF00FF) {
                                continue;
                            }
                            sumR += (argb >>> 16) & 0xFF;
                            sumG += (argb >>> 8) & 0xFF;
                            sumB += (argb) & 0xFF;
                            count++;
                        }
                    }
                    if (count <= 0) {
                        pixels[tz * tileSize + tx] = 0xFFFF00FF;
                    } else {
                        int r = sumR / count;
                        int g = sumG / count;
                        int b = sumB / count;
                        pixels[tz * tileSize + tx] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                }
            }

            return new Tile(tileSize, pixels);
        }

        private long regionKey(int rx, int rz, int bpp) {
            long kb = (long) bpp & 0xFFFFL;
            long kx = (long) rx & 0xFFFFFFL;
            long kz = (long) rz & 0xFFFFFFL;
            return (kb << 48) | (kx << 24) | kz;
        }

        private final class Region {
            private final long key;
            private final int tileSize;
            private final int regionPx;
            private final BitSet painted = new BitSet(REGION_CHUNKS * REGION_CHUNKS);

            private NativeImage image;
            private DynamicTexture texture;
            private ResourceLocation textureId;
            private boolean dirty;

            private Region(long key, int rx, int rz, int bpp) {
                this.key = key;
                this.tileSize = Math.max(1, 16 / bpp);
                this.regionPx = REGION_CHUNKS * tileSize;

                this.image = new NativeImage(regionPx, regionPx, false);
                this.image.fillRect(0, 0, regionPx, regionPx, UNLOADED_RGBA);
                this.texture = new DynamicTexture(image);
                this.textureId = Minecraft.getInstance().getTextureManager().register("starworld_debug_map_region_" + key, texture);
                this.texture.upload();
                this.dirty = false;
            }

            private boolean paintChunk(int chunkX, int chunkZ, Tile tile, boolean force) {
                if (tile == null || tile.size != tileSize || image == null) {
                    return false;
                }

                int localX = chunkX & 31;
                int localZ = chunkZ & 31;
                int idx = localZ * REGION_CHUNKS + localX;
                if (!force && painted.get(idx)) {
                    return false;
                }
                painted.set(idx);

                int px0 = localX * tileSize;
                int py0 = localZ * tileSize;
                for (int ty = 0; ty < tileSize; ty++) {
                    for (int tx = 0; tx < tileSize; tx++) {
                        int argb = tile.pixels[ty * tileSize + tx];
                        image.setPixelRGBA(px0 + tx, py0 + ty, argbToRgba(argb));
                    }
                }
                dirty = true;
                return true;
            }

            private void invalidateChunk(int chunkX, int chunkZ) {
                int localX = chunkX & 31;
                int localZ = chunkZ & 31;
                int idx = localZ * REGION_CHUNKS + localX;
                painted.clear(idx);
            }

            private boolean uploadIfDirty() {
                if (!dirty || texture == null) {
                    return false;
                }
                texture.upload();
                dirty = false;
                return true;
            }

            private void close() {
                if (textureId != null) {
                    Minecraft.getInstance().getTextureManager().release(textureId);
                    textureId = null;
                }
                if (texture != null) {
                    texture.close();
                    texture = null;
                }
                if (image != null) {
                    image.close();
                    image = null;
                }
            }
        }

        private final class TileDiskCache {
            private static final int MAGIC = 0x53574D50;
            private static final int VERSION_REGION = 2;
            private static final int VERSION_CHUNK = 1;
            private static final int REGION_TILE_COUNT = 32 * 32;
            private static final int REGION_MASK_BYTES = REGION_TILE_COUNT / 8;
            private static final int REGION_SIG_BYTES = REGION_TILE_COUNT * 4;
            private static final int REGION_HEADER_BYTES = 24;

            private volatile Path baseDir;
            private volatile String baseKey;

            private final ConcurrentHashMap<Long, Object> regionWriteLocks = new ConcurrentHashMap<>();

            private Tile tryLoad(ClientLevel level, long key) {
                Tile regionTile = tryLoadFromRegionFile(level, key);
                if (regionTile != null) {
                    return regionTile;
                }
                return tryLoadFromChunkFile(level, key);
            }

            private Tile tryLoadFromChunkFile(ClientLevel level, long key) {
                try {
                    Path file = tileChunkFile(level, key);
                    if (file == null || !Files.exists(file)) {
                        return null;
                    }

                    try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                        int magic = in.readInt();
                        if (magic != MAGIC) {
                            return null;
                        }
                        int ver = in.readInt();
                        if (ver != VERSION_CHUNK) {
                            return null;
                        }
                        int size = in.readInt();
                        int len = in.readInt();
                        if (size <= 0 || len != size * size) {
                            return null;
                        }
                        int[] pixels = new int[len];
                        for (int i = 0; i < len; i++) {
                            pixels[i] = in.readInt();
                        }
                        return new Tile(size, pixels);
                    }
                } catch (Throwable t) {
                    return null;
                }
            }

            private Tile tryLoadFromRegionFile(ClientLevel level, long key) {
                try {
                    Path file = tileRegionFile(level, key);
                    if (file == null || !Files.exists(file)) {
                        return null;
                    }
                    int bpp = unpackBpp(key);
                    int tileSize = Math.max(1, 16 / bpp);
                    int tileLen = tileSize * tileSize;

                    int cx = unpackChunkX(key);
                    int cz = unpackChunkZ(key);
                    int localX = cx & 31;
                    int localZ = cz & 31;
                    int idx = localZ * 32 + localX;

                    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                        if (!readAndValidateRegionHeader(raf, bpp, tileSize, tileLen)) {
                            return null;
                        }
                        if (!isRegionTilePresent(raf, idx)) {
                            return null;
                        }
                        int[] pixels = readRegionTilePixels(raf, idx, tileLen);
                        if (pixels == null) {
                            return null;
                        }
                        return new Tile(tileSize, pixels);
                    }
                } catch (Throwable t) {
                    return null;
                }
            }

            private interface TileConsumer {
                void accept(long key, Tile tile);
            }

            private void tryLoadBatch(ClientLevel level, int rx, int rz, int bpp, List<Long> keys, TileConsumer consumer) {
                if (keys == null || keys.isEmpty()) {
                    return;
                }
                try {
                    Path root = getBaseDir(level);
                    if (root == null) {
                        return;
                    }
                    Path regionDir = root.resolve("r." + rx + "." + rz);
                    Path file = regionDir.resolve("tiles." + bpp + ".bin");
                    if (!Files.exists(file)) {
                        for (long key : keys) {
                            Tile tile = tryLoadFromChunkFile(level, key);
                            if (tile != null) {
                                consumer.accept(key, tile);
                            }
                        }
                        return;
                    }

                    int tileSize = Math.max(1, 16 / bpp);
                    int tileLen = tileSize * tileSize;

                    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                        if (!readAndValidateRegionHeader(raf, bpp, tileSize, tileLen)) {
                            for (long key : keys) {
                                Tile tile = tryLoadFromChunkFile(level, key);
                                if (tile != null) {
                                    consumer.accept(key, tile);
                                }
                            }
                            return;
                        }

                        for (long key : keys) {
                            int cx = unpackChunkX(key);
                            int cz = unpackChunkZ(key);
                            int idx = (cz & 31) * 32 + (cx & 31);
                            if (!isRegionTilePresent(raf, idx)) {
                                Tile tile = tryLoadFromChunkFile(level, key);
                                if (tile != null) {
                                    consumer.accept(key, tile);
                                }
                                continue;
                            }
                            int[] pixels = readRegionTilePixels(raf, idx, tileLen);
                            if (pixels == null) {
                                continue;
                            }
                            consumer.accept(key, new Tile(tileSize, pixels));
                        }
                    }
                } catch (Throwable t) {
                }
            }

            private void tryWrite(ClientLevel level, long key, Tile tile) {
                try {
                    Path file = tileRegionFile(level, key);
                    if (file == null) {
                        return;
                    }
                    Path parent = file.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }

                    int bpp = unpackBpp(key);
                    if (bpp != 1) {
                        return;
                    }
                    int tileSize = Math.max(1, 16 / bpp);
                    int tileLen = tileSize * tileSize;
                    if (tile.size != tileSize || tile.pixels.length != tileLen) {
                        return;
                    }

                    int cx = unpackChunkX(key);
                    int cz = unpackChunkZ(key);
                    int idx = (cz & 31) * 32 + (cx & 31);

                    long regionKey = (((long) bpp) << 48) | (((long) (cx >> 5) & 0xFFFFFFL) << 24) | ((long) (cz >> 5) & 0xFFFFFFL);
                    Object lock = regionWriteLocks.computeIfAbsent(regionKey, k -> new Object());
                    synchronized (lock) {
                        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
                            ensureRegionFileInitialized(raf, bpp, tileSize, tileLen);
                            writeRegionTile(raf, idx, tile, tileLen);
                        }
                    }
                } catch (Throwable t) {
                }
            }

            private Path tileChunkFile(ClientLevel level, long key) {
                if (level == null) {
                    return null;
                }
                Path root = getBaseDir(level);
                if (root == null) {
                    return null;
                }
                int bpp = unpackBpp(key);
                int cx = unpackChunkX(key);
                int cz = unpackChunkZ(key);
                int rx = cx >> 5;
                int rz = cz >> 5;
                Path regionDir = root.resolve("r." + rx + "." + rz);
                return regionDir.resolve("c." + cx + "." + cz + "." + bpp + ".bin");
            }

            private Path tileRegionFile(ClientLevel level, long key) {
                if (level == null) {
                    return null;
                }
                Path root = getBaseDir(level);
                if (root == null) {
                    return null;
                }
                int bpp = unpackBpp(key);
                int cx = unpackChunkX(key);
                int cz = unpackChunkZ(key);
                int rx = cx >> 5;
                int rz = cz >> 5;
                Path regionDir = root.resolve("r." + rx + "." + rz);
                return regionDir.resolve("tiles." + bpp + ".bin");
            }

            private boolean readAndValidateRegionHeader(RandomAccessFile raf, int bpp, int tileSize, int tileLen) {
                try {
                    raf.seek(0L);
                    int magic = raf.readInt();
                    if (magic != MAGIC) {
                        return false;
                    }
                    int ver = raf.readInt();
                    if (ver != VERSION_REGION) {
                        return false;
                    }
                    int fbpp = raf.readInt();
                    int fsize = raf.readInt();
                    int flen = raf.readInt();
                    raf.readInt();
                    return fbpp == bpp && fsize == tileSize && flen == tileLen;
                } catch (Throwable t) {
                    return false;
                }
            }

            private void ensureRegionFileInitialized(RandomAccessFile raf, int bpp, int tileSize, int tileLen) {
                try {
                    long expectedLen = regionFileTotalBytes(tileLen);
                    if (raf.length() != expectedLen) {
                        raf.setLength(expectedLen);
                    }
                    raf.seek(0L);
                    int magic = raf.readInt();
                    if (magic == MAGIC) {
                        raf.seek(0L);
                        if (readAndValidateRegionHeader(raf, bpp, tileSize, tileLen)) {
                            return;
                        }
                    }

                    raf.seek(0L);
                    raf.writeInt(MAGIC);
                    raf.writeInt(VERSION_REGION);
                    raf.writeInt(bpp);
                    raf.writeInt(tileSize);
                    raf.writeInt(tileLen);
                    raf.writeInt(0);
                } catch (Throwable t) {
                }
            }

            private long regionFileTotalBytes(int tileLen) {
                return (long) REGION_HEADER_BYTES + (long) REGION_MASK_BYTES + (long) REGION_SIG_BYTES + (long) REGION_TILE_COUNT * (long) tileLen * 4L;
            }

            private boolean isRegionTilePresent(RandomAccessFile raf, int idx) {
                try {
                    int byteIndex = idx >> 3;
                    int bit = idx & 7;
                    raf.seek((long) REGION_HEADER_BYTES + (long) byteIndex);
                    int v = raf.read();
                    if (v < 0) {
                        return false;
                    }
                    return ((v >>> bit) & 1) != 0;
                } catch (Throwable t) {
                    return false;
                }
            }

            private int[] readRegionTilePixels(RandomAccessFile raf, int idx, int tileLen) {
                try {
                    long dataOffset = (long) REGION_HEADER_BYTES + (long) REGION_MASK_BYTES + (long) REGION_SIG_BYTES;
                    long pos = dataOffset + (long) idx * (long) tileLen * 4L;
                    raf.seek(pos);
                    int[] pixels = new int[tileLen];
                    for (int i = 0; i < tileLen; i++) {
                        pixels[i] = raf.readInt();
                    }
                    return pixels;
                } catch (Throwable t) {
                    return null;
                }
            }

            private void writeRegionTile(RandomAccessFile raf, int idx, Tile tile, int tileLen) {
                try {
                    int byteIndex = idx >> 3;
                    int bit = idx & 7;
                    long maskPos = (long) REGION_HEADER_BYTES + (long) byteIndex;
                    raf.seek(maskPos);
                    int cur = raf.read();
                    if (cur < 0) {
                        cur = 0;
                    }
                    int next = cur | (1 << bit);
                    raf.seek(maskPos);
                    raf.write(next);

                    long sigBase = (long) REGION_HEADER_BYTES + (long) REGION_MASK_BYTES;
                    raf.seek(sigBase + (long) idx * 4L);
                    raf.writeInt(tile.signature);

                    long dataOffset = sigBase + (long) REGION_SIG_BYTES;
                    long pos = dataOffset + (long) idx * (long) tileLen * 4L;
                    raf.seek(pos);
                    for (int i = 0; i < tileLen; i++) {
                        raf.writeInt(tile.pixels[i]);
                    }
                } catch (Throwable t) {
                }
            }

            private Path getBaseDir(ClientLevel level) {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    String serverKey = "singleplayer";
                    if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null && !mc.getCurrentServer().ip.isEmpty()) {
                        serverKey = mc.getCurrentServer().ip;
                    }
                    serverKey = sanitize(serverKey);
                    String dimKey = sanitize(level.dimension().location().toString());

                    String newBaseKey = serverKey + "|" + dimKey;
                    Path cached = baseDir;
                    if (cached != null && newBaseKey.equals(baseKey)) {
                        return cached;
                    }

                    Path dir = FMLPaths.CONFIGDIR.get().resolve("starworld").resolve("map_cache").resolve(serverKey).resolve(dimKey);
                    Files.createDirectories(dir);
                    baseDir = dir;
                    baseKey = newBaseKey;
                    return dir;
                } catch (Throwable t) {
                    return null;
                }
            }

            private String sanitize(String s) {
                if (s == null || s.isEmpty()) {
                    return "unknown";
                }
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if ((c >= 'a' && c <= 'z')
                            || (c >= 'A' && c <= 'Z')
                            || (c >= '0' && c <= '9')
                            || c == '.' || c == '_' || c == '-') {
                        sb.append(c);
                    } else {
                        sb.append('_');
                    }
                }
                return sb.toString();
            }
        }

        private long tileKey(int chunkX, int chunkZ, int bpp) {
            long kx = (long) chunkX & 0x3FFFFFL;
            long kz = (long) chunkZ & 0x3FFFFFL;
            long kb = (long) bpp & 0xFFFFL;
            return (kb << 48) | (kx << 24) | kz;
        }

        private int unpackBpp(long key) {
            return (int) ((key >>> 48) & 0xFFFFL);
        }

        private int unpackChunkX(long key) {
            int v = (int) ((key >>> 24) & 0x3FFFFFL);
            if ((v & (1 << 21)) != 0) {
                v |= ~0x3FFFFF;
            }
            return v;
        }

        private int unpackChunkZ(long key) {
            int v = (int) (key & 0x3FFFFFL);
            if ((v & (1 << 21)) != 0) {
                v |= ~0x3FFFFF;
            }
            return v;
        }

        private final class Tile {
            private final int size;
            private final int[] pixels;
            private final int signature;
            private final int sourceBpp;

            private Tile(int size, int[] pixels) {
                this(size, pixels, size == 16 ? 1 : 0);
            }

            private Tile(int size, int[] pixels, int sourceBpp) {
                this.size = size;
                this.pixels = pixels;
                this.signature = computeSignatureFromTile(pixels, size);
                this.sourceBpp = sourceBpp;
            }
        }

        private int computeSignatureFromTile(int[] pixels, int size) {
            int stride = Math.max(1, size / 4);
            int h = 1;
            for (int tz = 0; tz < size; tz += stride) {
                for (int tx = 0; tx < size; tx += stride) {
                    int argb = pixels[tz * size + tx];
                    h = 31 * h + argb;
                }
            }
            return h;
        }
    }

    private static double snapCenter(double value, int bpp) {
        if (bpp <= 1) {
            return Math.round(value);
        }
        return Math.round(value / (double) bpp) * (double) bpp;
    }

    private static void markChunkDirty(int chunkX, int chunkZ) {
        long kx = (long) chunkX & 0x3FFFFFL;
        long kz = (long) chunkZ & 0x3FFFFFL;
        long kb = 1L;
        long key = (kb << 48) | (kx << 24) | kz;
        DIRTY_TILES.add(key);
    }

    @Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    private static final class MapDirtyClientEvents {
        private MapDirtyClientEvents() {
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!event.getLevel().isClientSide) {
                return;
            }
            markChunkDirty(event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getLevel().isClientSide) {
                return;
            }
            markChunkDirty(event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
        }
    }
}
