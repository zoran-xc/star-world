package top.xcyyds.starworld.forge.client.npc.skin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import top.xcyyds.starworld.forge.npc.entity.ForgePlayerNpcEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcSkinController {
    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> REQUESTED = ConcurrentHashMap.newKeySet();

    private NpcSkinController() {
    }

    public static ResourceLocation getSkinLocation(ForgePlayerNpcEntity entity) {
        UUID id = entity.getNpcProfileId();
        ResourceLocation cached = SKIN_CACHE.get(id);
        if (cached != null) {
            return cached;
        }

        if (REQUESTED.add(id)) {
            GameProfile profile = entity.buildRenderGameProfile();
            SkinManager skinManager = Minecraft.getInstance().getSkinManager();
            skinManager.registerSkins(profile, (type, location, texture) -> {
                if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN) {
                    SKIN_CACHE.put(id, location);
                }
            }, true);
        }

        return DefaultPlayerSkin.getDefaultSkin(id);
    }
}
