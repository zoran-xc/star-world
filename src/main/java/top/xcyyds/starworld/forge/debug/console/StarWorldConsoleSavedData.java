package top.xcyyds.starworld.forge.debug.console;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StarWorldConsoleSavedData extends SavedData {
    private static final String DATA_NAME = "starworld_console";
    private static final int MAX_ENTRIES = 2000;

    private final List<StarWorldConsoleLogEntry> entries;

    private StarWorldConsoleSavedData(List<StarWorldConsoleLogEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public List<StarWorldConsoleLogEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public static StarWorldConsoleSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                StarWorldConsoleSavedData::load,
                () -> new StarWorldConsoleSavedData(List.of()),
                DATA_NAME
        );
    }

    public void append(long gameTime, String source, String message) {
        if (source == null) {
            source = "";
        }
        if (message == null) {
            message = "";
        }
        entries.add(new StarWorldConsoleLogEntry(gameTime, source, message));
        if (entries.size() > MAX_ENTRIES) {
            int remove = entries.size() - MAX_ENTRIES;
            entries.subList(0, remove).clear();
        }
        setDirty();
    }

    private static StarWorldConsoleSavedData load(CompoundTag tag) {
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        List<StarWorldConsoleLogEntry> entries = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag eTag = list.getCompound(i);
            long gameTime = eTag.getLong("gameTime");
            String source = eTag.getString("source");
            String message = eTag.getString("message");
            entries.add(new StarWorldConsoleLogEntry(gameTime, source, message));
        }
        return new StarWorldConsoleSavedData(entries);
    }

    @Override
    public @Nonnull CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        for (StarWorldConsoleLogEntry entry : entries) {
            CompoundTag eTag = new CompoundTag();
            eTag.putLong("gameTime", entry.gameTime());
            eTag.putString("source", entry.source());
            eTag.putString("message", entry.message());
            list.add(eTag);
        }
        tag.put("entries", list);
        return tag;
    }
}
