package top.xcyyds.starworld.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.client.npc.render.PlayerNpcRenderer;
import top.xcyyds.starworld.forge.npc.registry.StarWorldNpcEntityTypes;

@Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StarWorldClientModEvents {
    private StarWorldClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(StarWorldNpcEntityTypes.PLAYER_NPC.get(), PlayerNpcRenderer::new);
    }
}
