package top.xcyyds.starworld.common.nation;

import net.minecraft.nbt.CompoundTag;

public record Nation(
        int id,
        int capitalX,
        int capitalZ,
        int colorRgb,
        String zhName,
        String enName,
        double size
) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.putInt("capitalX", capitalX);
        tag.putInt("capitalZ", capitalZ);
        tag.putInt("colorRgb", colorRgb);
        tag.putString("zhName", zhName == null ? "" : zhName);
        tag.putString("enName", enName == null ? "" : enName);
        tag.putDouble("size", size);
        return tag;
    }

    public static Nation fromTag(CompoundTag tag) {
        return new Nation(
                tag.getInt("id"),
                tag.getInt("capitalX"),
                tag.getInt("capitalZ"),
                tag.getInt("colorRgb"),
                tag.getString("zhName"),
                tag.getString("enName"),
                tag.getDouble("size")
        );
    }
}
