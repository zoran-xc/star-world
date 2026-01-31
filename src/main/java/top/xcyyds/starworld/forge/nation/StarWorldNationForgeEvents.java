package top.xcyyds.starworld.forge.nation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;
import top.xcyyds.starworld.common.nation.NationSavedData;
import top.xcyyds.starworld.forge.debug.console.StarWorldConsole;

@Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarWorldNationForgeEvents {
    private StarWorldNationForgeEvents() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }

        long t0 = System.nanoTime();
        NationSavedData data = NationSavedData.getOrCreate(serverLevel);
        long ms = (System.nanoTime() - t0) / 1_000_000L;

        StarWorldConsole.log(serverLevel, "NATION", "NationSavedData.getOrCreate done, nations=" + data.nations().size() + ", costMs=" + ms);
    }
}
