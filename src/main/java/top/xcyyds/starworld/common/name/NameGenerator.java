package top.xcyyds.starworld.common.name;

import net.minecraft.util.RandomSource;

public final class NameGenerator {
    private static final String[] NATION_ZH_PREFIX = new String[]{
            "华", "星", "云", "风", "山", "海", "河", "川", "青", "赤", "玄", "白", "金", "玉", "苍", "北", "南", "东", "西"
    };

    private static final String[] NATION_ZH_MID = new String[]{
            "夏", "原", "林", "岚", "阳", "月", "岳", "泽", "城", "宁", "安", "明", "武", "岭", "谷", "沙", "霜"
    };

    private static final String[] NATION_ZH_SUFFIX = new String[]{
            "国", "王国", "公国", "共和国", "联邦", "帝国"
    };

    private static final String[] NATION_EN_ADJ = new String[]{
            "Star", "Cloud", "Wind", "Mountain", "River", "Sea", "Golden", "Silver", "Azure", "Crimson", "Emerald", "Northern", "Southern", "Eastern", "Western"
    };

    private static final String[] NATION_EN_NOUN = new String[]{
            "Haven", "Vale", "Reach", "Crown", "Frontier", "Dominion", "Realm", "Union", "March", "Coast", "Isles", "Dawn", "Forge", "Sanctum"
    };

    private static final String[] NATION_EN_SUFFIX = new String[]{
            "Kingdom", "Empire", "Republic", "Federation", "Duchy", "Commonwealth"
    };

    private static final String[] NPC_ZH_SURNAME = new String[]{
            "赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "尤", "许",
            "何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏", "陶", "姜", "戚", "谢", "邹", "喻", "柏", "水", "窦", "章"
    };

    private static final String[] NPC_ZH_GIVEN = new String[]{
            "子轩", "若曦", "雨辰", "沐阳", "清歌", "暮雪", "明远", "星河", "千墨", "归舟", "云深", "长风", "青岚", "洛尘", "朝歌", "北辰"
    };

    private static final String[] NPC_EN_FIRST = new String[]{
            "Alex", "Steve", "Noah", "Liam", "Ethan", "Mason", "Lucas", "Aiden", "Logan", "James", "Emma", "Olivia", "Ava", "Mia", "Sophia", "Amelia"
    };

    private static final String[] NPC_EN_LAST = new String[]{
            "Stone", "Rivers", "Woods", "Baker", "Carter", "Walker", "Turner", "Miller", "Cooper", "Fisher", "Reed", "Knight", "Hunter", "Frost", "Hart"
    };

    private NameGenerator() {
    }

    public static BilingualName generateNationName(RandomSource random) {
        int p = random.nextInt(NATION_ZH_PREFIX.length);
        int m = random.nextInt(NATION_ZH_MID.length);
        int s = random.nextInt(NATION_ZH_SUFFIX.length);

        String zhBase;
        if (random.nextFloat() < 0.55f) {
            zhBase = NATION_ZH_PREFIX[p] + NATION_ZH_MID[m];
        } else {
            zhBase = NATION_ZH_PREFIX[p];
        }
        String zh = zhBase + NATION_ZH_SUFFIX[s];

        String enCore;
        if (random.nextFloat() < 0.60f) {
            enCore = NATION_EN_ADJ[random.nextInt(NATION_EN_ADJ.length)] + " " + NATION_EN_NOUN[random.nextInt(NATION_EN_NOUN.length)];
        } else {
            enCore = NATION_EN_NOUN[random.nextInt(NATION_EN_NOUN.length)];
        }
        String en = enCore + " " + NATION_EN_SUFFIX[random.nextInt(NATION_EN_SUFFIX.length)];

        return new BilingualName(zh, en);
    }

    public static BilingualName generateNpcName(RandomSource random) {
        String zh = NPC_ZH_SURNAME[random.nextInt(NPC_ZH_SURNAME.length)] + NPC_ZH_GIVEN[random.nextInt(NPC_ZH_GIVEN.length)];

        String first = NPC_EN_FIRST[random.nextInt(NPC_EN_FIRST.length)];
        String last = NPC_EN_LAST[random.nextInt(NPC_EN_LAST.length)];
        String en = first + " " + last;

        return new BilingualName(zh, en);
    }
}
