package top.xcyyds.starworld.common.nation;

import net.minecraft.util.RandomSource;

import java.util.concurrent.atomic.AtomicReference;

public interface NationNameProvider {
    AtomicReference<NationNameProvider> INSTANCE = new AtomicReference<NationNameProvider>(new DefaultNationNameProvider());

    static NationNameProvider get() {
        return INSTANCE.get();
    }

    static void set(NationNameProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        INSTANCE.set(provider);
    }

    String generateZhName(RandomSource random);

    String generateEnName(RandomSource random);

    final class DefaultNationNameProvider implements NationNameProvider {
        private static final String[] PREFIX = new String[]{
                "华", "星", "云", "风", "山", "海", "河", "川", "青", "赤", "玄", "白", "金", "玉", "苍", "北", "南", "东", "西"
        };

        private static final String[] MID = new String[]{
                "夏", "原", "林", "岚", "阳", "月", "岳", "泽", "城", "宁", "安", "明", "武", "岚", "岭", "谷", "沙", "霜"
        };

        private static final String[] SUFFIX = new String[]{
                "国", "王国", "公国", "共和国", "联邦", "帝国"
        };

        @Override
        public String generateZhName(RandomSource random) {
            int p = random.nextInt(PREFIX.length);
            int m = random.nextInt(MID.length);
            int s = random.nextInt(SUFFIX.length);

            String base;
            if (random.nextFloat() < 0.55f) {
                base = PREFIX[p] + MID[m];
            } else {
                base = PREFIX[p];
            }

            return base + SUFFIX[s];
        }

        @Override
        public String generateEnName(RandomSource random) {
            return "";
        }
    }
}
