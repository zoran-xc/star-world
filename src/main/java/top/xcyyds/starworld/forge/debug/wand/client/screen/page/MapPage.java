package top.xcyyds.starworld.forge.debug.wand.client.screen.page;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.lwjgl.glfw.GLFW;
import top.xcyyds.starworld.forge.debug.wand.client.screen.DebugWandDebugScreen;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("null")
public final class MapPage implements DebugPage {
    private static final int[] BLOCKS_PER_PIXEL_LEVELS = new int[]{1, 2, 4, 8, 16};

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

    private MapRenderer renderer;

    public MapPage(DebugWandDebugScreen screen) {
        this.screen = screen;
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
        renderer.ensureTexture(mapW, mapH);
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
            renderer.tick();
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
        if (renderer != null) {
            renderer.ensureTexture(mapW, mapH);
            renderer.setView(centerX, centerZ, bpp, mapW, mapH);
            renderer.draw(graphics, mapLeft, mapTop);
        } else {
            graphics.fill(mapLeft, mapTop, mapRight, mapBottom, 0xAA000000);
        }

        int hoverWorldX;
        int hoverWorldZ;
        if (isInMap(mouseX, mouseY)) {
            double worldMinX = centerX - (mapW / 2.0) * bpp;
            double worldMinZ = centerZ - (mapH / 2.0) * bpp;
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

        drawPlayerMarker(graphics, bpp);

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

        int oldBpp = blocksPerPixel();
        double worldMinX = centerX - (mapW / 2.0) * oldBpp;
        double worldMinZ = centerZ - (mapH / 2.0) * oldBpp;
        double anchorX = worldMinX + (mouseX - mapLeft) * (double) oldBpp;
        double anchorZ = worldMinZ + (mouseY - mapTop) * (double) oldBpp;

        if (delta > 0) {
            blocksPerPixelIndex = Math.max(0, blocksPerPixelIndex - 1);
        } else {
            blocksPerPixelIndex = Math.min(BLOCKS_PER_PIXEL_LEVELS.length - 1, blocksPerPixelIndex + 1);
        }

        int newBpp = blocksPerPixel();
        double newWorldMinX = anchorX - (mouseX - mapLeft) * (double) newBpp;
        double newWorldMinZ = anchorZ - (mouseY - mapTop) * (double) newBpp;
        centerX = newWorldMinX + (mapW / 2.0) * newBpp;
        centerZ = newWorldMinZ + (mapH / 2.0) * newBpp;
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
        return BLOCKS_PER_PIXEL_LEVELS[blocksPerPixelIndex];
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
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        double worldMinX = centerX - (mapW / 2.0) * bpp;
        double worldMinZ = centerZ - (mapH / 2.0) * bpp;
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
        private final Map<Long, Tile> tileCache = new HashMap<>();

        private final ArrayDeque<Long> buildQueue = new ArrayDeque<>();
        private final Set<Long> buildQueued = new HashSet<>();
        private final ArrayDeque<Long> blitQueue = new ArrayDeque<>();
        private final Set<Long> blitQueued = new HashSet<>();

        private NativeImage image;
        private DynamicTexture texture;
        private ResourceLocation textureId;

        private double viewCenterX;
        private double viewCenterZ;
        private int viewBpp;
        private int viewW;
        private int viewH;
        private long viewKey;

        private void ensureTexture(int w, int h) {
            if (w <= 0 || h <= 0) {
                return;
            }
            if (image != null && image.getWidth() == w && image.getHeight() == h) {
                return;
            }
            closeTexture();

            image = new NativeImage(w, h, false);
            image.fillRect(0, 0, w, h, 0x00000000);

            texture = new DynamicTexture(image);
            textureId = Minecraft.getInstance().getTextureManager().register("starworld_debug_map_" + System.nanoTime(), texture);
            texture.upload();
        }

        private void setView(double cx, double cz, int bpp, int w, int h) {
            long newKey = (((long) bpp) << 48) ^ ((long) Math.floor(cx) << 24) ^ (long) Math.floor(cz) ^ ((long) w << 8) ^ (long) h;
            boolean changed = newKey != viewKey;
            viewKey = newKey;

            viewCenterX = cx;
            viewCenterZ = cz;
            viewBpp = bpp;
            viewW = w;
            viewH = h;

            if (changed && image != null) {
                image.fillRect(0, 0, image.getWidth(), image.getHeight(), 0x00000000);
                blitQueue.clear();
                blitQueued.clear();
                buildQueue.clear();
                buildQueued.clear();
            }

            scheduleVisibleChunks();
        }

        private void scheduleVisibleChunks() {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                return;
            }
            if (image == null) {
                return;
            }

            double worldMinX = viewCenterX - (viewW / 2.0) * viewBpp;
            double worldMinZ = viewCenterZ - (viewH / 2.0) * viewBpp;
            double worldMaxX = viewCenterX + (viewW / 2.0) * viewBpp;
            double worldMaxZ = viewCenterZ + (viewH / 2.0) * viewBpp;

            int cMinX = Mth.floor(worldMinX) >> 4;
            int cMaxX = Mth.floor(worldMaxX) >> 4;
            int cMinZ = Mth.floor(worldMinZ) >> 4;
            int cMaxZ = Mth.floor(worldMaxZ) >> 4;

            for (int cz = cMinZ; cz <= cMaxZ; cz++) {
                for (int cx = cMinX; cx <= cMaxX; cx++) {
                    long key = tileKey(cx, cz, viewBpp);
                    Tile tile = tileCache.get(key);
                    if (tile == null) {
                        ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                        if (chunk != null && buildQueued.add(key)) {
                            buildQueue.addLast(key);
                        }
                    }
                    if (blitQueued.add(key)) {
                        blitQueue.addLast(key);
                    }
                }
            }
        }

        private void tick() {
            if (image == null || texture == null) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                return;
            }

            boolean changed = false;

            int buildBudget = 1;
            while (buildBudget-- > 0 && !buildQueue.isEmpty()) {
                long key = buildQueue.removeFirst();
                buildQueued.remove(key);
                if (tileCache.containsKey(key)) {
                    continue;
                }

                int bpp = unpackBpp(key);
                int cx = unpackChunkX(key);
                int cz = unpackChunkZ(key);
                ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                Tile tile = buildTile(level, cx, cz, bpp);
                tileCache.put(key, tile);

                if (blitQueued.add(key)) {
                    blitQueue.addLast(key);
                }
            }

            int blitBudget = 10;
            while (blitBudget-- > 0 && !blitQueue.isEmpty()) {
                long key = blitQueue.removeFirst();

                Tile tile = tileCache.get(key);
                if (tile == null) {
                    blitQueued.remove(key);
                    continue;
                }

                int bpp = unpackBpp(key);
                if (bpp != viewBpp) {
                    blitQueued.remove(key);
                    continue;
                }

                int cx = unpackChunkX(key);
                int cz = unpackChunkZ(key);
                blitTile(tile, cx, cz);
                changed = true;
            }

            if (changed) {
                texture.upload();
            }
        }

