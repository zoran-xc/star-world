package top.xcyyds.starworld.common.nation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;

import java.util.List;

public final class NationQuery {
    private static final long WARP_SALT_X = 0x776172705F786C31L;
    private static final long WARP_SALT_Z = 0x776172705F7A6C32L;
    private static final long BUFFER_SALT = 0x6275666665725F31L;
    private static final long WILD_SALT = 0x77696C646E657373L;

    private NationQuery() {
    }

    public static NationQueryResult query(ServerLevel level, int x, int z) {
        NationSavedData data = NationSavedData.getOrCreate(level);
        NationQueryResult result = query(level.getSeed(), data.nations(), x, z);

        if (result.borderBand() == NationBorderBand.OUTSIDE) {
            return result;
        }

        if (isOcean(level, x, z)) {
            int coastDist = approxDistanceToCoast(level, x, z, 512, 32);
            if (coastDist > 500) {
                return new NationQueryResult(result.nation(), NationBorderBand.OUTSIDE, result.borderDistanceBlocks(), result.bufferInnerStartBlocks(), result.bufferTotalBlocks());
            }
        }

        return result;
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

        double maxSize = 1.0;
        for (Nation nation : nations) {
            maxSize = Math.max(maxSize, nation.size());
        }

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

        boolean spawnProtected = (x * (long) x + z * (long) z) <= (2000L * 2000L);
        if (!spawnProtected) {
            boolean superPower = bestNation.size() >= maxSize * 0.999;

            double baseReachFactor = superPower ? 2.2 : 0.95;
            double wildness = noise01(mixSeed(worldSeed, WILD_SALT), x, z, 12000.0);

            double reachFactor = baseReachFactor - wildness * 0.35;
            if (best > reachFactor) {
                int bufferTotal = bufferTotalBlocks(worldSeed, x, z);
                int innerStart = Math.max(1, (int) Math.round(bufferTotal * 0.40));
                return new NationQueryResult(bestNation, NationBorderBand.OUTSIDE, borderDistanceBlocks, innerStart, bufferTotal);
            }

            double wildernessChance = superPower ? 0.20 : 0.70;
            double wRoll = noise01(mixSeed(worldSeed, WILD_SALT ^ 0x9E3779B97F4A7C15L), x, z, 5500.0);
            if (wRoll < wildernessChance) {
                int bufferTotal = bufferTotalBlocks(worldSeed, x, z);
                int innerStart = Math.max(1, (int) Math.round(bufferTotal * 0.40));
                return new NationQueryResult(bestNation, NationBorderBand.OUTSIDE, borderDistanceBlocks, innerStart, bufferTotal);
            }
        }

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

    @SuppressWarnings("null")
    private static boolean isOcean(ServerLevel level, int x, int z) {
        int seaLevel = level.getSeaLevel();
        return level.getBiome(new BlockPos(x, seaLevel, z)).is(BiomeTags.IS_OCEAN);
    }

    @SuppressWarnings("null")
    private static int approxDistanceToCoast(ServerLevel level, int x, int z, int maxRadius, int step) {
        int seaLevel = level.getSeaLevel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int best = Integer.MAX_VALUE;
        for (int r = 0; r <= maxRadius; r += step) {
            if (best <= 500) {
                return best;
            }
            if (r == 0) {
                continue;
            }

            for (int dx = -r; dx <= r; dx += step) {
                int dz1 = -r;
                int dz2 = r;

                pos.set(x + dx, seaLevel, z + dz1);
                if (!level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
                    best = Math.min(best, (int) Math.round(Math.sqrt(dx * (long) dx + dz1 * (long) dz1)));
                }

                pos.set(x + dx, seaLevel, z + dz2);
                if (!level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
                    best = Math.min(best, (int) Math.round(Math.sqrt(dx * (long) dx + dz2 * (long) dz2)));
                }
            }

            for (int dz = -r; dz <= r; dz += step) {
                int dx1 = -r;
                int dx2 = r;

                pos.set(x + dx1, seaLevel, z + dz);
                if (!level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
                    best = Math.min(best, (int) Math.round(Math.sqrt(dx1 * (long) dx1 + dz * (long) dz)));
                }

                pos.set(x + dx2, seaLevel, z + dz);
                if (!level.getBiome(pos).is(BiomeTags.IS_OCEAN)) {
                    best = Math.min(best, (int) Math.round(Math.sqrt(dx2 * (long) dx2 + dz * (long) dz)));
                }
            }
        }

        return best;
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
