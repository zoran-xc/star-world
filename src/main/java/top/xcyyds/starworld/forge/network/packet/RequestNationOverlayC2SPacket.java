package top.xcyyds.starworld.forge.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.xcyyds.starworld.common.nation.Nation;
import top.xcyyds.starworld.common.nation.NationQuery;
import top.xcyyds.starworld.common.nation.NationQueryResult;
import top.xcyyds.starworld.common.nation.NationSavedData;
import top.xcyyds.starworld.forge.network.StarWorldNetwork;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public final class RequestNationOverlayC2SPacket {
    private static final int MAX_CHUNKS_AREA = 16384;
    private static final int CACHE_MAX_CHUNKS = 200_000;

    private final String dimId;
    private final int cMinX;
    private final int cMaxX;
    private final int cMinZ;
    private final int cMaxZ;

    public RequestNationOverlayC2SPacket(String dimId, int cMinX, int cMaxX, int cMinZ, int cMaxZ) {
        this.dimId = dimId;
        this.cMinX = cMinX;
        this.cMaxX = cMaxX;
        this.cMinZ = cMinZ;
        this.cMaxZ = cMaxZ;
    }

    public static void encode(RequestNationOverlayC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dimId);
        buf.writeVarInt(msg.cMinX);
        buf.writeVarInt(msg.cMaxX);
        buf.writeVarInt(msg.cMinZ);
        buf.writeVarInt(msg.cMaxZ);
    }

    public static RequestNationOverlayC2SPacket decode(FriendlyByteBuf buf) {
        String dimId = buf.readUtf();
        int cMinX = buf.readVarInt();
        int cMaxX = buf.readVarInt();
        int cMinZ = buf.readVarInt();
        int cMaxZ = buf.readVarInt();
        return new RequestNationOverlayC2SPacket(dimId, cMinX, cMaxX, cMinZ, cMaxZ);
    }

    public static void handle(RequestNationOverlayC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            ResourceLocation dim = level.dimension().location();
            if (dim == null || !dim.toString().equals(msg.dimId)) {
                return;
            }

            int cMinX = Math.min(msg.cMinX, msg.cMaxX);
            int cMaxX = Math.max(msg.cMinX, msg.cMaxX);
            int cMinZ = Math.min(msg.cMinZ, msg.cMaxZ);
            int cMaxZ = Math.max(msg.cMinZ, msg.cMaxZ);

            long w = (long) cMaxX - (long) cMinX + 1L;
            long h = (long) cMaxZ - (long) cMinZ + 1L;
            if (w <= 0 || h <= 0 || w * h > MAX_CHUNKS_AREA) {
                return;
            }

            NationSavedData data = NationSavedData.getOrCreate(level);
            List<Nation> nations = data.nations();

            int[][] grid = new int[(int) h][(int) w];
            for (int dz = 0; dz < h; dz++) {
                int cz = cMinZ + dz;
                for (int dx = 0; dx < w; dx++) {
                    int cx = cMinX + dx;
                    int id = NationOverlayCache.getNationId(level, nations, cx, cz);
                    grid[dz][dx] = id;
                }
            }

            List<NationOverlayS2CPacket.Segment> segments = new ArrayList<>();
            for (int dz = 0; dz < h; dz++) {
                int cz = cMinZ + dz;
                for (int dx = 0; dx < w; dx++) {
                    int cx = cMinX + dx;
                    int id = grid[dz][dx];

                    if (dx + 1 < w) {
                        int right = grid[dz][dx + 1];
                        if (right != id) {
                            int x = (cx + 1) << 4;
                            int z0 = cz << 4;
                            int z1 = (cz + 1) << 4;
                            segments.add(new NationOverlayS2CPacket.Segment(x, z0, x, z1));
                        }
                    }
                    if (dz + 1 < h) {
                        int down = grid[dz + 1][dx];
                        if (down != id) {
                            int z = (cz + 1) << 4;
                            int x0 = cx << 4;
                            int x1 = (cx + 1) << 4;
                            segments.add(new NationOverlayS2CPacket.Segment(x0, z, x1, z));
                        }
                    }
                }
            }

            List<NationOverlayS2CPacket.Label> labels = new ArrayList<>(nations.size());
            for (Nation n : nations) {
                labels.add(new NationOverlayS2CPacket.Label(n.id(), n.colorRgb(), n.zhName(), n.enName(), n.capitalX(), n.capitalZ()));
            }

            NationOverlayS2CPacket s2c = new NationOverlayS2CPacket(msg.dimId, cMinX, cMaxX, cMinZ, cMaxZ, segments, labels);
            StarWorldNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), s2c);
        });
        ctx.get().setPacketHandled(true);
    }

    private static final class NationOverlayCache {
        private static final Object LOCK = new Object();
        private static final Map<String, Map<Long, Integer>> CACHE = new LinkedHashMap<>();

        private NationOverlayCache() {
        }

        private static int getNationId(ServerLevel level, List<Nation> nations, int chunkX, int chunkZ) {
            String dimId = level.dimension().location().toString();
            long key = pack(chunkX, chunkZ);

            synchronized (LOCK) {
                Map<Long, Integer> perDim = CACHE.computeIfAbsent(dimId, d -> new LinkedHashMap<>(8192, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Entry<Long, Integer> eldest) {
                        return size() > CACHE_MAX_CHUNKS;
                    }
                });

                Integer cached = perDim.get(key);
                if (cached != null) {
                    return cached;
                }

                int wx = (chunkX << 4) + 8;
                int wz = (chunkZ << 4) + 8;
                NationQueryResult result = NationQuery.query(level.getSeed(), nations, wx, wz);
                int id = result.nation() == null ? 0 : result.nation().id();
                perDim.put(key, id);
                return id;
            }
        }

        private static long pack(int x, int z) {
            return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        }
    }
}
