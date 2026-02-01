package top.xcyyds.starworld.forge.nation.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StarWorldClientNationOverlayState {
    private static volatile Snapshot snapshot;

    private StarWorldClientNationOverlayState() {
    }

    public static Snapshot getSnapshot() {
        return snapshot;
    }

    public static void setSnapshot(Snapshot s) {
        snapshot = s;
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

    public record Snapshot(String dimId, int cMinX, int cMaxX, int cMinZ, int cMaxZ, List<Segment> segments, List<Label> labels) {
        public Snapshot {
            segments = Collections.unmodifiableList(new ArrayList<>(segments));
            labels = Collections.unmodifiableList(new ArrayList<>(labels));
        }

        public boolean covers(String dimId, int cMinX, int cMaxX, int cMinZ, int cMaxZ) {
            if (this.dimId == null || dimId == null) {
                return false;
            }
            if (!this.dimId.equals(dimId)) {
                return false;
            }
            return cMinX >= this.cMinX && cMaxX <= this.cMaxX && cMinZ >= this.cMinZ && cMaxZ <= this.cMaxZ;
        }
    }
}
