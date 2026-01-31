package top.xcyyds.starworld.forge.npc.skin;

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

    public ForgeNpcSkinSourceNameProvider() {
        this.configFile = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME).resolve(FILE_NAME);
        ensureConfigFileExists();
    }

    @Override
    public String pickSkinSourceName(RandomSource random, @Nullable MinecraftServer server) {
        List<String> list = getConfigNames();
        if (!list.isEmpty()) {
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
}
