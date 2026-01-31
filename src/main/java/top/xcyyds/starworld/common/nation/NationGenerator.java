package top.xcyyds.starworld.common.nation;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import top.xcyyds.starworld.common.name.BilingualName;
import top.xcyyds.starworld.common.name.BilingualNameProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class NationGenerator {
    private NationGenerator() {
    }

    public static List<Nation> generate(long worldSeed) {
        RandomSource random = RandomSource.create(mixSeed(worldSeed, 0x6E6174696F6E5F4CL));

        int neighborCount = 2 + random.nextInt(3);
        int extraCount = random.nextInt(3);
        int total = 1 + neighborCount + extraCount;

        List<Nation> nations = new ArrayList<>(total);
        HashSet<Long> usedCapitals = new HashSet<>();

        int id = 0;

        Nation spawnNation = createSpawnNation(random, worldSeed, id++, usedCapitals);
        nations.add(spawnNation);

        for (int i = 0; i < neighborCount; i++) {
            nations.add(createNeighborNation(random, worldSeed, id++, i, neighborCount, usedCapitals));
        }

        for (int i = 0; i < extraCount; i++) {
            nations.add(createFarNation(random, worldSeed, id++, usedCapitals));
        }

        return nations;
    }

    private static Nation createSpawnNation(RandomSource random, long worldSeed, int id, HashSet<Long> usedCapitals) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        int r = 1200 + random.nextInt(1301);

        int x = (int) Math.round(Math.cos(angle) * r);
        int z = (int) Math.round(Math.sin(angle) * r);
        long packed = pack(x, z);
        usedCapitals.add(packed);

        double size = 12000.0 + random.nextDouble() * 18000.0;
        int color = randomColor(random);
        BilingualName name = BilingualNameProvider.get().generateNationName(random);

        return new Nation(id, x, z, color, name.zh(), name.en(), size);
    }

    private static Nation createNeighborNation(RandomSource random, long worldSeed, int id, int index, int neighborCount, HashSet<Long> usedCapitals) {
        double baseAngle = (Mth.TWO_PI / (double) neighborCount) * (double) index;
        double angleJitter = (random.nextDouble() - 0.5) * 0.7;
        double angle = baseAngle + angleJitter;

        int r = 3500 + random.nextInt(3501);
        int x = (int) Math.round(Math.cos(angle) * r);
        int z = (int) Math.round(Math.sin(angle) * r);

        for (int attempt = 0; attempt < 6; attempt++) {
            long packed = pack(x, z);
            if (!usedCapitals.contains(packed)) {
                usedCapitals.add(packed);
                break;
            }
            angle += 0.4;
            x = (int) Math.round(Math.cos(angle) * r);
            z = (int) Math.round(Math.sin(angle) * r);
        }

        double size = 7000.0 + random.nextDouble() * 12000.0;
        int color = randomColor(random);
        BilingualName name = BilingualNameProvider.get().generateNationName(random);

        return new Nation(id, x, z, color, name.zh(), name.en(), size);
    }

    private static Nation createFarNation(RandomSource random, long worldSeed, int id, HashSet<Long> usedCapitals) {
        double angle = random.nextDouble() * Mth.TWO_PI;

        double t = Math.pow(random.nextDouble(), 0.35);
        int r = (int) Math.round(12000.0 + t * 48000.0);
        int x = (int) Math.round(Math.cos(angle) * r);
        int z = (int) Math.round(Math.sin(angle) * r);

        for (int attempt = 0; attempt < 10; attempt++) {
            long packed = pack(x, z);
            if (!usedCapitals.contains(packed)) {
                usedCapitals.add(packed);
                break;
            }
            angle += 0.5;
            x = (int) Math.round(Math.cos(angle) * r);
            z = (int) Math.round(Math.sin(angle) * r);
        }

        double size = 18000.0 + random.nextDouble() * 45000.0;
        int color = randomColor(random);
        BilingualName name = BilingualNameProvider.get().generateNationName(random);

        return new Nation(id, x, z, color, name.zh(), name.en(), size);
    }

    private static int randomColor(RandomSource random) {
        float h = random.nextFloat();
        float s = 0.55f + random.nextFloat() * 0.35f;
        float v = 0.75f + random.nextFloat() * 0.20f;
        return Mth.hsvToRgb(h, s, v);
    }

    private static long mixSeed(long seed, long salt) {
        long z = seed ^ salt;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
    }
}
