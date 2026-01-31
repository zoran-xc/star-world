package top.xcyyds.starworld.common.name;

import net.minecraft.nbt.CompoundTag;

public record BilingualName(String zh, String en) {
    public BilingualName {
        if (zh == null) {
            zh = "";
        }
        if (en == null) {
            en = "";
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("zh", zh);
        tag.putString("en", en);
        return tag;
    }

    public static BilingualName fromTag(CompoundTag tag) {
        if (tag == null) {
            return new BilingualName("", "");
        }
        return new BilingualName(tag.getString("zh"), tag.getString("en"));
    }
}
