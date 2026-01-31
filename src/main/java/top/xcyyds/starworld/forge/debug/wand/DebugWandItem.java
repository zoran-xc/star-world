package top.xcyyds.starworld.forge.debug.wand;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class DebugWandItem extends Item {
    public DebugWandItem(Properties properties) {
        super(properties);
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
                    } catch (Throwable ignored) {
                    }
                });
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return super.use(level, player, usedHand);
    }
}
