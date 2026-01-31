package top.xcyyds.starworld.forge.debug.wand;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class DebugWandItem extends Item {
    public DebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        if (player.isShiftKeyDown()) {
            Level level = context.getLevel();
            InteractionHand hand = context.getHand();
            ItemStack stack = context.getItemInHand();

            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> clazz = Class.forName("top.xcyyds.starworld.forge.debug.wand.client.DebugWandClient");
                        clazz.getMethod("openDebugScreen", InteractionHand.class).invoke(null, hand);
                    } catch (Throwable t) {
                        try {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("[StarWorld] 打开调试界面失败: " + t.getClass().getSimpleName()), true);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> clazz = Class.forName("top.xcyyds.starworld.forge.debug.wand.client.DebugWandClient");
                        clazz.getMethod("openDebugScreen", InteractionHand.class).invoke(null, usedHand);
                    } catch (Throwable t) {
                        try {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("[StarWorld] 打开调试界面失败: " + t.getClass().getSimpleName()), true);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return super.use(level, player, usedHand);
    }
}
