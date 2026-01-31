package top.xcyyds.starworld.forge.client.npc.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import top.xcyyds.starworld.forge.client.npc.skin.NpcSkinController;
import top.xcyyds.starworld.forge.npc.entity.ForgePlayerNpcEntity;

public final class PlayerNpcRenderer extends LivingEntityRenderer<ForgePlayerNpcEntity, PlayerModel<ForgePlayerNpcEntity>> {
    public PlayerNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ForgePlayerNpcEntity entity) {
        return NpcSkinController.getSkinLocation(entity);
    }
}
