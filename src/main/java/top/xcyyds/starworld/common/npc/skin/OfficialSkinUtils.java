package top.xcyyds.starworld.common.npc.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import java.util.Optional;
import java.util.UUID;

public final class OfficialSkinUtils {
    private static final String[] DEFAULT_NPC_SKIN_SOURCES = new String[]{
            "Notch",
            "jeb_",
            "Dinnerbone",
            "Grumm",
            "Searge"
    };

    private OfficialSkinUtils() {
    }

    public static Optional<Property> fetchTexturesByPlayerName(MinecraftServer server, String playerName) {
        if (server == null || playerName == null || playerName.isEmpty()) {
            return Optional.empty();
        }

        if (server.getProfileCache() == null) {
            return Optional.empty();
        }

        Optional<GameProfile> cached = server.getProfileCache().get(playerName);
        if (cached.isEmpty()) {
            return Optional.empty();
        }

        GameProfile filled = server.getSessionService().fillProfileProperties(cached.get(), true);
        return filled.getProperties().get("textures").stream().findFirst();
    }

    public static Optional<Property> fetchTexturesByUuid(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return Optional.empty();
        }

        GameProfile filled = server.getSessionService().fillProfileProperties(new GameProfile(uuid, ""), true);
        return filled.getProperties().get("textures").stream().findFirst();
    }

    public static String randomNpcSkinSourceName(RandomSource random) {
        if (random == null) {
            random = RandomSource.create();
        }
        return DEFAULT_NPC_SKIN_SOURCES[random.nextInt(DEFAULT_NPC_SKIN_SOURCES.length)];
    }
}
