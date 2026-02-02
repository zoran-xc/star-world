package top.xcyyds.starworld.forge.nation.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class StarWorldClientNationOverlayState {
    public static final int TILE_SIZE_CHUNKS = 128;
    private static final int MAX_TILES_PER_DIM = 64;

    private static final Object LOCK = new Object();
    private static final Map<TileKey, Tile> TILES = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<TileKey, Tile> eldest) {
            return false;
        }
    };
    private static final Map<String, List<Label>> LABELS_BY_DIM = new LinkedHashMap<>(4, 0.75f, true);

    private StarWorldClientNationOverlayState() {
    }

    public record Segment(int x0, int z0, int x1, int z1) {
    }

    public record Label(int id, int colorRgb, String zhName, String enName, int capitalX, int capitalZ) {
        public String displayName() {
            if (zhName != null && !zhName.isEmpty()) {
                return zhName;
            }
            return enName == null ? "" : enName;
        }
    }

    public record TileKey(String dimId, int tileX, int tileZ) {
    }

    public record Tile(TileKey key, int cMinX, int cMaxX, int cMinZ, int cMaxZ, int[] nationIds, List<Segment> segments, long receivedAtMs) {
        public Tile {
            segments = Collections.unmodifiableList(new ArrayList<>(segments));
        }

        public boolean containsChunk(int cx, int cz) {
            return cx >= cMinX && cx <= cMaxX && cz >= cMinZ && cz <= cMaxZ;
        }

        public int width() {
            return cMaxX - cMinX + 1;
        }

        public int height() {
            return cMaxZ - cMinZ + 1;
        }

        public int nationIdAtChunk(int cx, int cz) {
            if (!containsChunk(cx, cz)) {
                return 0;
            }
            int w = width();
            int h = height();
            if (w <= 0 || h <= 0) {
                return 0;
            }
            if (nationIds == null || nationIds.length != w * h) {
                return 0;
            }
            int dx = cx - cMinX;
            int dz = cz - cMinZ;
            int idx = dz * w + dx;
            if (idx < 0 || idx >= nationIds.length) {
                return 0;
            }
            return nationIds[idx];
        }
    }

    public record View(List<Tile> tiles, List<Label> labels) {
        public View {
            tiles = Collections.unmodifiableList(new ArrayList<>(tiles));
            labels = Collections.unmodifiableList(new ArrayList<>(labels));
        }

        public Label labelById(int id) {
            if (id == 0) {
                return null;
            }
            for (Label l : labels) {
                if (l.id() == id) {
                    return l;
                }
            }
            return null;
        }
    }

    public static void applyTile(String dimId, int cMinX, int cMaxX, int cMinZ, int cMaxZ, int[] nationIds, List<Segment> segments, List<Label> labels) {
        if (dimId == null) {
            return;
        }
        int tileX = Math.floorDiv(cMinX, TILE_SIZE_CHUNKS);
        int tileZ = Math.floorDiv(cMinZ, TILE_SIZE_CHUNKS);
        TileKey key = new TileKey(dimId, tileX, tileZ);

        synchronized (LOCK) {
            // cap tiles per dimension
            int perDim = 0;
            for (TileKey k : TILES.keySet()) {
                if (dimId.equals(k.dimId())) {
                    perDim++;
                }
            }
            if (perDim >= MAX_TILES_PER_DIM) {
                // evict oldest tile for this dim
                TileKey eldest = null;
                for (TileKey k : TILES.keySet()) {
                    if (dimId.equals(k.dimId())) {
                        eldest = k;
                        break;
                    }
                }
                if (eldest != null) {
                    TILES.remove(eldest);
                }
            }

            TILES.put(key, new Tile(key, cMinX, cMaxX, cMinZ, cMaxZ, nationIds, segments, System.currentTimeMillis()));
            LABELS_BY_DIM.put(dimId, Collections.unmodifiableList(new ArrayList<>(labels)));
        }
    }

    public static boolean hasTile(String dimId, int tileX, int tileZ) {
        if (dimId == null) {
            return false;
        }
        synchronized (LOCK) {
            return TILES.containsKey(new TileKey(dimId, tileX, tileZ));
        }
    }

    public static View getView(String dimId, int cMinX, int cMaxX, int cMinZ, int cMaxZ) {
        if (dimId == null) {
            return null;
        }
        List<Tile> tiles = new ArrayList<>();
        List<Label> labels;
        synchronized (LOCK) {
            for (Tile t : TILES.values()) {
                if (!dimId.equals(t.key().dimId())) {
                    continue;
                }
                if (t.cMaxX() < cMinX || t.cMinX() > cMaxX || t.cMaxZ() < cMinZ || t.cMinZ() > cMaxZ) {
                    continue;
                }
                tiles.add(t);
            }
            labels = LABELS_BY_DIM.getOrDefault(dimId, List.of());
        }
        return new View(tiles, labels);
    }

    public static int nationIdAtWorld(String dimId, int wx, int wz) {
        if (dimId == null) {
            return 0;
        }
        int cx = wx >> 4;
        int cz = wz >> 4;
        int tileX = Math.floorDiv(cx, TILE_SIZE_CHUNKS);
        int tileZ = Math.floorDiv(cz, TILE_SIZE_CHUNKS);
        Tile t;
        synchronized (LOCK) {
            t = TILES.get(new TileKey(dimId, tileX, tileZ));
        }
        if (t == null) {
            return 0;
        }
        return t.nationIdAtChunk(cx, cz);
    }

    public static Label labelById(String dimId, int id) {
        if (dimId == null || id == 0) {
            return null;
        }
        List<Label> labels;
        synchronized (LOCK) {
            labels = LABELS_BY_DIM.get(dimId);
        }
        if (labels == null) {
            return null;
        }
        for (Label l : labels) {
            if (l.id() == id) {
                return l;
            }
        }
        return null;
    }
}
