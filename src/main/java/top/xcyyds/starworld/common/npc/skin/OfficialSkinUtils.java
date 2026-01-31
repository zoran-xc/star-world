package top.xcyyds.starworld.common.npc.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Optional;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public final class OfficialSkinUtils {
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

    public static boolean isSlimSkinTexturesValue(String texturesValue) {
        if (texturesValue == null || texturesValue.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(texturesValue);
            String json = new String(decoded, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);
            if (root == null || !root.isJsonObject()) {
                return false;
            }

            JsonObject rootObj = root.getAsJsonObject();
            JsonObject textures = rootObj.getAsJsonObject("textures");
            if (textures == null) {
                return false;
            }

            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null) {
                return false;
            }

            JsonObject meta = skin.getAsJsonObject("metadata");
            if (meta == null) {
                return false;
            }

            JsonElement model = meta.get("model");
            return model != null && model.isJsonPrimitive() && "slim".equalsIgnoreCase(model.getAsString());
        } catch (Throwable t) {
            return false;
        }
    }
}
