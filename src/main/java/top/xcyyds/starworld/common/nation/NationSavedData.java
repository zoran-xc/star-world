package top.xcyyds.starworld.common.nation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NationSavedData extends SavedData {
    private static final String DATA_NAME = "starworld_nations";

    private final List<Nation> nations;

    private NationSavedData(List<Nation> nations) {
        this.nations = new ArrayList<>(nations);
    }

    public List<Nation> nations() {
        return Collections.unmodifiableList(nations);
    }

    public static NationSavedData getOrCreate(ServerLevel level) {
        NationSavedData data = level.getDataStorage().computeIfAbsent(
                NationSavedData::load,
                () -> new NationSavedData(NationGenerator.generate(level.getSeed())),
                DATA_NAME
        );
        if (data.nations.isEmpty()) {
            return regenerate(level);
        }
        return data;
    }

    public static NationSavedData regenerate(ServerLevel level) {
        NationSavedData data = new NationSavedData(NationGenerator.generate(level.getSeed()));
        data.setDirty();
        level.getDataStorage().set(DATA_NAME, data);
        return data;
    }

    private static NationSavedData load(CompoundTag tag) {
        ListTag list = tag.getList("nations", Tag.TAG_COMPOUND);
        List<Nation> nations = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nTag = list.getCompound(i);
            nations.add(Nation.fromTag(nTag));
        }
        return new NationSavedData(nations);
    }

    @Override
    public @Nonnull CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Nation nation : nations) {
            list.add(nation.toTag());
        }
        tag.put("nations", list);
        return tag;
    }
}
