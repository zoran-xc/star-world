package top.xcyyds.starworld.forge.client.npc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.xcyyds.starworld.forge.client.npc.skin.NpcSkinController;
import top.xcyyds.starworld.forge.npc.entity.ForgePlayerNpcEntity;

public final class PlayerNpcRenderer extends EntityRenderer<ForgePlayerNpcEntity> {
    private final MobRenderer<ForgePlayerNpcEntity, PlayerModel<ForgePlayerNpcEntity>> defaultRenderer;
    private final MobRenderer<ForgePlayerNpcEntity, PlayerModel<ForgePlayerNpcEntity>> slimRenderer;

    public PlayerNpcRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.defaultRenderer = new MobRenderer<>(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F) {
            @Override
            public ResourceLocation getTextureLocation(ForgePlayerNpcEntity entity) {
                return NpcSkinController.getSkinLocation(entity);
            }

            @Override
            protected void renderNameTag(ForgePlayerNpcEntity entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
                String lang = Minecraft.getInstance().getLanguageManager().getSelected();
                boolean preferZh = lang != null && lang.toLowerCase().startsWith("zh");

                String zh = entity.getNpcNameZh();
                String en = entity.getNpcNameEn();

                String chosen;
                if (preferZh) {
                    chosen = zh.isEmpty() ? en : zh;
                } else {
                    chosen = en.isEmpty() ? zh : en;
                }

                if (chosen == null || chosen.isEmpty()) {
                    super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
                    return;
                }

                Component name = Component.literal(chosen).withStyle(ChatFormatting.BLUE);
                super.renderNameTag(entity, name, poseStack, buffer, packedLight);
            }
        };
        this.slimRenderer = new MobRenderer<>(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F) {
            @Override
            public ResourceLocation getTextureLocation(ForgePlayerNpcEntity entity) {
                return NpcSkinController.getSkinLocation(entity);
            }

            @Override
            protected void renderNameTag(ForgePlayerNpcEntity entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
                String lang = Minecraft.getInstance().getLanguageManager().getSelected();
                boolean preferZh = lang != null && lang.toLowerCase().startsWith("zh");

                String zh = entity.getNpcNameZh();
                String en = entity.getNpcNameEn();

                String chosen;
                if (preferZh) {
                    chosen = zh.isEmpty() ? en : zh;
                } else {
                    chosen = en.isEmpty() ? zh : en;
                }

                if (chosen == null || chosen.isEmpty()) {
                    super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
                    return;
                }

                Component name = Component.literal(chosen).withStyle(ChatFormatting.BLUE);
                super.renderNameTag(entity, name, poseStack, buffer, packedLight);
            }
        };
    }

    @Override
    public ResourceLocation getTextureLocation(ForgePlayerNpcEntity entity) {
        return NpcSkinController.getSkinLocation(entity);
    }

    @Override
    public void render(ForgePlayerNpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isSkinSlim()) {
            slimRenderer.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        defaultRenderer.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
