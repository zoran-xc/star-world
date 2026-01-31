package top.xcyyds.starworld.common.nation;

import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class NationQuery {
    private static final long WARP_SALT_X = 0x776172705F786C31L;
    private static final long WARP_SALT_Z = 0x776172705F7A6C32L;
    private static final long BUFFER_SALT = 0x6275666665725F31L;

    private NationQuery() {
    }

    public static NationQueryResult query(ServerLevel level, int x, int z) {
        NationSavedData data = NationSavedData.getOrCreate(level);
        return query(level.getSeed(), data.nations(), x, z);
    }

    public static NationQueryResult query(long worldSeed, List<Nation> nations, int x, int z) {
        if (nations == null || nations.isEmpty()) {
            return new NationQueryResult(new Nation(0, 0, 0, 0xFFFFFF, "", "", 1.0), NationBorderBand.INSIDE, Double.POSITIVE_INFINITY, 0, 0);
        }

        double warpStrength = 700.0;
        double warpScale = 6000.0;
        double detailScale = 900.0;

        double warpX = (noiseSigned(mixSeed(worldSeed, WARP_SALT_X), x, z, warpScale)) * warpStrength;
        double warpZ = (noiseSigned(mixSeed(worldSeed, WARP_SALT_Z), x, z, warpScale)) * warpStrength;
        double wx = x + warpX;
        double wz = z + warpZ;

        Nation bestNation = null;
        double best = Double.POSITIVE_INFINITY;
        double second = Double.POSITIVE_INFINITY;

        for (Nation nation : nations) {
            double size = Math.max(1.0, nation.size());
            double dx = wx - nation.capitalX();
            double dz = wz - nation.capitalZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            double score = dist / size;

            long nationSalt = 0x6E6174696F6E5F69L ^ ((long) nation.id() * 0x9E3779B97F4A7C15L);
            score += noiseSigned(mixSeed(worldSeed, nationSalt), x, z, detailScale) * 0.03;

            if (score < best) {
                second = best;
                best = score;
                bestNation = nation;
            } else if (score < second) {
                second = score;
            }
        }

        if (bestNation == null) {
            bestNation = nations.get(0);
            second = best;
        }

        double margin = Math.max(0.0, second - best);
        double borderDistanceBlocks = (margin * Math.max(1.0, bestNation.size())) / 2.0;

        int bufferTotal = bufferTotalBlocks(worldSeed, x, z);
        int innerStart = Math.max(1, (int) Math.round(bufferTotal * 0.40));

        NationBorderBand band;
        if (borderDistanceBlocks < innerStart) {
            band = NationBorderBand.INNER_BUFFER;
        } else if (borderDistanceBlocks < bufferTotal) {
            band = NationBorderBand.OUTER_BUFFER;
        } else {
            band = NationBorderBand.INSIDE;
        }

        return new NationQueryResult(bestNation, band, borderDistanceBlocks, innerStart, bufferTotal);
    }

    private static int bufferTotalBlocks(long worldSeed, int x, int z) {
        double n = noise01(mixSeed(worldSeed, BUFFER_SALT), x, z, 9000.0);
        int w = 5 + (int) Math.floor(45.0 * clamp01(n));
        return Math.max(5, Math.min(50, w));
    }

    private static double noiseSigned(long seed, double x, double z, double scale) {
        return noise01(seed, x, z, scale) * 2.0 - 1.0;
    }

    private static double noise01(long seed, double x, double z, double scale) {
        double sx = x / scale;
        double sz = z / scale;

        int x0 = fastFloor(sx);
        int z0 = fastFloor(sz);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = sx - x0;
        double tz = sz - z0;

        double v00 = hash01(seed, x0, z0);
        double v10 = hash01(seed, x1, z0);
        double v01 = hash01(seed, x0, z1);
        double v11 = hash01(seed, x1, z1);

        double u = smoothstep(tx);
        double v = smoothstep(tz);

        double a = lerp(v00, v10, u);
        double b = lerp(v01, v11, u);
        return lerp(a, b, v);
    }

    private static double hash01(long seed, int x, int z) {
        long h = seed;
        h ^= (long) x * 0x517CC1B727220A95L;
        h ^= (long) z * 0x9E3779B97F4A7C15L;
        h = mixSeed(h, 0xD1B54A32D192ED03L);
        return ((h >>> 11) & ((1L << 53) - 1)) * (1.0 / (double) (1L << 53));
    }

    private static int fastFloor(double x) {
        int i = (int) x;
        return x < (double) i ? i - 1 : i;
    }

    private static double smoothstep(double t) {
        t = clamp01(t);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static long mixSeed(long seed, long salt) {
        long z = seed ^ salt;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
