package top.xcyyds.starworld.forge.client.npc.skin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import top.xcyyds.starworld.forge.npc.entity.ForgePlayerNpcEntity;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcSkinController {
    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_REQUEST_KEY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST_TIME_MS = new ConcurrentHashMap<>();
    private static final long RETRY_INTERVAL_MS = 3_000L;

    private NpcSkinController() {
    }

    public static ResourceLocation getSkinLocation(ForgePlayerNpcEntity entity) {
        UUID id = entity.getNpcProfileId();
        String key = buildRequestKey(entity);
        if (key.isEmpty()) {
            return DefaultPlayerSkin.getDefaultSkin(id);
        }

        ResourceLocation cached = SKIN_CACHE.get(id);

        String lastKey = LAST_REQUEST_KEY.get(id);
        boolean keyChanged = !Objects.equals(lastKey, key);
        long now = Util.getMillis();
        long lastTime = LAST_REQUEST_TIME_MS.getOrDefault(id, 0L);
        boolean needRetry = cached == null && (now - lastTime) >= RETRY_INTERVAL_MS;

        if (keyChanged) {
            SKIN_CACHE.remove(id);
            cached = null;
        }

        if (keyChanged || needRetry) {
            LAST_REQUEST_KEY.put(id, key);
            LAST_REQUEST_TIME_MS.put(id, now);

            GameProfile profile = entity.buildRenderGameProfile();
            SkinManager skinManager = Minecraft.getInstance().getSkinManager();
            skinManager.registerSkins(profile, (type, location, texture) -> {
                if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN) {
                    SKIN_CACHE.put(id, location);
                }
            }, true);
        }

        if (cached != null) {
            return cached;
        }

        return DefaultPlayerSkin.getDefaultSkin(id);
    }

    private static String buildRequestKey(ForgePlayerNpcEntity entity) {
        String texturesValue = entity.getSkinTexturesValue();
        if (texturesValue != null && !texturesValue.isEmpty()) {
            String sig = entity.getSkinTexturesSignature();
            if (sig == null) {
                sig = "";
            }
            return "textures:" + texturesValue + ":" + sig;
        }

        String sourceName = entity.getSkinSourceName();
        if (sourceName == null || sourceName.isEmpty()) {
            return "";
        }
        return "name:" + sourceName;
    }
}
