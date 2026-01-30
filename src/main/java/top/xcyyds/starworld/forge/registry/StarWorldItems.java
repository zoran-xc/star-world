package top.xcyyds.starworld.forge.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.xcyyds.starworld.common.StarWorldCommon;

public final class StarWorldItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StarWorldCommon.MOD_ID);

    public static final RegistryObject<Item> DEBUG_WAND = ITEMS.register("debug_wand", () -> new Item(new Item.Properties()));

    private StarWorldItems() {
    }
}
