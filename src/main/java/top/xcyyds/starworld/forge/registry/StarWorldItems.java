package top.xcyyds.starworld.forge.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.npc.registry.StarWorldNpcEntityTypes;

public final class StarWorldItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StarWorldCommon.MOD_ID);

    public static final RegistryObject<Item> DEBUG_WAND = ITEMS.register("debug_wand", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLAYER_NPC_SPAWN_EGG = ITEMS.register("player_npc_spawn_egg", () -> new ForgeSpawnEggItem(
            StarWorldNpcEntityTypes.PLAYER_NPC,
            0x3B5AA3,
            0xD1B38C,
            new Item.Properties()
    ));

    private StarWorldItems() {
    }
}