        private void draw(GuiGraphics graphics, int x, int y) {
            if (textureId == null || image == null) {
                return;
            }
            graphics.enableScissor(x, y, x + image.getWidth(), y + image.getHeight());
            graphics.blit(textureId, x, y, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight());
            graphics.disableScissor();
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
                    int by = Math.max(level.getMinBuildHeight(), y - 1);
                    BlockPos pos = new BlockPos(wx, by, wz);
                    BlockState state = level.getBlockState(pos);

                    MapColor mapColor = state.getMapColor(level, pos);
                    int rgb = mapColor == null ? 0 : mapColor.col;
                    int argb = 0xFF000000 | (rgb & 0xFFFFFF);
                    pixels[tz * tileSize + tx] = argb;
                }
            }

            return new Tile(tileSize, pixels);
        }

        private void blitTile(Tile tile, int chunkX, int chunkZ) {
            if (image == null) {
                return;
            }

            double worldMinX = viewCenterX - (viewW / 2.0) * viewBpp;
            double worldMinZ = viewCenterZ - (viewH / 2.0) * viewBpp;

            int chunkWorldX = chunkX << 4;
            int chunkWorldZ = chunkZ << 4;

            int px0 = (int) Math.floor((chunkWorldX - worldMinX) / (double) viewBpp);
            int py0 = (int) Math.floor((chunkWorldZ - worldMinZ) / (double) viewBpp);

            int tileSize = tile.size;
            for (int ty = 0; ty < tileSize; ty++) {
                int py = py0 + ty;
                if (py < 0 || py >= image.getHeight()) {
                    continue;
                }
                for (int tx = 0; tx < tileSize; tx++) {
                    int px = px0 + tx;
                    if (px < 0 || px >= image.getWidth()) {
                        continue;
                    }
                    int argb = tile.pixels[ty * tileSize + tx];
                    int rgba = argbToRgba(argb);
                    image.setPixelRGBA(px, py, rgba);
                }
            }
        }

        private int argbToRgba(int argb) {
            int a = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = (argb) & 0xFF;
            return (a << 24) | (b << 16) | (g << 8) | r;
        }

        private void close() {
            closeTexture();
            tileCache.clear();
            buildQueue.clear();
            buildQueued.clear();
            blitQueue.clear();
            blitQueued.clear();
        }

        private void closeTexture() {
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

            private Tile(int size, int[] pixels) {
                this.size = size;
                this.pixels = pixels;
            }
        }
    }
}
