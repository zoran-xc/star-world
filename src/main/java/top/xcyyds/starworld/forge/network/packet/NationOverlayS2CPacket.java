package top.xcyyds.starworld.forge.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import top.xcyyds.starworld.forge.nation.client.StarWorldClientNationOverlayState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class NationOverlayS2CPacket {
    private final String dimId;
    private final int cMinX;
    private final int cMaxX;
    private final int cMinZ;
    private final int cMaxZ;
    private final int[] nationIds;
    private final List<Segment> segments;
    private final List<Label> labels;

    public NationOverlayS2CPacket(String dimId, int cMinX, int cMaxX, int cMinZ, int cMaxZ, int[] nationIds, List<Segment> segments, List<Label> labels) {
        this.dimId = dimId;
        this.cMinX = cMinX;
        this.cMaxX = cMaxX;
        this.cMinZ = cMinZ;
        this.cMaxZ = cMaxZ;
        this.nationIds = nationIds;
        this.segments = segments;
        this.labels = labels;
    }

    public static void encode(NationOverlayS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dimId);
        buf.writeVarInt(msg.cMinX);
        buf.writeVarInt(msg.cMaxX);
        buf.writeVarInt(msg.cMinZ);
        buf.writeVarInt(msg.cMaxZ);

        buf.writeVarInt(msg.nationIds.length);
        for (int id : msg.nationIds) {
            buf.writeVarInt(id);
        }

        buf.writeVarInt(msg.segments.size());
        for (Segment s : msg.segments) {
            buf.writeVarInt(s.x0);
            buf.writeVarInt(s.z0);
            buf.writeVarInt(s.x1);
            buf.writeVarInt(s.z1);
        }

        buf.writeVarInt(msg.labels.size());
        for (Label l : msg.labels) {
            buf.writeVarInt(l.id);
            buf.writeVarInt(l.colorRgb);
            buf.writeUtf(l.zhName == null ? "" : l.zhName);
            buf.writeUtf(l.enName == null ? "" : l.enName);
            buf.writeVarInt(l.capitalX);
            buf.writeVarInt(l.capitalZ);
        }
    }

    public static NationOverlayS2CPacket decode(FriendlyByteBuf buf) {
        String dimId = buf.readUtf();
        int cMinX = buf.readVarInt();
        int cMaxX = buf.readVarInt();
        int cMinZ = buf.readVarInt();
        int cMaxZ = buf.readVarInt();

        int gridLen = buf.readVarInt();
        int[] nationIds = new int[gridLen];
        for (int i = 0; i < gridLen; i++) {
            nationIds[i] = buf.readVarInt();
        }

        int segCount = buf.readVarInt();
        List<Segment> segments = new ArrayList<>(segCount);
        for (int i = 0; i < segCount; i++) {
            int x0 = buf.readVarInt();
            int z0 = buf.readVarInt();
            int x1 = buf.readVarInt();
            int z1 = buf.readVarInt();
            segments.add(new Segment(x0, z0, x1, z1));
        }

        int labelCount = buf.readVarInt();
        List<Label> labels = new ArrayList<>(labelCount);
        for (int i = 0; i < labelCount; i++) {
            int id = buf.readVarInt();
            int colorRgb = buf.readVarInt();
            String zhName = buf.readUtf();
            String enName = buf.readUtf();
            int capitalX = buf.readVarInt();
            int capitalZ = buf.readVarInt();
            labels.add(new Label(id, colorRgb, zhName, enName, capitalX, capitalZ));
        }

        return new NationOverlayS2CPacket(dimId, cMinX, cMaxX, cMinZ, cMaxZ, nationIds, segments, labels);
    }

    public static void handle(NationOverlayS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<StarWorldClientNationOverlayState.Segment> segs = new ArrayList<>(msg.segments.size());
            for (Segment s : msg.segments) {
                segs.add(new StarWorldClientNationOverlayState.Segment(s.x0, s.z0, s.x1, s.z1));
            }
            List<StarWorldClientNationOverlayState.Label> labs = new ArrayList<>(msg.labels.size());
            for (Label l : msg.labels) {
                labs.add(new StarWorldClientNationOverlayState.Label(l.id, l.colorRgb, l.zhName, l.enName, l.capitalX, l.capitalZ));
            }

            StarWorldClientNationOverlayState.applyTile(
                    msg.dimId,
                    msg.cMinX,
                    msg.cMaxX,
                    msg.cMinZ,
                    msg.cMaxZ,
                    msg.nationIds,
                    segs,
                    labs
            );
        });
        ctx.get().setPacketHandled(true);
    }

    public static final class Segment {
        public final int x0;
        public final int z0;
        public final int x1;
        public final int z1;

        public Segment(int x0, int z0, int x1, int z1) {
            this.x0 = x0;
            this.z0 = z0;
            this.x1 = x1;
            this.z1 = z1;
        }
    }

    public static final class Label {
        public final int id;
        public final int colorRgb;
        public final String zhName;
        public final String enName;
        public final int capitalX;
        public final int capitalZ;

        public Label(int id, int colorRgb, String zhName, String enName, int capitalX, int capitalZ) {
            this.id = id;
            this.colorRgb = colorRgb;
            this.zhName = zhName;
            this.enName = enName;
            this.capitalX = capitalX;
            this.capitalZ = capitalZ;
        }
    }
}
