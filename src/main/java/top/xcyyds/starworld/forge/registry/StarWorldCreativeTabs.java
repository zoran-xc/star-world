package top.xcyyds.starworld.forge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import top.xcyyds.starworld.common.StarWorldCommon;

public final class StarWorldCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarWorldCommon.MOD_ID);

    public static final RegistryObject<CreativeModeTab> STARWORLD_TAB = TABS.register("starworld", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(StarWorldItems.DEBUG_WAND.get()))
            .title(Component.translatable("itemGroup.starworld"))
            .displayItems((parameters, output) -> output.accept(StarWorldItems.DEBUG_WAND.get()))
            .build());

    private StarWorldCreativeTabs() {
    }
}
