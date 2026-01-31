package top.xcyyds.starworld.forge.npc.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;
import top.xcyyds.starworld.common.npc.skin.NpcSkinSourceNameProvider;
import top.xcyyds.starworld.common.npc.skin.OfficialSkinUtils;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ForgeNpcSkinSourceNameProvider implements NpcSkinSourceNameProvider {
    private static final String CONFIG_DIR_NAME = "starworld";
    private static final String FILE_NAME = "npc_skin_sources.txt";

    private final Path configFile;

    private volatile List<String> cachedNames = List.of();
    private volatile long cachedLastModified = -1L;

    private volatile List<String> cachedServerNames = List.of();
    private volatile long cachedServerUserCacheModified = -1L;
    private volatile long cachedServerWhitelistModified = -1L;

    public ForgeNpcSkinSourceNameProvider() {
        this.configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME).resolve(FILE_NAME);
        ensureConfigFileExists();
    }

    @Override
    public String pickSkinSourceName(RandomSource random, @Nullable MinecraftServer server) {
        if (random == null) {
            random = RandomSource.create();
        }

        LinkedHashSet<String> combined = new LinkedHashSet<>();
        combined.addAll(getConfigNames());
        combined.addAll(getServerNames(server));

        if (!combined.isEmpty()) {
            List<String> list = new ArrayList<>(combined);
            return list.get(random.nextInt(list.size()));
        }

        return OfficialSkinUtils.randomNpcSkinSourceName(random);
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
                set.add(s);
            }
            return new ArrayList<>(set);
        } catch (Throwable t) {
            return List.of();
        }
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
            List<String> defaults = List.of(
                    "Notch",
                    "jeb_",
                    "Dinnerbone",
                    "Grumm",
                    "Searge"
            );
            Files.write(configFile, defaults, StandardCharsets.UTF_8);
        } catch (Throwable t) {
        }
    }

    private List<String> getServerNames(@Nullable MinecraftServer server) {
        if (server == null) {
            return List.of();
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

        if (usercacheModified == cachedServerUserCacheModified
                && whitelistModified == cachedServerWhitelistModified
                && !cachedServerNames.isEmpty()) {
            return cachedServerNames;
        }

        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.addAll(readNamesFromUserCache(usercacheFile));
        set.addAll(readNamesFromWhitelist(whitelistFile));

        List<String> result = new ArrayList<>(set);
        cachedServerNames = result;
        cachedServerUserCacheModified = usercacheModified;
        cachedServerWhitelistModified = whitelistModified;
        return result;
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

    private static List<String> readNamesFromUserCache(@Nullable Path file) {
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

    private static List<String> readNamesFromWhitelist(@Nullable Path file) {
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
