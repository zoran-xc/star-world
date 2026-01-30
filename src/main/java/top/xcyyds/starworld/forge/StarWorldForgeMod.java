package top.xcyyds.starworld.forge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.forge.registry.StarWorldCreativeTabs;
import top.xcyyds.starworld.forge.registry.StarWorldItems;

@Mod(StarWorldForgeMod.MOD_ID)
public final class StarWorldForgeMod {
    public static final String MOD_ID = StarWorldCommon.MOD_ID;

    private static final Logger LOGGER = LogUtils.getLogger();

    public StarWorldForgeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        StarWorldItems.ITEMS.register(modEventBus);
        StarWorldCreativeTabs.TABS.register(modEventBus);

        StarWorldCommon.init(new ForgePlatform(LOGGER));
    }
}
