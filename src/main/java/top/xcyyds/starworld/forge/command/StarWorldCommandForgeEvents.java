package top.xcyyds.starworld.forge.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.xcyyds.starworld.common.StarWorldCommon;

@Mod.EventBusSubscriber(modid = StarWorldCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarWorldCommandForgeEvents {
    private StarWorldCommandForgeEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StarWorldCommandService.onRegisterCommands(event);
    }
}
