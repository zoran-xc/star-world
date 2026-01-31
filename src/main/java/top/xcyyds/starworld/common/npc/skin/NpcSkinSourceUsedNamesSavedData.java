package top.xcyyds.starworld.common.npc.skin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class NpcSkinSourceUsedNamesSavedData extends SavedData {
    private static final String DATA_NAME = "starworld_npc_skin_source_used_names";

    private final Set<String> usedLower;

    private NpcSkinSourceUsedNamesSavedData(Set<String> usedLower) {
        this.usedLower = new HashSet<>(usedLower);
    }

    public static NpcSkinSourceUsedNamesSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                NpcSkinSourceUsedNamesSavedData::load,
                () -> new NpcSkinSourceUsedNamesSavedData(Set.of()),
                DATA_NAME
        );
    }

    public Set<String> usedLower() {
        return Collections.unmodifiableSet(usedLower);
    }

    public boolean isUsed(String nameLower) {
        return usedLower.contains(nameLower);
    }

    public boolean markUsed(String nameLower) {
        if (nameLower == null || nameLower.isEmpty()) {
            return false;
        }
        boolean added = usedLower.add(nameLower);
        if (added) {
            setDirty();
        }
        return added;
    }

    private static NpcSkinSourceUsedNamesSavedData load(CompoundTag tag) {
        Set<String> set = new HashSet<>();
        if (tag.contains("used", Tag.TAG_LIST)) {
            ListTag list = tag.getList("used", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                s = s.trim();
                if (!s.isEmpty()) {
                    set.add(s.toLowerCase());
                }
            }
        }
        return new NpcSkinSourceUsedNamesSavedData(set);
    }

    @Override
    public @Nonnull CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        for (String s : usedLower) {
            list.add(StringTag.valueOf(s));
        }
        tag.put("used", list);
        return tag;
    }
}
