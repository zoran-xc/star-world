package top.xcyyds.starworld.forge.npc.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;
import top.xcyyds.starworld.common.npc.skin.NpcSkinSourceNameProvider;
import top.xcyyds.starworld.common.npc.skin.NpcSkinSourceUsedNamesSavedData;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ForgeNpcSkinSourceNameProvider implements NpcSkinSourceNameProvider {
    private static final String CONFIG_DIR_NAME = "starworld";
    private static final String FILE_NAME = "npc_skin_sources.txt";

    private static final String[] BANNED_DEFAULT_NAMES = new String[]{
            "Notch",
            "jeb_",
            "Dinnerbone",
            "Grumm",
            "Searge"
    };

    private final Path configFile;

    private volatile List<String> cachedNames = List.of();
    private volatile long cachedLastModified = -1L;

    private volatile Set<String> cachedBannedServerNamesLower = Set.of();
    private volatile long cachedBannedUserCacheModified = -1L;
    private volatile long cachedBannedWhitelistModified = -1L;

    public ForgeNpcSkinSourceNameProvider() {
        this.configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME).resolve(FILE_NAME);
        ensureConfigFileExists();
    }

    @Override
    public String pickSkinSourceName(RandomSource random, @Nullable MinecraftServer server) {
        if (random == null) {
            random = RandomSource.create();
        }

        List<String> list = getConfigNames();
        if (!list.isEmpty()) {
            Set<String> banned = getBannedServerNamesLower(server);
            if (banned.isEmpty()) {
                return pickUniqueFromConfig(random, server, list);
            }

            List<String> filtered = new ArrayList<>(list.size());
            for (String name : list) {
                if (name == null || name.isEmpty()) {
                    continue;
                }
                if (!banned.contains(name.toLowerCase())) {
                    filtered.add(name);
                }
            }
            if (!filtered.isEmpty()) {
                return pickUniqueFromConfig(random, server, filtered);
            }
        }

        return "";
    }

    private static String pickUniqueFromConfig(RandomSource random, @Nullable MinecraftServer server, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        if (server == null) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        ServerLevel level;
        try {
            level = server.overworld();
        } catch (Throwable t) {
            level = null;
        }
        if (level == null) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        NpcSkinSourceUsedNamesSavedData data = NpcSkinSourceUsedNamesSavedData.getOrCreate(level);

        List<String> pool = new ArrayList<>(candidates.size());
        for (String name : candidates) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            String lower = name.toLowerCase();
            if (!data.isUsed(lower)) {
                pool.add(name);
            }
        }
        if (pool.isEmpty()) {
            return "";
        }

        while (!pool.isEmpty()) {
            int idx = random.nextInt(pool.size());
            String picked = pool.remove(idx);
            String lower = picked.toLowerCase();
            if (data.markUsed(lower)) {
                return picked;
            }
        }

        return "";
    }

    private List<String> getConfigNames() {
        try {
            long modified = Files.exists(configFile) ? Files.getLastModifiedTime(configFile).toMillis() : -1L;
            if (modified == cachedLastModified && !cachedNames.isEmpty()) {
                return cachedNames;
            }
            List<String> loaded = readNamesFromFile(configFile);
            cachedNames = loaded;
            cachedLastModified = modified;
            return loaded;
        } catch (Throwable t) {
            return cachedNames;
        }
    }

    private static List<String> readNamesFromFile(Path file) {
        if (file == null || !Files.exists(file)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String raw : lines) {
                if (raw == null) {
                    continue;
                }
                String s = raw.trim();
                if (s.isEmpty()) {
                    continue;
                }
                if (isBannedDefaultName(s)) {
                    continue;
                }
                set.add(s);
            }
            return new ArrayList<>(set);
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static boolean isBannedDefaultName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (String banned : BANNED_DEFAULT_NAMES) {
            if (banned != null && banned.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void ensureConfigFileExists() {
        try {
            Path dir = configFile.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            if (Files.exists(configFile)) {
                return;
            }
            Files.createFile(configFile);
        } catch (Throwable t) {
        }
    }

    private Set<String> getBannedServerNamesLower(@Nullable MinecraftServer server) {
        if (server == null) {
            return Set.of();
        }

        Path usercacheFile = null;
        Path whitelistFile = null;

        try {
            usercacheFile = server.getFile("usercache.json").toPath();
        } catch (Throwable t) {
        }

        try {
            whitelistFile = server.getFile("whitelist.json").toPath();
        } catch (Throwable t) {
        }

        long usercacheModified = getLastModified(usercacheFile);
        long whitelistModified = getLastModified(whitelistFile);

        if (usercacheModified == cachedBannedUserCacheModified
                && whitelistModified == cachedBannedWhitelistModified) {
            return cachedBannedServerNamesLower;
        }

        Set<String> set = new HashSet<>();
        for (String name : readNamesFromJsonFile(usercacheFile)) {
            set.add(name.toLowerCase());
        }
        for (String name : readNamesFromJsonFile(whitelistFile)) {
            set.add(name.toLowerCase());
        }

        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String name = player.getGameProfile().getName();
                if (name != null && !name.isEmpty()) {
                    set.add(name.toLowerCase());
                }
            }
        } catch (Throwable t) {
        }

        cachedBannedServerNamesLower = Set.copyOf(set);
        cachedBannedUserCacheModified = usercacheModified;
        cachedBannedWhitelistModified = whitelistModified;
        return cachedBannedServerNamesLower;
    }

    private static long getLastModified(@Nullable Path file) {
        try {
            if (file == null || !Files.exists(file)) {
                return -1L;
            }
            return Files.getLastModifiedTime(file).toMillis();
        } catch (Throwable t) {
            return -1L;
        }
    }

    private static List<String> readNamesFromJsonFile(@Nullable Path file) {
        if (file == null || !Files.exists(file)) {
            return List.of();
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);
            if (root == null || !root.isJsonArray()) {
                return List.of();
            }
            JsonArray arr = root.getAsJsonArray();
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (JsonElement e : arr) {
                if (e == null || !e.isJsonObject()) {
                    continue;
                }
                JsonObject obj = e.getAsJsonObject();
                JsonElement nameEl = obj.get("name");
                if (nameEl != null && nameEl.isJsonPrimitive()) {
                    String name = nameEl.getAsString();
                    if (name != null) {
                        name = name.trim();
                        if (!name.isEmpty()) {
                            set.add(name);
                        }
                    }
                }
            }
            return new ArrayList<>(set);
        } catch (Throwable t) {
            return List.of();
        }
    }
}
